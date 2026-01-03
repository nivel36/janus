/*
 * Copyright 2025 Abel Ferrer Jiménez
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
package es.nivel36.janus.service.auth;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import es.nivel36.janus.service.appuser.AppUser;
import es.nivel36.janus.service.appuser.AppUserService;
import es.nivel36.janus.util.Strings;

/**
 * Handles login/logout operations backed by in-memory sessions.
 */
@Service
public class AuthService {

	private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

	private final AppUserService appUserService;
	private final Clock clock;
	private final Map<String, AuthSession> sessions = new ConcurrentHashMap<>();

	public AuthService(final AppUserService appUserService, final Clock clock) {
		this.appUserService = Objects.requireNonNull(appUserService, "appUserService cannot be null");
		this.clock = Objects.requireNonNull(clock, "clock cannot be null");
	}

	public String login(final String username, final String password) {
		final AppUser appUser = this.appUserService.authenticate(username, password);
		final String token = UUID.randomUUID().toString();
		final AuthSession session = new AuthSession(appUser.getUsername(), Instant.now(clock));
		this.sessions.put(token, session);
		logger.info("Auth session created for user {}", appUser.getUsername());
		return token;
	}

	public void logout(final String token) {
		Strings.requireNonBlank(token, "token cannot be null or blank.");
		final AuthSession removed = this.sessions.remove(token);
		if (removed == null) {
			throw new AuthenticationFailedException("Session not found or already logged out.");
		}
		logger.info("Auth session removed for user {}", removed.username());
	}
}
