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
package es.nivel36.janus.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

class SecurityConfigTest {

	private final SecurityConfig securityConfig = new SecurityConfig();

	@Test
	void jwtAuthenticationConverterShouldMapRealmAndResourceRolesToAuthorities() {
		final Jwt jwt = buildJwt(Map.of(
				"realm_access", Map.of("roles", List.of("employee", "admin")),
				"resource_access", Map.of("janus-api", Map.of("roles", List.of("user")))));

		final AbstractAuthenticationToken authentication = securityConfig.jwtAuthenticationConverter().convert(jwt);

		assertThat(authentication).isNotNull();
		assertThat(authentication.getAuthorities()).extracting("authority")
				.contains("ROLE_EMPLOYEE", "ROLE_ADMIN", "ROLE_USER");
	}

	@Test
	void jwtAuthenticationConverterShouldKeepScopeAuthorities() {
		final Jwt jwt = buildJwt(Map.of("scope", "openid profile"));

		final AbstractAuthenticationToken authentication = securityConfig.jwtAuthenticationConverter().convert(jwt);

		assertThat(authentication).isNotNull();
		assertThat(authentication.getAuthorities()).extracting("authority")
				.contains("SCOPE_openid", "SCOPE_profile");
	}

	private Jwt buildJwt(final Map<String, Object> claims) {
		return new Jwt("token", Instant.now(), Instant.now().plusSeconds(300),
				Map.of("alg", "none"), claims);
	}
}
