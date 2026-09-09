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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import es.nivel36.janus.api.v1.SecurityTestConfiguration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(SecurityTestConfiguration.class)
@Transactional
class AppUserControllerIT {

	private @Autowired MockMvc mvc;

	private static final String BASE = "/api/v1/appusers";

	@Test
	@Sql(statements = {
			"INSERT INTO schedule(id,code,name) VALUES(1,'STD','Standard')",
			"INSERT INTO employee(id,name,surname,email,schedule_id) VALUES(10,'Alice','One','alice@example.test',1)" })
	void adminCanProvisionEmployeeAndEmployeeCannotBeLinkedTwice() throws Exception {
		final String first = """
				{"username":"alice","keycloakSubject":"11111111-1111-4111-8111-111111111111","locale":"en-US","timeFormat":"H24","defaultTimezone":"UTC","employeeId":10}
				""";
		this.mvc.perform(post(BASE).contentType(APPLICATION_JSON).content(first).with(jwt()
				.authorities(createAuthorityList("ROLE_JANUS_ADMIN"))))
				.andExpect(status().isCreated());

		final String duplicate = """
				{"username":"alice2","keycloakSubject":"22222222-2222-4222-8222-222222222222","locale":"en-US","timeFormat":"H24","defaultTimezone":"UTC","employeeId":10}
				""";
		this.mvc.perform(post(BASE).contentType(APPLICATION_JSON).content(duplicate).with(jwt()
				.authorities(createAuthorityList("ROLE_JANUS_ADMIN"))))
				.andExpect(status().isBadRequest());
	}

