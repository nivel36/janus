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
		final Jwt jwt = buildJwt(Map.of(
				"realm_access", Map.of("roles", List.of("JANUS_EMPLOYEE", "JANUS_ADMIN")),
				"resource_access", Map.of("janus-api", Map.of("roles", List.of("JANUS_USER")))));

		final  Collection<GrantedAuthority> authorities = KeycloakJwtRolesConverter.extract(jwt);

		assertThat(authorities).extracting("authority")
				.contains("ROLE_JANUS_EMPLOYEE", "ROLE_JANUS_ADMIN", "ROLE_JANUS_USER");
	}

	private Jwt buildJwt(final Map<String, Object> claims) {
		return new Jwt("token", Instant.now(), Instant.now().plusSeconds(300),
				Map.of("alg", "none"), claims);
	}
}
