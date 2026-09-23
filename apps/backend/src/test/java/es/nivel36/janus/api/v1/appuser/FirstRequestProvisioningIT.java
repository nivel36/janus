/*
 * Copyright 2026 Abel Ferrer Jiménez
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package es.nivel36.janus.api.v1.appuser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.core.authority.AuthorityUtils.createAuthorityList;
import static es.nivel36.janus.api.v1.SecurityTestConfiguration.verifiedJwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import es.nivel36.janus.api.v1.SecurityTestConfiguration;

@SpringBootTest(properties = {
		"spring.security.oauth2.resourceserver.jwt.issuer-uri=http://janus.local/auth/realms/Nivel36",
		"janus.user-provisioning.defaults.locale=es-ES", "janus.user-provisioning.defaults.time-format=H24",
		"janus.user-provisioning.defaults.default-timezone=Europe/Madrid" })
@AutoConfigureMockMvc
@Import(SecurityTestConfiguration.class)
class FirstRequestProvisioningIT {

	private static final String SUBJECT = "9a60b9f4-7436-4d93-9c25-08e08f3dfc58";
	private static final String OTHER_SUBJECT = "b9b0c670-b030-4ce2-8a48-516a86cb80e2";
	private static final String OPAQUE_SUBJECT = "oidc-provider|tenant:customers|user:aferrer:opaque-identity";
	private static final String ADMIN_SUBJECT = "admin-subject-for-concurrency-test";
	private static final String ADMIN_USERNAME = "concurrency-test-admin";
	private static final String USERNAME = "aferrer@nivel36.es";
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
		this.jdbcClient.sql("DELETE FROM app_user WHERE email IN ('subject-target-one', 'subject-target-two', :adminUsername)")
				.param("adminUsername", ADMIN_USERNAME).update();
		this.jdbcClient.sql("DELETE FROM employee WHERE email = :email").param("email", LINK_EMAIL).update();
		this.jdbcClient.sql("DELETE FROM schedule WHERE id = 901").update();
	}

	@Test
	void provisionsAndRetrievesUserWithLongOpaqueSubject() throws Exception {
		this.mvc.perform(get("/api/v1/appusers/me").with(verifiedJwt()
				.jwt(token -> token.issuer(this.issuer).subject(OPAQUE_SUBJECT)
						.claim("email", "opaque-subject@example.test"))
				.authorities(createAuthorityList("ROLE_JANUS_USER")))).andExpect(status().isOk());

		this.mvc.perform(get("/api/v1/appusers/me").with(verifiedJwt()
				.jwt(token -> token.issuer(this.issuer).subject(OPAQUE_SUBJECT)
						.claim("email", "ignored@example.test"))
				.authorities(createAuthorityList("ROLE_JANUS_USER"))))
				.andExpect(status().isOk()).andExpect(jsonPath("$.email").value("opaque-subject@example.test"));

		assertThat(this.jdbcClient.sql("SELECT keycloak_subject FROM app_user WHERE email = 'opaque-subject@example.test'")
				.query(String.class).single()).isEqualTo(OPAQUE_SUBJECT);
	}

	@Test
	void concurrentRequestsForSameSubjectAndUsernameAreIdempotent() throws Exception {
		final List<MvcResult> results = this.provisionConcurrently(SUBJECT, "same-name", null, SUBJECT, "same-name", null);

		assertThat(results).isNotEmpty().allMatch(result -> result.getResponse().getStatus() == 200);
		assertThat(this.countProfiles()).isOne();
	}

	@Test
	void concurrentRequestsForSameSubjectAndDifferentUsernamesReturnTheSubjectWinner() throws Exception {
		final List<MvcResult> results = this.provisionConcurrently(SUBJECT, "first-name", null, SUBJECT, "second-name", null);

		assertThat(results).isNotEmpty().allMatch(result -> result.getResponse().getStatus() == 200);
		assertThat(this.countProfiles()).isOne();
		assertThat(results).extracting(result -> result.getResponse().getContentAsString()).isNotEmpty()
				.allMatch(body -> body.contains(this.usernameForSubject(SUBJECT)));
	}

	@Test
	void concurrentRequestsForSameEmployeeAndDifferentSubjectsOnlyLinkOneProfile() throws Exception {
		final Long employeeId = this.insertEmployee();
		final List<MvcResult> results = this.provisionConcurrently(SUBJECT, "employee-one", LINK_EMAIL,
				OTHER_SUBJECT, "employee-two", LINK_EMAIL);

		assertThat(results).isNotEmpty().allMatch(result -> result.getResponse().getStatus() == 200);
		assertThat(this.jdbcClient.sql("SELECT COUNT(*) FROM app_user WHERE employee_id = :employeeId")
				.param("employeeId", employeeId).query(Long.class).single()).isOne();
		assertThat(this.jdbcClient.sql("SELECT COUNT(*) FROM app_user WHERE keycloak_subject IN (:one, :two)")
				.param("one", SUBJECT).param("two", OTHER_SUBJECT).query(Long.class).single()).isEqualTo(2L);
	}

	@Test
	void concurrentRequestsForSameEmailAndDifferentSubjectsCreateBothIdentities() throws Exception {
		final List<MvcResult> results = this.provisionConcurrently(SUBJECT, "occupied-name", null,
				OTHER_SUBJECT, "occupied-name", null);

		assertThat(results).extracting(result -> result.getResponse().getStatus()).containsOnly(200);
		assertThat(this.jdbcClient.sql("SELECT COUNT(*) FROM app_user WHERE email = 'occupied-name@example.test'")
				.query(Long.class).single()).isEqualTo(2L);
	}

	private List<MvcResult> provisionConcurrently(final String firstSubject, final String firstUsername,
			final String firstEmail, final String secondSubject, final String secondUsername, final String secondEmail)
			throws Exception {
		final CountDownLatch ready = new CountDownLatch(2);
		final CountDownLatch start = new CountDownLatch(1);
		try (var executor = Executors.newFixedThreadPool(2)) {
			final Future<MvcResult> first = executor.submit(() -> this.performProvisioning(firstSubject, firstUsername,
					firstEmail, ready, start));
			final Future<MvcResult> second = executor.submit(() -> this.performProvisioning(secondSubject, secondUsername,
					secondEmail, ready, start));
			ready.await();
			start.countDown();
			return List.of(first.get(), second.get());
		}
	}

	private MvcResult performProvisioning(final String subject, final String username, final String email,
			final CountDownLatch ready, final CountDownLatch start) throws Exception {
		ready.countDown();
		start.await();
		return this.mvc.perform(get("/api/v1/appusers/me").with(verifiedJwt().jwt(token -> {
			token.issuer(this.issuer).subject(subject).claim("email", email == null ? username + "@example.test" : email)
					.claim("email_verified", true);
		}).authorities(createAuthorityList("ROLE_JANUS_USER")))).andReturn();
	}

	private String usernameForSubject(final String subject) {
		return this.jdbcClient.sql("SELECT email FROM app_user WHERE keycloak_subject = :subject")
				.param("subject", subject).query(String.class).single();
	}

	@Test
	void verifiedEmailLinksTheOnlyUnlinkedEmployeeAfterNormalization() throws Exception {
		final Long employeeId = this.insertEmployee();

		this.mvc.perform(get("/api/v1/appusers/me").with(verifiedJwt()
				.jwt(token -> token.issuer(this.issuer).subject(SUBJECT).claim("preferred_username", "linked-user")
						.claim("email", "  FIRST-ACCESS-LINK@EXAMPLE.TEST ").claim("email_verified", true))
				.authorities(createAuthorityList("ROLE_JANUS_USER")))).andExpect(status().isOk());
		assertThat(this.jdbcClient.sql("SELECT employee_id FROM app_user WHERE keycloak_subject = :subject")
				.param("subject", SUBJECT).query(Long.class).single()).isEqualTo(employeeId);
	}

	@Test
	void employeeLinkedToAnotherIdentityIsNotReassigned() throws Exception {
		final Long employeeId = this.insertEmployee();
		this.provision(SUBJECT, "first-identity");

		this.mvc.perform(
				get("/api/v1/appusers/me")
						.with(verifiedJwt()
								.jwt(token -> token.issuer(this.issuer).subject(OTHER_SUBJECT)
										.claim("preferred_username", "second-identity").claim("email", LINK_EMAIL)
										.claim("email_verified", true))
								.authorities(createAuthorityList("ROLE_JANUS_USER"))))
				.andExpect(status().isOk());
		assertThat(this.jdbcClient.sql("SELECT COUNT(*) FROM app_user WHERE keycloak_subject = :subject AND employee_id IS NULL")
				.param("subject", OTHER_SUBJECT).query(Long.class).single()).isOne();

		assertThat(this.jdbcClient.sql("SELECT keycloak_subject FROM app_user WHERE employee_id = :employeeId")
				.param("employeeId", employeeId).query(String.class).single()).isEqualTo(SUBJECT);
	}

	private void provision(final String subject, final String username) throws Exception {
		this.mvc.perform(get("/api/v1/appusers/me").with(verifiedJwt()
				.jwt(token -> token.issuer(this.issuer).subject(subject).claim("preferred_username", username)
						.claim("email", LINK_EMAIL).claim("email_verified", true))
				.authorities(createAuthorityList("ROLE_JANUS_USER")))).andExpect(status().isOk());
	}

	@Test
	void firstEmployeeVisitCanSearchTimeLogsAfterLoadingProfile() throws Exception {
		this.insertEmployee();
		final var authentication = verifiedJwt().jwt(token -> token.issuer(this.issuer).subject(SUBJECT)
				.claim("preferred_username", "first-employee").claim("email", LINK_EMAIL)
				.claim("email_verified", true)).authorities(createAuthorityList("ROLE_JANUS_EMPLOYEE"));

		this.mvc.perform(get("/api/v1/timelogs").with(authentication)).andExpect(status().isForbidden());
		assertThat(this.countProfiles()).isZero();
		this.mvc.perform(get("/api/v1/appusers/me").with(authentication)).andExpect(status().isOk());
		this.mvc.perform(get("/api/v1/timelogs").with(authentication))
				.andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements").value(0));
	}

	private Long insertEmployee() {
		this.jdbcClient.sql("INSERT INTO schedule(id, code, name) VALUES (901, 'FIRST-ACCESS', 'First access')").update();
		this.jdbcClient.sql("""
				INSERT INTO employee (name, surname, email, schedule_id)
				VALUES ('First', 'Access', :email, 901)
				""").param("email", LINK_EMAIL).update();
		return this.jdbcClient.sql("SELECT id FROM employee WHERE email = :email")
				.param("email", LINK_EMAIL).query(Long.class).single();
	}

	@Test
	void firstAuthenticatedRequestProvisionsLocalProfile() throws Exception {
		assertThat(this.countProfiles()).isZero();

		this.mvc.perform(get("/api/v1/appusers/me").with(
				verifiedJwt().jwt(token -> token.issuer(this.issuer).subject(SUBJECT).claim("email", USERNAME))
						.authorities(createAuthorityList("ROLE_JANUS_USER"))))
				.andExpect(status().isOk()).andExpect(jsonPath("$.email").value(USERNAME))
				.andExpect(jsonPath("$.locale").value("es-ES")).andExpect(jsonPath("$.timeFormat").value("H24"))
				.andExpect(jsonPath("$.defaultTimezone").value("Europe/Madrid"));

		assertThat(this.countProfiles()).isOne();
	}

	private long countProfiles() {
		return this.jdbcClient.sql("SELECT COUNT(*) FROM app_user WHERE keycloak_subject = :subject")
				.param("subject", SUBJECT).query(Long.class).single();
	}
}