	@Test
	@Sql(statements = {
			"INSERT INTO app_user(username,keycloak_subject,locale,time_format,default_timezone) VALUES('jdoe','11111111-1111-4111-8111-111111111111','en-US','H24','Europe/Madrid')" })
	void testFindByUsernameShouldReturnUser() throws Exception {
		this.mvc.perform(get(BASE + "/{username}", "jdoe").with(jwt()//
				.authorities(createAuthorityList("ROLE_JANUS_ADMIN")))) //
				.andExpect(status().isOk()) //
				.andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON)) //
				.andExpect(jsonPath("$.username").value("jdoe")) //
				.andExpect(jsonPath("$.locale").value("en-US")) //
				.andExpect(jsonPath("$.timeFormat").value("H24")) //
				.andExpect(jsonPath("$.defaultTimezone").value("Europe/Madrid"));
	}

	@Test
	void testFindUnknownUserShouldReturn404() throws Exception {
		this.mvc.perform(get(BASE + "/{username}", "unknown").with(jwt()//
				.authorities(createAuthorityList("ROLE_JANUS_ADMIN")))) //
				.andExpect(status().isNotFound());
	}

	@Test
	void testFindWithInvalidPatternShouldFail400() throws Exception {
		this.mvc.perform(get(BASE + "/{username}", "bad user").with(jwt())) //
				.andExpect(status().isBadRequest());
	}

	@Test
	@Sql(statements = {
			"INSERT INTO app_user(username,keycloak_subject,locale,time_format,default_timezone) VALUES('jdoe','11111111-1111-4111-8111-111111111111','en-US','H24','Europe/Madrid')" })
	void testCreateAlreadyExistsShouldReturn400() throws Exception {
		final String body = """
				  {"username":"jdoe","keycloakSubject":"22222222-2222-4222-8222-222222222222","locale":"en-US","timeFormat":"H24","defaultTimezone":"Europe/Madrid"}
				""";

		this.mvc.perform(post(BASE).contentType(APPLICATION_JSON).content(body).with(jwt()//
				.authorities(createAuthorityList("ROLE_JANUS_ADMIN")))) //
				.andExpect(status().isBadRequest());
	}

	@Test
	void testCreateShouldReturn201AndBody() throws Exception {
		final String body = """
				  {"username":"asmith","keycloakSubject":"22222222-2222-4222-8222-222222222222","locale":"en-GB","timeFormat":"H12","defaultTimezone":"Europe/London"}
				""";

		this.mvc.perform(post(BASE).contentType(APPLICATION_JSON).content(body).with(jwt()//
				.authorities(createAuthorityList("ROLE_JANUS_ADMIN")))) //
				.andExpect(status().isCreated()) //
				.andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON)) //
				.andExpect(jsonPath("$.username").value("asmith")) //
				.andExpect(jsonPath("$.locale").value("en-GB")) //
				.andExpect(jsonPath("$.timeFormat").value("H12")) //
				.andExpect(jsonPath("$.defaultTimezone").value("Europe/London"));
		this.mvc.perform(get(BASE + "/{username}", "asmith").with(jwt()//
				.authorities(createAuthorityList("ROLE_JANUS_ADMIN")))).andExpect(status().isOk());
	}

	@Test
	void testCreateShouldRejectInvalidTimezone() throws Exception {
		final String body = """
				  {"username":"asmith","keycloakSubject":"22222222-2222-4222-8222-222222222222","locale":"en-GB","timeFormat":"H12","defaultTimezone":"Mars/Olympus"}
				""";

		this.mvc.perform(post(BASE).contentType(APPLICATION_JSON).content(body).with(jwt()//
				.authorities(createAuthorityList("ROLE_JANUS_ADMIN")))) //
				.andExpect(status().isBadRequest());
	}

	@Test
	void testCreateShouldAcceptUsernamesWithAtSign() throws Exception {
		final String body = """
				  {"username":"alice@example.com","keycloakSubject":"22222222-2222-4222-8222-222222222222","locale":"en-GB","timeFormat":"H12","defaultTimezone":"Europe/London"}
				""";

		this.mvc.perform(post(BASE).contentType(APPLICATION_JSON).content(body).with(jwt()//
				.authorities(createAuthorityList("ROLE_JANUS_ADMIN")))) //
				.andExpect(status().isCreated()) //
				.andExpect(jsonPath("$.username").value("alice@example.com")) //
				.andExpect(jsonPath("$.defaultTimezone").value("Europe/London"));

		this.mvc.perform(get(BASE + "/{username}", "alice@example.com").with(jwt()//
				.authorities(createAuthorityList("ROLE_JANUS_ADMIN")))) //
				.andExpect(status().isOk()) //
				.andExpect(jsonPath("$.username").value("alice@example.com"));
	}

	@Test
	@Sql(statements = {
			"INSERT INTO app_user(username,keycloak_subject,locale,time_format,default_timezone) VALUES('jdoe','11111111-1111-4111-8111-111111111111','en-US','H24','Europe/Madrid')" })
	void testUpdateShouldReturn200AndUpdatedBody() throws Exception {
		final String body = """
				  {"locale":"en-CA","timeFormat":"H12","defaultTimezone":"America/Toronto"}
				""";

		this.mvc.perform(put(BASE + "/{username}", "jdoe").with(jwt()//
				.authorities(createAuthorityList("ROLE_JANUS_ADMIN"))) //
				.contentType(APPLICATION_JSON).content(body)) //
				.andExpect(status().isOk()) //
				.andExpect(jsonPath("$.username").value("jdoe")) //
				.andExpect(jsonPath("$.locale").value("en-CA")) //
				.andExpect(jsonPath("$.timeFormat").value("H12")) //
				.andExpect(jsonPath("$.defaultTimezone").value("America/Toronto"));
	}

	@Test
	@Sql(statements = {
			"INSERT INTO app_user(username,keycloak_subject,locale,time_format,default_timezone) VALUES('jdoe','11111111-1111-4111-8111-111111111111','en-US','H24','Europe/Madrid')" })
	void testMeUpdatesBySubjectDespiteCopiedMutableClaims() throws Exception {
		final String body = """
				  {"locale":"en-CA","timeFormat":"H12","defaultTimezone":"America/Toronto"}
				""";

		this.mvc.perform(put(BASE + "/me").with(jwt().jwt(jwt -> jwt.issuer("https://issuer.example.test")
				.subject("11111111-1111-4111-8111-111111111111")
				.claim("email", "someone-else@example.com").claim("preferred_username", "someone-else"))
				.authorities(createAuthorityList("ROLE_JANUS_USER"))).contentType(APPLICATION_JSON).content(body))
				.andExpect(status().isOk()).andExpect(jsonPath("$.username").value("jdoe"));
	}

	@Test
	@Sql(statements = {
			"INSERT INTO app_user(username,keycloak_subject,locale,time_format,default_timezone) VALUES('jdoe','11111111-1111-4111-8111-111111111111','en-US','H24','Europe/Madrid')" })
	void testMeFindsProvisionedIdentity() throws Exception {
		this.mvc.perform(get(BASE + "/me").with(jwt().jwt(jwt -> jwt.issuer("https://issuer.example.test")
				.subject("11111111-1111-4111-8111-111111111111").claim("preferred_username", "changed"))
				.authorities(createAuthorityList("ROLE_JANUS_USER"))))
				.andExpect(status().isOk()).andExpect(jsonPath("$.username").value("jdoe"));
	}

	@Test
	void testMeCreatesUnprovisionedIdentityWithInitialPreferences() throws Exception {
		this.mvc.perform(get(BASE + "/me").with(jwt().jwt(jwt -> jwt.issuer("https://issuer.example.test")
				.subject("99999999-9999-4999-8999-999999999999").claim("preferred_username", "new-user"))
				.authorities(createAuthorityList("ROLE_JANUS_USER"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("new-user"))
				.andExpect(jsonPath("$.locale").value("en"))
				.andExpect(jsonPath("$.timeFormat").value("H24"))
				.andExpect(jsonPath("$.defaultTimezone").value("UTC"));

		this.mvc.perform(get(BASE + "/me").with(jwt().jwt(jwt -> jwt.issuer("https://issuer.example.test")
				.subject("99999999-9999-4999-8999-999999999999").claim("preferred_username", "renamed-user"))
				.authorities(createAuthorityList("ROLE_JANUS_USER"))))
				.andExpect(status().isOk()).andExpect(jsonPath("$.username").value("new-user"));
	}

	@Test
	void testMeRejectsMissingOrOversizedPreferredUsernameOnFirstAccess() throws Exception {
		this.mvc.perform(get(BASE + "/me").with(jwt().jwt(jwt -> jwt
				.subject("77777777-7777-4777-8777-777777777777"))
				.authorities(createAuthorityList("ROLE_JANUS_USER"))))
				.andExpect(status().isBadRequest());

		this.mvc.perform(get(BASE + "/me").with(jwt().jwt(jwt -> jwt
				.subject("88888888-8888-4888-8888-888888888888")
				.claim("preferred_username", "x".repeat(51)))
				.authorities(createAuthorityList("ROLE_JANUS_USER"))))
				.andExpect(status().isBadRequest());
	}

	@Test
	void testNonAdminCannotProvision() throws Exception {
		final String body = """
				  {"username":"asmith","keycloakSubject":"22222222-2222-4222-8222-222222222222","locale":"en-GB","timeFormat":"H12","defaultTimezone":"Europe/London"}
				""";
		this.mvc.perform(post(BASE).contentType(APPLICATION_JSON).content(body).with(jwt()
				.authorities(createAuthorityList("ROLE_JANUS_USER"))))
				.andExpect(status().isForbidden());
	}

	@Test
	@Sql(statements = {
			"INSERT INTO app_user(username,keycloak_subject,locale,time_format,default_timezone) VALUES('jdoe','11111111-1111-4111-8111-111111111111','en-US','H24','Europe/Madrid')" })
	void testDeleteShouldReturn204AndRemoveFromList() throws Exception {
		this.mvc.perform(delete(BASE + "/{username}", "jdoe").with(jwt()//
				.authorities(createAuthorityList("ROLE_JANUS_ADMIN")))) //
				.andExpect(status().isNoContent());

		this.mvc.perform(get(BASE + "/{username}", "jdoe").with(jwt()//
				.authorities(createAuthorityList("ROLE_JANUS_ADMIN")))) //
				.andExpect(status().isNotFound()); //
	}
}
