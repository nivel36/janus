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
package es.nivel36.janus.api.v1.auth;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.nivel36.janus.service.auth.AuthService;
import es.nivel36.janus.util.Strings;
import jakarta.validation.Valid;

/**
 * REST controller exposing authentication endpoints.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
	private static final String BEARER_PREFIX = "Bearer ";

	private final AuthService authService;

	public AuthController(final AuthService authService) {
		this.authService = Objects.requireNonNull(authService, "authService cannot be null");
	}

	@PostMapping("/login")
	public ResponseEntity<LoginResponse> login(@Valid @RequestBody final LoginRequest request) {
		logger.debug("Login ACTION performed");
		final String token = this.authService.login(request.username(), request.password());
		return ResponseEntity.ok(new LoginResponse(token, request.username()));
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(@RequestHeader("Authorization") final String authorization) {
		logger.debug("Logout ACTION performed");
		final String token = extractBearerToken(authorization);
		this.authService.logout(token);
		return ResponseEntity.noContent().build();
	}

	private String extractBearerToken(final String authorization) {
		Strings.requireNonBlank(authorization, "Authorization header must not be blank.");
		if (!authorization.startsWith(BEARER_PREFIX)) {
			throw new IllegalArgumentException("Authorization header must use Bearer scheme.");
		}
		final String token = authorization.substring(BEARER_PREFIX.length()).trim();
		return Strings.requireNonBlank(token, "Authorization token must not be blank.");
	}
}
