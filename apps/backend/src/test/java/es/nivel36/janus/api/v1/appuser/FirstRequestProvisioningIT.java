/*
 * Copyright 2026 Abel Ferrer Jiménez Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package es.nivel36.janus.api.v1.appuser;

import static es.nivel36.janus.api.v1.SecurityTestConfiguration.verifiedJwt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.core.authority.AuthorityUtils.createAuthorityList;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import es.nivel36.janus.api.v1.SecurityTestConfiguration;

/**
 * Exercises committed first-access provisioning and concurrent
 * identity/employee claims using validated JWT fixtures.
 */
@SpringBootTest(properties = {
		"spring.security.oauth2.resourceserver.jwt.issuer-uri=http://janus.local/auth/realms/Nivel36",
		"janus.user-provisioning.defaults.locale=es-ES", "janus.user-provisioning.defaults.time-format=H24",
		"janus.user-provisioning.defaults.default-timezone=Europe/Madrid" })

@AutoConfigureMockMvc
@Import(SecurityTestConfiguration.class)
class FirstRequestProvisioningIT {

	private static final String SUBJECT = "9a60b9f4-7436-4d93-9c25-08e08f3dfc58";
	private static final String OTHER_SUBJECT = "b9b0c670-b030-4ce2-8a48-516a86cb80e2";
	private static final String OPAQUE_SUBJECT = "x".repeat(255);
	private static final String EMAIL = "aferrer@nivel36.es";
	private static final String LINK_EMAIL = "first-access-link@example.test";

	private @Autowired MockMvc mvc;
	private @Autowired JdbcClient jdbcClient;
	private @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer;

	@BeforeEach
	@AfterEach
	void removeLocalProfile() {
		this.jdbcClient.sql("DELETE FROM app_user WHERE keycloak_subject IN (:subject, :otherSubject, :opaqueSubject)")
				.param("subject", SUBJECT).param("otherSubject", OTHER_SUBJECT).param("opaqueSubject", OPAQUE_SUBJECT)
				.update();
		this.jdbcClient.sql("DELETE FROM employee WHERE email = :email").param("email", LINK_EMAIL).update();
		this.jdbcClient.sql("DELETE FROM schedule WHERE id = 901").update();
	}

	@Test
	void provisionsAndRetrievesUserWithLongOpaqueSubject() throws Exception {
		this.mvc.perform(
				get("/api/v1/app-users/me").with(
						verifiedJwt()
								.jwt(
										token -> token.issuer(this.issuer).subject(OPAQUE_SUBJECT)
												.claim("email", "opaque-subject@example.test"))
								.authorities(createAuthorityList("ROLE_JANUS_USER"))))
				.andExpect(status().isOk());

		this.mvc.perform(
				get("/api/v1/app-users/me").with(
						verifiedJwt()
								.jwt(
										token -> token.issuer(this.issuer).subject(OPAQUE_SUBJECT)
												.claim("email", "ignored@example.test"))
								.authorities(createAuthorityList("ROLE_JANUS_USER"))))
				.andExpect(status().isOk()).andExpect(jsonPath("$.email").value("opaque-subject@example.test"));

		assertThat(
				this.jdbcClient.sql("SELECT email FROM app_user WHERE keycloak_subject = :subject")
						.param("subject", OPAQUE_SUBJECT).query(String.class).single())
				.isEqualTo("opaque-subject@example.test");
	}

	@Test
	void concurrentRequestsForSameSubjectAndEmailAreIdempotent() throws Exception {
		final List<MvcResult> results = this
				.provisionConcurrently(SUBJECT, "same-name@example.test", SUBJECT, "same-name@example.test");

		assertThat(results).isNotEmpty().allMatch(result -> result.getResponse().getStatus() == 200);
		assertThat(this.countProfiles()).isOne();
	}

	@Test
	void concurrentRequestsForSameSubjectAndDifferentEmailsReturnTheSubjectWinner() throws Exception {
		final List<MvcResult> results = this
				.provisionConcurrently(SUBJECT, "first-name@example.test", SUBJECT, "second-name@example.test");

		assertThat(results).isNotEmpty().allMatch(result -> result.getResponse().getStatus() == 200);
		assertThat(this.countProfiles()).isOne();
		assertThat(this.emailForSubject(SUBJECT)).isIn("first-name@example.test", "second-name@example.test");
	}

