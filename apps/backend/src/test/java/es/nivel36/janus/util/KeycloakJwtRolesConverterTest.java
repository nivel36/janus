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
package es.nivel36.janus.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

public class KeycloakJwtRolesConverterTest {

	@Test
	void jwtAuthenticationConverterShouldMapRealmAndResourceRolesToAuthorities() {
		final Jwt jwt = this.buildJwt(Map.of("realm_access", Map.of("roles", List.of("JANUS_EMPLOYEE", "JANUS_ADMIN")),
				"resource_access", Map.of("janus-api", Map.of("roles", List.of("JANUS_USER")))));

		final Collection<GrantedAuthority> authorities = KeycloakJwtRolesConverter.extract(jwt);

		assertThat(authorities).extracting("authority").contains("ROLE_JANUS_EMPLOYEE", "ROLE_JANUS_ADMIN",
				"ROLE_JANUS_USER");
	}

	private Jwt buildJwt(final Map<String, Object> claims) {
		return new Jwt("token", Instant.now(), Instant.now().plusSeconds(300), Map.of("alg", "none"), claims);
	}
}
