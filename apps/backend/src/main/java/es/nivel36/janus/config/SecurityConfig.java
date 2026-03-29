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

import java.util.List;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.FrameOptionsConfig;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import es.nivel36.janus.util.KeycloakJwtRolesConverter;

/**
 * Security configuration for Janus.
 *
 * <p>
 * This configuration defines the HTTP security rules applied to incoming
 * requests, including stateless session management, CORS handling, CSRF
 * disabling, security headers, OAuth2 resource server support with JWT
 * authentication, and request authorization rules.
 *
 * <p>
 * All requests targeting {@code /api/**} require authentication, while any
 * other request is allowed without authentication.
 *
 * <p>
 * This configuration also customizes JWT authority extraction by combining the
 * default scope-based authorities with role-based authorities extracted from
 * the token.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	/**
	 * Content Security Policy applied to API responses.
	 *
	 * <p>
	 * This policy denies all resource loading and disables potentially unsafe
	 * browser behaviors such as embedding, form submission, and base URI usage.
	 */
	private static final String API_CONTENT_SECURITY_POLICY = "default-src 'none'; base-uri 'none'; frame-ancestors 'none'; form-action 'none'";

	/**
	 * Permissions Policy applied to API responses.
	 *
	 * <p>
	 * This policy disables access to a set of browser features for API endpoints,
	 * reducing the exposed surface for client-side capabilities.
	 */
	private static final String API_PERMISSIONS_POLICY = "accelerometer=(), camera=(), geolocation=(), gyroscope=(), magnetometer=(), microphone=(), payment=(), usb=()";

	/**
	 * Creates the main {@link SecurityFilterChain} used to secure HTTP requests.
	 *
	 * <p>
	 * This filter chain enables CORS with default handling, disables CSRF
	 * protection, configures stateless session management, applies several
	 * defensive HTTP headers, enforces authentication for {@code /api/**}
	 * endpoints, configures JWT-based OAuth2 resource server support, and returns
	 * {@link HttpStatus#UNAUTHORIZED} when authentication is required but missing
	 * or invalid.
	 *
	 * @param http the {@link HttpSecurity} builder used to configure web security.
	 *             Can't be {@code null}.
	 * @return the configured {@link SecurityFilterChain}.
	 * @throws Exception if the security configuration cannot be built.
	 */
	@Bean
	SecurityFilterChain securityFilterChain(final HttpSecurity http) throws Exception {
		return http.cors(Customizer.withDefaults()) //
				.csrf(CsrfConfigurer::disable) //
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) //
				.headers(headers -> { //
					headers.contentTypeOptions(Customizer.withDefaults()); //
					headers.frameOptions(FrameOptionsConfig::deny); //
					headers.referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.NO_REFERRER)); //
					headers.permissionsPolicyHeader(permissions -> permissions.policy(API_PERMISSIONS_POLICY)); //
					headers.contentSecurityPolicy(csp -> csp.policyDirectives(API_CONTENT_SECURITY_POLICY)); //
					headers.cacheControl(Customizer.withDefaults()); //
				}) //
				.authorizeHttpRequests(this::getAuthorizations) //
				.oauth2ResourceServer(
						oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(this.jwtAuthenticationConverter()))) //
				.exceptionHandling(exception -> exception
						.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))) //
				.build();
	}

	private AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry getAuthorizations(
			final AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
		return auth.requestMatchers("/api/**").authenticated() //
				.anyRequest().permitAll();
	}

	/**
	 * Creates the {@link CorsConfigurationSource} used for API endpoints.
	 *
	 * <p>
	 * The resulting configuration restricts cross-origin requests to the provided
	 * origins, allows a fixed set of HTTP methods and headers, disables credential
	 * sharing, and caches preflight responses for {@code 3600} seconds.
	 *
	 * <p>
	 * The CORS configuration is applied only to requests matching {@code /api/**}.
	 *
	 * @param allowedOrigins the list of allowed origins for cross-origin requests.
	 *                       Can be empty, but not expected to be {@code null}.
	 * @return a {@link CorsConfigurationSource} containing the configured CORS
	 *         rules.
	 */
	@Bean
	CorsConfigurationSource corsConfigurationSource(
			@Value("${janus.cors.allowed-origins}") final List<String> allowedOrigins) {
		final CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(allowedOrigins);
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin"));
		config.setAllowCredentials(false);
		config.setMaxAge(3600L);

		final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/api/**", config);
		return source;
	}

	/**
	 * Creates the {@link JwtAuthenticationConverter} used to derive authorities
	 * from a JWT.
	 *
	 * <p>
	 * The returned converter combines authorities produced by the default
	 * {@link JwtGrantedAuthoritiesConverter} with additional authorities extracted
	 * through
	 * {@link KeycloakJwtRolesConverter#extract(org.springframework.security.oauth2.jwt.Jwt)}.
	 * Duplicate authorities are removed from the final result.
	 *
	 * @return a {@link JwtAuthenticationConverter} that maps JWT claims to granted
	 *         authorities.
	 */
	@Bean
	JwtAuthenticationConverter jwtAuthenticationConverter() {
		final JwtGrantedAuthoritiesConverter scopesConverter = new JwtGrantedAuthoritiesConverter();

		final JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();
		authenticationConverter.setJwtGrantedAuthoritiesConverter(jwt -> Stream
				.concat(scopesConverter.convert(jwt).stream(), KeycloakJwtRolesConverter.extract(jwt).stream())
				.distinct().toList());
		return authenticationConverter;
	}
}
