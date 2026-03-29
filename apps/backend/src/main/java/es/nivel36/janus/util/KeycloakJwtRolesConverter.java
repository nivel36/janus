package es.nivel36.janus.util;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

public class KeycloakJwtRolesConverter {
	
	private KeycloakJwtRolesConverter() {};

	public static Collection<GrantedAuthority> extract(final Jwt jwt) {
		return Stream.concat(extractRealmRoles(jwt), extractResourceRoles(jwt)).distinct().toList();
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

		return roleValues.stream().filter(String.class::isInstance).map(String.class::cast).map(String::trim)
				.filter(role -> !role.isBlank()).map(role -> role.toUpperCase())
				.map(role -> new SimpleGrantedAuthority("ROLE_" + role));
	}

}