	@Test
	void concurrentRequestsForSameEmployeeAndDifferentSubjectsOnlyLinkOneProfile() throws Exception {
		final Long employeeId = this.insertEmployee();
		final List<MvcResult> results = this.provisionConcurrently(SUBJECT, LINK_EMAIL, OTHER_SUBJECT, LINK_EMAIL);

		assertThat(results).isNotEmpty().allMatch(result -> result.getResponse().getStatus() == 200);
		assertThat(
				this.jdbcClient.sql("SELECT COUNT(*) FROM app_user WHERE employee_id = :employeeId")
						.param("employeeId", employeeId).query(Long.class).single())
				.isOne();
		assertThat(
				this.jdbcClient.sql("SELECT COUNT(*) FROM app_user WHERE keycloak_subject IN (:one, :two)")
						.param("one", SUBJECT).param("two", OTHER_SUBJECT).query(Long.class).single())
				.isEqualTo(2L);
	}

	@Test
	void concurrentRequestsForSameEmailAndDifferentSubjectsCreateBothIdentities() throws Exception {
		final List<MvcResult> results = this.provisionConcurrently(
				SUBJECT,
				"occupied-name@example.test",
				OTHER_SUBJECT,
				"occupied-name@example.test");

		assertThat(results).extracting(result -> result.getResponse().getStatus()).containsOnly(200);
		assertThat(
				this.jdbcClient.sql("SELECT COUNT(*) FROM app_user WHERE email = 'occupied-name@example.test'")
						.query(Long.class).single())
				.isEqualTo(2L);
	}

