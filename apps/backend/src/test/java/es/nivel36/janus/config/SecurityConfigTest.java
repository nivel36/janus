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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class SecurityConfigTest {

	private static final String ISSUER = "https://issuer.example.test";

	@Test
	void shouldRejectTokenWithIncorrectAudience() {
		final OAuth2TokenValidator<Jwt> validator = new SecurityConfig().jwtValidator(ISSUER, "janus-api");
		final Jwt jwt = this.jwt(List.of("other-client"));

		assertThat(validator.validate(jwt).hasErrors()).isTrue();
	}

	@Test
	void shouldRejectTokenWithoutAudienceEvenWhenAuthorizedPartyIsTheSpa() {
		final OAuth2TokenValidator<Jwt> validator = new SecurityConfig().jwtValidator(ISSUER, "janus-api");
		final Instant now = Instant.now();
		final Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").issuer(ISSUER)
				.subject("immutable-provider-id").claim("azp", "janus-spa").issuedAt(now)
				.expiresAt(now.plusSeconds(300)).build();

		assertThat(validator.validate(jwt).hasErrors()).isTrue();
	}

	@Test
	void shouldAcceptTokenWithExpectedAudience() {
		final OAuth2TokenValidator<Jwt> validator = new SecurityConfig().jwtValidator(ISSUER, "janus-api");
		final Jwt jwt = this.jwt(List.of("other-client", "janus-api"));

		assertThat(validator.validate(jwt).hasErrors()).isFalse();
	}

	@Test
	void shouldUseSubjectAsPrincipal() {
		final Jwt jwt = this.jwt(List.of("janus-api"), "person@example.test", true, "old-login");
		final JwtAuthenticationToken authentication = (JwtAuthenticationToken) new SecurityConfig()
				.jwtAuthenticationConverter("janus-api").convert(jwt);

		assertThat(authentication.getName()).isEqualTo("immutable-provider-id");
		assertThat(AuthenticatedIdentity.externalIdentity(authentication))
				.isEqualTo(new ExternalIdentity(ISSUER, "immutable-provider-id"));
	}

	@Test
	void shouldIgnoreChangedPreferredUsernameAndEmailForExternalIdentity() {
		final Jwt first = this.jwt(List.of("janus-api"), "person@example.test", true, "old-login");
		final Jwt renamed = this.jwt(List.of("janus-api"), "renamed@example.test", true, "new-login");

		assertThat(AuthenticatedIdentity.externalIdentity(new JwtAuthenticationToken(first)))
				.isEqualTo(AuthenticatedIdentity.externalIdentity(new JwtAuthenticationToken(renamed)));
	}

	@Test
	void shouldNormalizeEmailCase() {
		final Jwt jwt = this.jwt(List.of("janus-api"), " Person@Example.TEST ", true, "person");

		assertThat(AuthenticatedIdentity.matchesEmail(new JwtAuthenticationToken(jwt), "person@example.test")).isTrue();
	}

	@Test
	void shouldAcceptMissingEmailAsIdentityDoesNotDependOnIt() {
		final Jwt jwt = this.jwt(List.of("janus-api"), null, true, "person");

		assertThat(new SecurityConfig().jwtValidator(ISSUER, "janus-api").validate(jwt).hasErrors()).isFalse();
		org.assertj.core.api.Assertions.assertThatThrownBy(
				() -> AuthenticatedIdentity.email(new JwtAuthenticationToken(jwt)))
				.isInstanceOf(BadCredentialsException.class);
	}

	@Test
	void shouldAcceptUnverifiedEmailForGlobalIdentityValidation() {
		final Jwt jwt = this.jwt(List.of("janus-api"), "person@example.test", false, "person");

		assertThat(new SecurityConfig().jwtValidator(ISSUER, "janus-api").validate(jwt).hasErrors()).isFalse();
		org.assertj.core.api.Assertions.assertThatThrownBy(
				() -> AuthenticatedIdentity.email(new JwtAuthenticationToken(jwt)))
				.isInstanceOf(BadCredentialsException.class);
	}

	@Test
	void shouldRejectExternalIdentityWithoutIssuer() {
		final Instant now = Instant.now();
		final Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject("subject").issuedAt(now)
				.expiresAt(now.plusSeconds(60)).build();
		org.assertj.core.api.Assertions.assertThatThrownBy(
				() -> AuthenticatedIdentity.externalIdentity(new JwtAuthenticationToken(jwt)))
				.isInstanceOf(BadCredentialsException.class);
	}

	@Test
	void shouldRejectExternalIdentityWithoutSubject() {
		final Instant now = Instant.now();
		final Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").issuer(ISSUER).issuedAt(now)
				.expiresAt(now.plusSeconds(60)).build();
		org.assertj.core.api.Assertions.assertThatThrownBy(
				() -> AuthenticatedIdentity.externalIdentity(new JwtAuthenticationToken(jwt)))
				.isInstanceOf(BadCredentialsException.class);
	}

	@Test
	void shouldScopeSameSubjectByIssuer() {
		final Jwt first = this.jwt(List.of("janus-api"));
		final Instant now = Instant.now();
		final Jwt second = Jwt.withTokenValue("token").header("alg", "none").issuer("https://other.test")
				.subject("immutable-provider-id").issuedAt(now).expiresAt(now.plusSeconds(60)).build();
		assertThat(AuthenticatedIdentity.externalIdentity(new JwtAuthenticationToken(first)))
				.isNotEqualTo(AuthenticatedIdentity.externalIdentity(new JwtAuthenticationToken(second)));
	}

	private Jwt jwt(final List<String> audience) {
		return this.jwt(audience, "person@example.test", true, "person");
	}

	private Jwt jwt(final List<String> audience, final String email, final boolean emailVerified,
			final String preferredUsername) {
		final Instant now = Instant.now();
		final Jwt.Builder builder = Jwt.withTokenValue("token").header("alg", "none").issuer(ISSUER).audience(audience)
				.subject("immutable-provider-id").claim("email_verified", emailVerified)
				.claim("preferred_username", preferredUsername).issuedAt(now).notBefore(now.minusSeconds(1))
				.expiresAt(now.plusSeconds(300));
		if (email != null) {
			builder.claim("email", email);
		}
		return builder.build();
	}
}
