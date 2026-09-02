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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.core.authority.AuthorityUtils.createAuthorityList;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import es.nivel36.janus.api.v1.SecurityTestConfiguration;

@SpringBootTest(properties = {
		"spring.security.oauth2.resourceserver.jwt.issuer-uri=http://janus.local/auth/realms/Nivel36",
		"janus.bootstrap.initial-user.enabled=true", "janus.bootstrap.initial-user.username=aferrer@nivel36.es",
		"janus.bootstrap.initial-user.subject=9a60b9f4-7436-4d93-9c25-08e08f3dfc58",
		"janus.bootstrap.initial-user.locale=es-ES", "janus.bootstrap.initial-user.time-format=H24",
		"janus.bootstrap.initial-user.default-timezone=Europe/Madrid" })
@AutoConfigureMockMvc
@Import(SecurityTestConfiguration.class)
class DeployedIssuerInitialUserIT {

	private @Autowired MockMvc mvc;
	private @Autowired JdbcClient jdbcClient;
	private @Autowired @Qualifier("initialAppUserInitializer") ApplicationRunner initialAppUserInitializer;
	private @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer;

	@Test
	void configuredDeploymentIssuerFindsInitialUser() throws Exception {
		this.mvc.perform(get("/api/v1/appusers/me").with(jwt().jwt(token -> token.issuer(this.issuer)
			.subject("9a60b9f4-7436-4d93-9c25-08e08f3dfc58"))
			.authorities(createAuthorityList("ROLE_JANUS_USER"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.username").value("aferrer@nivel36.es"));
	}

	@Test
	@Transactional
	void existingUsernameKeepsItsExternalIdentityWhenInitializerRunsAgain() throws Exception {
		final String existingIssuer = "https://existing.example/realms/Existing";
		final String existingSubject = "9423793d-786b-438b-a162-cfab4c324d9b";
		this.jdbcClient.sql("""
				UPDATE app_user
				SET identity_issuer = :issuer, identity_subject = :subject
				WHERE username = 'aferrer@nivel36.es'
				""")
			.param("issuer", existingIssuer)
			.param("subject", existingSubject)
			.update();

		this.initialAppUserInitializer.run(new DefaultApplicationArguments());

		final Map<String, Object> identity = this.jdbcClient.sql("""
				SELECT identity_issuer, identity_subject
				FROM app_user
				WHERE username = 'aferrer@nivel36.es'
				""")
			.query()
			.singleRow();
		assertThat(identity).containsEntry("IDENTITY_ISSUER", existingIssuer)
			.containsEntry("IDENTITY_SUBJECT", existingSubject);
	}
}
