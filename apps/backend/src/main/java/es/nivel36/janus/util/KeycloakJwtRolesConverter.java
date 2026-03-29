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
 * This converter reads both realm-level roles (from {@code realm_access}) and
 * client/resource-level roles (from {@code resource_access}) contained in the
 * JWT claims. All extracted roles are normalized by:
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
	 * Extracts all roles from the given {@link Jwt} and converts them into a
	 * collection of {@link GrantedAuthority}.
	 *
	 * <p>
	 * This method combines roles from both realm and resource access sections of
	 * the token. The resulting collection contains distinct authorities.
	 *
	 * @param jwt the JWT token from which roles are extracted. Can't be
	 *            {@code null}.
	 * @return a collection of unique {@link GrantedAuthority} derived from the
	 *         token.
	 */
	public static Collection<GrantedAuthority> extract(final Jwt jwt) {
		return Stream.concat(extractRealmRoles(jwt), extractResourceRoles(jwt)) //
				.distinct() //
				.toList();
	}

	private static Stream<GrantedAuthority> extractRealmRoles(final Jwt jwt) {
		final Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
		if (realmAccess == null) {
			return Stream.empty();
		}

		return getRolesFromMap(realmAccess);
	}

	private static Stream<GrantedAuthority> extractResourceRoles(final Jwt jwt) {
		final Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
		if (resourceAccess == null) {
			return Stream.empty();
		}

		return resourceAccess.values().stream().filter(Map.class::isInstance).map(Map.class::cast)
				.flatMap(KeycloakJwtRolesConverter::getRolesFromMap);
	}

	private static Stream<GrantedAuthority> getRolesFromMap(final Map<String, Object> source) {
		final Object roles = source.get("roles");
		if (!(roles instanceof Collection<?> roleValues)) {
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
