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

import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Utility class responsible for extracting {@link GrantedAuthority} instances
 * from a Keycloak {@link Jwt}.
 *
 * <p>
 * This converter reads client roles only from the configured client's entry in
 * {@code resource_access}. Realm roles and roles belonging to any other client
 * are deliberately ignored. Extracted roles are normalized by:
 * <ul>
 * <li>Trimming whitespace</li>
 * <li>Filtering out blank values</li>
 * <li>Converting to uppercase</li>
 * <li>Prefixing with {@code ROLE_}</li>
 * </ul>
 *
 * <p>
 * The resulting authorities are returned as a distinct collection, meaning
 * duplicated roles across different sections of the token are removed.
 *
 * <p>
 * This class is not intended to be instantiated.
 */
public class KeycloakJwtRolesConverter {

	/**
	 * Private constructor to prevent instantiation of this utility class.
	 */
	private KeycloakJwtRolesConverter() {
	}

	/**
	 * Extracts the configured client's roles from the given {@link Jwt} and converts them into a
	 * collection of {@link GrantedAuthority}.
	 *
	 * <p>
	 * @param jwt the JWT token from which roles are extracted. Can't be
	 *            {@code null}.
	 * @param clientId the resource client whose roles are trusted. Can't be
	 *                 {@code null}.
	 * @return a collection of unique {@link GrantedAuthority} derived from the
	 *         token.
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

		return roleValues.stream() //
				.filter(String.class::isInstance) //
				.map(String.class::cast) //
				.map(String::trim) //
				.filter(role -> !role.isBlank()) //
				.map(String::toUpperCase) //
				.map(role -> new SimpleGrantedAuthority("ROLE_" + role));
	}
}
