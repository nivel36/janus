package es.nivel36.janus.api.v1;

import java.time.Instant;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

@TestConfiguration
public class SecurityTestConfiguration {

	@Bean
	JwtDecoder jwtDecoder() {
		return token -> Jwt.withTokenValue(token) //
				.header("alg", "none") //
				.claim("sub", "jdoe") //
				.claim("scope", "read") //
				.issuedAt(Instant.now()) //
				.expiresAt(Instant.now().plusSeconds(3600)) //
				.build(); //
	}
}
