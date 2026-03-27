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

import java.util.Collection;
import java.util.List;
import java.util.Map;
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
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	private static final String API_CONTENT_SECURITY_POLICY = "default-src 'none'; base-uri 'none'; frame-ancestors 'none'; form-action 'none'";
	private static final String API_PERMISSIONS_POLICY = "accelerometer=(), camera=(), geolocation=(), gyroscope=(), magnetometer=(), microphone=(), payment=(), usb=()";

	@Bean
	SecurityFilterChain securityFilterChain(final HttpSecurity http) throws Exception {
		return http.cors(Customizer.withDefaults()) //
				.csrf(CsrfConfigurer::disable) //
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) //
				.headers(headers -> { //
					headers.contentTypeOptions(Customizer.withDefaults()); //
					headers.frameOptions(frameOptions -> frameOptions.deny()); //
					headers.referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.NO_REFERRER)); //
					headers.permissionsPolicyHeader(permissions -> permissions.policy(API_PERMISSIONS_POLICY)); //
					headers.contentSecurityPolicy(csp -> csp.policyDirectives(API_CONTENT_SECURITY_POLICY)); //
					headers.cacheControl(Customizer.withDefaults()); //
				}) //
				.authorizeHttpRequests(this::getAuthorizations) //
				.oauth2ResourceServer(
						oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))) //
				.exceptionHandling(exception -> exception
						.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))) //
				.build();
	}

	private AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry getAuthorizations(
			final AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
		return auth.requestMatchers("/api/**").authenticated() //
				.anyRequest().permitAll();
	}

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

	@Bean
	JwtAuthenticationConverter jwtAuthenticationConverter() {
		final JwtGrantedAuthoritiesConverter scopesConverter = new JwtGrantedAuthoritiesConverter();

		final JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();
		authenticationConverter.setJwtGrantedAuthoritiesConverter(jwt -> Stream.concat(
				scopesConverter.convert(jwt).stream(),
				extractKeycloakRoles(jwt).stream()).distinct().toList());
		return authenticationConverter;
	}

	private Collection<GrantedAuthority> extractKeycloakRoles(final Jwt jwt) {
		return Stream.concat(
				extractRealmRoles(jwt),
				extractResourceRoles(jwt)).distinct().toList();
	}

	private Stream<GrantedAuthority> extractRealmRoles(final Jwt jwt) {
		final Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
		if (realmAccess == null) {
			return Stream.empty();
		}

		return getRolesFromMap(realmAccess);
	}

	private Stream<GrantedAuthority> extractResourceRoles(final Jwt jwt) {
		final Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
		if (resourceAccess == null) {
			return Stream.empty();
		}

		return resourceAccess.values().stream().filter(Map.class::isInstance).map(Map.class::cast)
				.flatMap(this::getRolesFromMap);
	}

	private Stream<GrantedAuthority> getRolesFromMap(final Map<String, Object> source) {
		final Object roles = source.get("roles");
		if (!(roles instanceof Collection<?> roleValues)) {
			return Stream.empty();
		}

		return roleValues.stream().filter(String.class::isInstance).map(String.class::cast).map(String::trim)
				.filter(role -> !role.isBlank()).map(role -> role.toUpperCase())
				.map(role -> new SimpleGrantedAuthority("ROLE_" + role));
	}
}
