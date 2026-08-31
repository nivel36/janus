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

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;

class SecurityConfigTest {

	private static final String ISSUER = "https://issuer.example.test";

	@Test
	void shouldRejectTokenWithIncorrectAudience() {
		final OAuth2TokenValidator<Jwt> validator = new SecurityConfig().jwtValidator(ISSUER, "janus-api");
		final Jwt jwt = this.jwt(List.of("other-client"));

		assertThat(validator.validate(jwt).hasErrors()).isTrue();
	}

	@Test
	void shouldAcceptTokenWithExpectedAudience() {
		final OAuth2TokenValidator<Jwt> validator = new SecurityConfig().jwtValidator(ISSUER, "janus-api");
		final Jwt jwt = this.jwt(List.of("other-client", "janus-api"));

		assertThat(validator.validate(jwt).hasErrors()).isFalse();
	}

	private Jwt jwt(final List<String> audience) {
		final Instant now = Instant.now();
		return Jwt.withTokenValue("token").header("alg", "none").issuer(ISSUER).audience(audience)
				.issuedAt(now).notBefore(now.minusSeconds(1)).expiresAt(now.plusSeconds(300)).build();
	}
}