	private List<MvcResult> provisionConcurrently(
			final String firstSubject,
			final String firstEmail,
			final String secondSubject,
			final String secondEmail) throws Exception {
		final CountDownLatch ready = new CountDownLatch(2);
		final CountDownLatch start = new CountDownLatch(1);
		try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
			final Future<MvcResult> first = executor
					.submit(() -> this.performProvisioning(firstSubject, firstEmail, ready, start));
			final Future<MvcResult> second = executor
					.submit(() -> this.performProvisioning(secondSubject, secondEmail, ready, start));
			try {
				assertThat(ready.await(10, TimeUnit.SECONDS)).as("both provisioning workers ready").isTrue();
				start.countDown();
				return List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS));
			} finally {
				start.countDown();
				first.cancel(true);
				second.cancel(true);
				executor.shutdownNow();
			}
		}
	}

	private MvcResult performProvisioning(
			final String subject,
			final String email,
			final CountDownLatch ready,
			final CountDownLatch start) throws Exception {
		ready.countDown();
		assertThat(start.await(10, TimeUnit.SECONDS)).as("provisioning start signal").isTrue();
		return this.mvc.perform(get("/api/v1/app-users/me").with(verifiedJwt().jwt(token -> {
			token.issuer(this.issuer).subject(subject).claim("email", email).claim("email_verified", true);
			if (LINK_EMAIL.equals(email)) {
				token.claim("employeeNumber", "EMP-0901");
			}
		}).authorities(createAuthorityList("ROLE_JANUS_USER")))).andReturn();
	}

	private String emailForSubject(final String subject) {
		return this.jdbcClient.sql("SELECT email FROM app_user WHERE keycloak_subject = :subject")
				.param("subject", subject).query(String.class).single();
	}

	@Test
	void validEmployeeNumberClaimLinksUnclaimedEmployeeAndNormalizesEmailCase() throws Exception {
		final Long employeeId = this.insertEmployee();

		this.mvc.perform(
				get("/api/v1/app-users/me").with(
						verifiedJwt()
								.jwt(
										token -> token.issuer(this.issuer).subject(SUBJECT)
												.claim("preferred_username", "linked-user")
												.claim("email", "FIRST-ACCESS-LINK@EXAMPLE.TEST")
												.claim("employeeNumber", "EMP-0901").claim("email_verified", true))
								.authorities(createAuthorityList("ROLE_JANUS_USER"))))
				.andExpect(status().isOk());
		assertThat(
				this.jdbcClient.sql("SELECT employee_id FROM app_user WHERE keycloak_subject = :subject")
						.param("subject", SUBJECT).query(Long.class).single())
				.isEqualTo(employeeId);
		assertThat(this.emailForSubject(SUBJECT)).isEqualTo(LINK_EMAIL);
	}

	@Test
	void employeeLinkedToAnotherIdentityIsNotReassigned() throws Exception {
		final Long employeeId = this.insertEmployee();
		this.provision(SUBJECT);

		this.mvc.perform(
				get("/api/v1/app-users/me").with(
						verifiedJwt().jwt(
								token -> token.issuer(this.issuer).subject(OTHER_SUBJECT)
										.claim("preferred_username", "second-identity").claim("email", LINK_EMAIL)
										.claim("employeeNumber", "EMP-0901").claim("email_verified", true))
								.authorities(createAuthorityList("ROLE_JANUS_USER"))))
				.andExpect(status().isOk());
		assertThat(
				this.jdbcClient
						.sql("SELECT COUNT(*) FROM app_user WHERE keycloak_subject = :subject AND employee_id IS NULL")
						.param("subject", OTHER_SUBJECT).query(Long.class).single())
				.isOne();

		assertThat(
				this.jdbcClient.sql("SELECT keycloak_subject FROM app_user WHERE employee_id = :employeeId")
						.param("employeeId", employeeId).query(String.class).single())
				.isEqualTo(SUBJECT);
	}

	private void provision(final String subject) throws Exception {
		this.mvc.perform(
				get("/api/v1/app-users/me").with(
						verifiedJwt()
								.jwt(
										token -> token.issuer(this.issuer).subject(subject).claim("email", LINK_EMAIL)
												.claim("employeeNumber", "EMP-0901").claim("email_verified", true))
								.authorities(createAuthorityList("ROLE_JANUS_USER"))))
				.andExpect(status().isOk());
	}

	@Test
	void firstEmployeeVisitCanSearchTimeLogsAfterLoadingProfile() throws Exception {
		this.insertEmployee();
		final JwtRequestPostProcessor authentication = verifiedJwt().jwt(
				token -> token.issuer(this.issuer).subject(SUBJECT).claim("preferred_username", "first-employee")
						.claim("email", LINK_EMAIL).claim("employeeNumber", "EMP-0901").claim("email_verified", true))
				.authorities(createAuthorityList("ROLE_JANUS_EMPLOYEE"));

		this.mvc.perform(get("/api/v1/time-logs").with(authentication)).andExpect(status().isForbidden());
		assertThat(this.countProfiles()).isZero();
		this.mvc.perform(get("/api/v1/app-users/me").with(authentication)).andExpect(status().isOk());
		this.mvc.perform(get("/api/v1/time-logs").with(authentication)).andExpect(status().isOk())
				.andExpect(jsonPath("$.page.totalElements").value(0));
	}

	private Long insertEmployee() {
		this.jdbcClient.sql("INSERT INTO schedule(id, code, name) VALUES (901, 'FIRST-ACCESS', 'First access')")
				.update();
		this.jdbcClient.sql("""
				INSERT INTO employee (employee_number, name, surname, email, schedule_id)
				VALUES ('EMP-0901', 'First', 'Access', :email, 901)
				""").param("email", LINK_EMAIL).update();
		return this.jdbcClient.sql("SELECT id FROM employee WHERE email = :email").param("email", LINK_EMAIL)
				.query(Long.class).single();
	}

	@Test
	void unknownEmployeeNumberCreatesAnUnassociatedAccount() throws Exception {
		this.mvc.perform(
				get("/api/v1/app-users/me").with(
						verifiedJwt()
								.jwt(
										token -> token.issuer(this.issuer).subject(SUBJECT).claim("email", EMAIL)
												.claim("employeeNumber", "EMP-UNKNOWN").claim("email_verified", true))
								.authorities(createAuthorityList("ROLE_JANUS_USER"))))
				.andExpect(status().isOk());

		assertThat(
				this.jdbcClient
						.sql("SELECT COUNT(*) FROM app_user WHERE keycloak_subject = :subject AND employee_id IS NULL")
						.param("subject", SUBJECT).query(Long.class).single())
				.isOne();
	}

	@Test
	void firstAuthenticatedRequestProvisionsLocalProfile() throws Exception {
		assertThat(this.countProfiles()).isZero();

		this.mvc.perform(
				get("/api/v1/app-users/me").with(
						verifiedJwt().jwt(token -> token.issuer(this.issuer).subject(SUBJECT).claim("email", EMAIL))
								.authorities(createAuthorityList("ROLE_JANUS_USER"))))
				.andExpect(status().isOk()).andExpect(jsonPath("$.email").value(EMAIL))
				.andExpect(jsonPath("$.locale").value("es-ES")).andExpect(jsonPath("$.timeFormat").value("H24"))
				.andExpect(jsonPath("$.defaultTimezone").value("Europe/Madrid"));

		assertThat(this.countProfiles()).isOne();
	}

	private long countProfiles() {
		return this.jdbcClient.sql("SELECT COUNT(*) FROM app_user WHERE keycloak_subject = :subject")
				.param("subject", SUBJECT).query(Long.class).single();
	}
}
