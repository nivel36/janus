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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import es.nivel36.janus.api.v1.SecurityTestConfiguration;

@SpringBootTest(properties = {
		"spring.security.oauth2.resourceserver.jwt.issuer-uri=http://janus.local/auth/realms/Nivel36",
		"janus.user-provisioning.defaults.locale=es-ES", "janus.user-provisioning.defaults.time-format=H24",
		"janus.user-provisioning.defaults.default-timezone=Europe/Madrid" })
@AutoConfigureMockMvc
@Import(SecurityTestConfiguration.class)
class FirstRequestProvisioningIT {

	private static final String SUBJECT = "9a60b9f4-7436-4d93-9c25-08e08f3dfc58";
	private static final String USERNAME = "aferrer@nivel36.es";

	private @Autowired MockMvc mvc;
	private @Autowired JdbcClient jdbcClient;
	private @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer;

	@BeforeEach
	@AfterEach
	void removeLocalProfile() {
		this.jdbcClient.sql("DELETE FROM app_user WHERE keycloak_subject = :subject")
			.param("subject", SUBJECT)
			.update();
	}

	@Test
	void firstAuthenticatedRequestProvisionsLocalProfile() throws Exception {
		assertThat(countProfiles()).isZero();

		this.mvc.perform(get("/api/v1/appusers/me").with(jwt().jwt(token -> token.issuer(this.issuer)
			.subject(SUBJECT).claim("preferred_username", USERNAME))
			.authorities(createAuthorityList("ROLE_JANUS_USER"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.username").value(USERNAME))
			.andExpect(jsonPath("$.locale").value("es-ES"))
			.andExpect(jsonPath("$.timeFormat").value("H24"))
			.andExpect(jsonPath("$.defaultTimezone").value("Europe/Madrid"));

		assertThat(countProfiles()).isOne();
	}

	private long countProfiles() {
		return this.jdbcClient.sql("SELECT COUNT(*) FROM app_user WHERE keycloak_subject = :subject")
			.param("subject", SUBJECT)
			.query(Long.class)
			.single();
	}
}
