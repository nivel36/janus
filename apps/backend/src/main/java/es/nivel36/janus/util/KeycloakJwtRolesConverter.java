/*
 * Copyright 2026 Abel Ferrer Jiménez
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package es.nivel36.janus.util;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Extracts Spring Security authorities from the configured Keycloak client's
 * roles.
 * <p>
 * Only the client's entry in {@code resource_access} is considered. Realm roles
 * and other clients' roles are ignored. String roles are trimmed, blank values
 * are omitted, and remaining values are uppercased using the default locale and
 * prefixed with {@code ROLE_}. Duplicate authorities are removed.
 * <p>
 * Missing client data or a non-collection {@code roles} value produces an empty
 * collection. Non-string role elements are ignored.
 */
public class KeycloakJwtRolesConverter {

	private KeycloakJwtRolesConverter() {
	}

	/**
	 * Returns the unique authorities derived from the configured client's roles.
	 *
	 * @param  jwt                  the token whose client roles are read; must not
	 *                              be {@code null}
	 * @param  clientId             the resource client whose roles are selected
	 * @return                      an unmodifiable collection of authorities,
	 *                              possibly empty
	 * @throws NullPointerException if {@code jwt} is {@code null}
	 */
	public static Collection<GrantedAuthority> extract(final Jwt jwt, final String clientId) {
		final Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
		if (resourceAccess == null) {
			return java.util.List.of();
		}

		final Object clientAccess = resourceAccess.get(clientId);
		if (!(clientAccess instanceof final Map<?, ?> clientRoles)) {
			return java.util.List.of();
		}

		@SuppressWarnings("unchecked")
		final Map<String, Object> roles = (Map<String, Object>) clientRoles;
		return getRolesFromMap(roles).distinct().toList();
	}

	private static Stream<GrantedAuthority> getRolesFromMap(final Map<String, Object> source) {
		final Object roles = source.get("roles");
		if (!(roles instanceof final Collection<?> roleValues)) {
			return Stream.empty();
		}

		return roleValues.stream().filter(String.class::isInstance).map(String.class::cast).map(String::trim)
				.filter(role -> !role.isBlank()).map(String::toUpperCase)
				.map(role -> new SimpleGrantedAuthority("ROLE_" + role));
	}
}
