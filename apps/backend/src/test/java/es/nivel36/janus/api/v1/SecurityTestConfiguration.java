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
package es.nivel36.janus.api.v1;

import java.time.Instant;
import java.util.List;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

@TestConfiguration
public class SecurityTestConfiguration {

	public static JwtRequestPostProcessor verifiedJwt() {
		return jwt().jwt(jwt -> jwt.claim("email_verified", true));
	}

	@Bean
	@Primary
	JwtDecoder testJwtDecoder(final OAuth2TokenValidator<Jwt> jwtValidator) {
		return token -> {
			final Jwt.Builder builder = Jwt.withTokenValue(token) //
				.header("alg", "none") //
				.issuer("http://localhost:8180/realms/Nivel36") //
				.audience(List.of("janus-api")) //
				.claim("sub", "provider-account-id") //
				.claim("email", token) //
				.claim("preferred_username", "renamed-user") //
				.claim("scope", "read") //
				.issuedAt(Instant.now()) //
				.expiresAt(Instant.now().plusSeconds(3600));
			if (!"email-unverified".equals(token)) {
				builder.claim("email_verified", true);
			}
			final Jwt jwt = builder.build();
			final var result = jwtValidator.validate(jwt);
			if (result.hasErrors()) {
				throw new JwtValidationException("Invalid test JWT", result.getErrors());
			}
			return jwt;
		};
	}
}
