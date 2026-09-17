/*
 * Copyright 2026 Abel Ferrer Jiménez
 *
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

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.core.authority.AuthorityUtils.createAuthorityList;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import es.nivel36.janus.api.v1.EmployeeIdentityTestExecutionListener;
import es.nivel36.janus.api.v1.SecurityTestConfiguration;
import jakarta.persistence.EntityManager;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(SecurityTestConfiguration.class)
@Transactional
@TestExecutionListeners(listeners = EmployeeIdentityTestExecutionListener.class, mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
class AppUserControllerIT {

	private @Autowired MockMvc mvc;
	private @Autowired JdbcTemplate jdbcTemplate;
	private @Autowired EntityManager entityManager;

	private static final String BASE = "/api/v1/appusers";

	@Test
	@Sql(statements = {
			"INSERT INTO app_user(username,keycloak_subject,locale,time_format,default_timezone) VALUES('jdoe','11111111-1111-4111-8111-111111111111','en-US','H24','Europe/Madrid')" })
	void testUpdateShouldReturn200AndUpdatedBody() throws Exception {
		final String body = """
				  {"locale":"en-CA","timeFormat":"H12","defaultTimezone":"America/Toronto"}
				""";

		this.mvc.perform(put(BASE + "/me").with(jwt().jwt(token -> token.subject("11111111-1111-4111-8111-111111111111"))//
				.authorities(createAuthorityList("ROLE_JANUS_ADMIN"))) //
				.contentType(APPLICATION_JSON).content(body)) //
				.andExpect(status().isOk()) //
				.andExpect(jsonPath("$.username").value("jdoe")) //
				.andExpect(jsonPath("$.locale").value("en-CA")) //
				.andExpect(jsonPath("$.timeFormat").value("H12")) //
				.andExpect(jsonPath("$.defaultTimezone").value("America/Toronto"));
	}

	@Test
	void testLocaleValidationAcceptsBcp47LanguageAndScriptTags() {
		final var languageOnly = new UpdateAppUserRequest("es", null, "UTC");
		final var languageScriptRegion = new UpdateAppUserRequest("zh-Hans-CN", null, "UTC");

		org.assertj.core.api.Assertions.assertThat(languageOnly.isLocaleValid()).isTrue();
		org.assertj.core.api.Assertions.assertThat(languageScriptRegion.isLocaleValid()).isTrue();
	}

	@Test
	@Sql(statements = {
			"INSERT INTO app_user(username,keycloak_subject,locale,time_format,default_timezone) VALUES('jdoe','11111111-1111-4111-8111-111111111111','en-US','H24','Europe/Madrid')" })
	void testUpdateRejectsUnsupportedLocale() throws Exception {
		this.mvc.perform(put(BASE + "/me").with(jwt().jwt(token -> token.subject("11111111-1111-4111-8111-111111111111")))
				.contentType(APPLICATION_JSON)
				.content("{\"locale\":\"zz-ZZ\",\"timeFormat\":\"H24\",\"defaultTimezone\":\"UTC\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	@Sql(statements = {
			"INSERT INTO app_user(username,keycloak_subject,locale,time_format,default_timezone) VALUES('jdoe','11111111-1111-4111-8111-111111111111','en-US','H24','Europe/Madrid')" })
	void testMeUpdatesBySubjectDespiteCopiedMutableClaims() throws Exception {
		final String body = """
				  {"locale":"en-CA","timeFormat":"H12","defaultTimezone":"America/Toronto"}
				""";

		this.mvc.perform(put(BASE + "/me")
				.with(jwt()
						.jwt(jwt -> jwt.issuer("https://issuer.example.test")
								.subject("11111111-1111-4111-8111-111111111111")
								.claim("email", "someone-else@example.com").claim("preferred_username", "someone-else"))
						.authorities(createAuthorityList("ROLE_JANUS_USER")))
				.contentType(APPLICATION_JSON).content(body)).andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("jdoe"));
	}

	@Test
	@Sql(statements = {
			"INSERT INTO app_user(username,keycloak_subject,locale,time_format,default_timezone) VALUES('jdoe','11111111-1111-4111-8111-111111111111','en-US','H24','Europe/Madrid')" })
	void testMeFindsProvisionedIdentity() throws Exception {
		this.mvc.perform(
				get(BASE + "/me").with(jwt()
						.jwt(jwt -> jwt.issuer("https://issuer.example.test")
								.subject("11111111-1111-4111-8111-111111111111").claim("preferred_username", "changed"))
						.authorities(createAuthorityList("ROLE_JANUS_USER"))))
				.andExpect(status().isOk()).andExpect(jsonPath("$.username").value("jdoe"));
	}

	@Test
	void testMeCreatesUnprovisionedIdentityWithInitialPreferences() throws Exception {
		this.mvc.perform(get(BASE + "/me").with(jwt().jwt(jwt -> jwt.issuer("https://issuer.example.test")
				.subject("99999999-9999-4999-8999-999999999999").claim("preferred_username", "new-user"))
				.authorities(createAuthorityList("ROLE_JANUS_USER")))).andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("new-user")).andExpect(jsonPath("$.locale").value("en-US"))
				.andExpect(jsonPath("$.timeFormat").value("H24")).andExpect(jsonPath("$.defaultTimezone").value("UTC"));

		this.mvc.perform(get(BASE + "/me").with(jwt()
				.jwt(jwt -> jwt.issuer("https://issuer.example.test").subject("99999999-9999-4999-8999-999999999999")
						.claim("preferred_username", "renamed-user"))
				.authorities(createAuthorityList("ROLE_JANUS_USER")))).andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("new-user"));

		org.assertj.core.api.Assertions
				.assertThat(this.jdbcTemplate.queryForObject("SELECT COUNT(*) FROM app_user WHERE keycloak_subject = ?",
						Integer.class, "99999999-9999-4999-8999-999999999999"))
				.isEqualTo(1);
	}

	@Test
	@Sql(statements = {
			"INSERT INTO app_user(username,keycloak_subject,locale,time_format,default_timezone) VALUES('recreated-user','11111111-1111-4111-8111-111111111111','en-US','H24','UTC')" })
	void recreatedAccountRequiresAdministrativeSubjectReplacement() throws Exception {
		final String replacement = "opaque-provider|tenant:customers|recreated-user:replacement-identity";
		final var replacementIdentity = jwt().jwt(token -> token.subject(replacement)
				.claim("preferred_username", "recreated-user")).authorities(createAuthorityList("ROLE_JANUS_USER"));

		this.mvc.perform(get(BASE + "/me").with(replacementIdentity)).andExpect(status().isConflict())
				.andExpect(jsonPath("$.type").value("urn:problem:external-identity-conflict"));

		this.mvc.perform(put(BASE + "/{username}/keycloak-subject", "recreated-user")
				.with(jwt().authorities(createAuthorityList("ROLE_JANUS_ADMIN"))).contentType(APPLICATION_JSON)
				.content("{\"keycloakSubject\":\"" + replacement + "\"}"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.username").value("recreated-user"));

		this.entityManager.flush();
		this.mvc.perform(get(BASE + "/me").with(replacementIdentity)).andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("recreated-user"));
	}

	@Test
	void testMeRejectsTokenWithoutJanusRolesWithoutCreatingAccount() throws Exception {
		final String subject = "44444444-4444-4444-8444-444444444444";

		this.mvc.perform(get(BASE + "/me")
				.with(jwt().jwt(jwt -> jwt.subject(subject).claim("preferred_username", "unauthorized-user"))
						.authorities(createAuthorityList("SCOPE_openid", "ROLE_OTHER_CLIENT"))))
				.andExpect(status().isForbidden());

		org.assertj.core.api.Assertions.assertThat(this.jdbcTemplate
				.queryForObject("SELECT COUNT(*) FROM app_user WHERE keycloak_subject = ?", Integer.class, subject))
				.isZero();
	}

	@Test
	void testMeRejectsPreferredUsernameThatAdminEndpointsCannotAddress() throws Exception {
		this.mvc.perform(get(BASE + "/me").with(jwt().jwt(jwt -> jwt.subject("77777777-7777-4777-8777-777777777777"))
				.authorities(createAuthorityList("ROLE_JANUS_USER")))).andExpect(status().isBadRequest());

		this.mvc.perform(get(BASE + "/me").with(jwt().jwt(
				jwt -> jwt.subject("88888888-8888-4888-8888-888888888888").claim("preferred_username", "x".repeat(51)))
				.authorities(createAuthorityList("ROLE_JANUS_USER")))).andExpect(status().isBadRequest());

		this.mvc.perform(get(BASE + "/me").with(
				jwt().jwt(jwt -> jwt.subject("66666666-6666-4666-8666-666666666666").claim("preferred_username", "ab"))
						.authorities(createAuthorityList("ROLE_JANUS_USER"))))
				.andExpect(status().isBadRequest());

		this.mvc.perform(get(BASE + "/me").with(jwt()
				.jwt(jwt -> jwt.subject("55555555-5555-4555-8555-555555555555").claim("preferred_username", "john/doe"))
				.authorities(createAuthorityList("ROLE_JANUS_USER")))).andExpect(status().isBadRequest());
	}

	@Test
	@Sql(statements = {
			"INSERT INTO app_user(username,keycloak_subject,locale,time_format,default_timezone) VALUES('jdoe','11111111-1111-4111-8111-111111111111','en-US','H24','Europe/Madrid')" })
	void testDeleteShouldReturn204AndRemoveAccount() throws Exception {
		this.mvc.perform(delete(BASE + "/{username}", "jdoe").with(jwt()//
				.authorities(createAuthorityList("ROLE_JANUS_ADMIN")))) //
				.andExpect(status().isNoContent());

		this.entityManager.flush();
		org.assertj.core.api.Assertions.assertThat(this.jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM app_user WHERE username = ?", Integer.class, "jdoe")).isZero();
	}
}
