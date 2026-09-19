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
package es.nivel36.janus.api.v1.appuser;

import java.time.ZoneId;
import java.util.Locale;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RestController;

import es.nivel36.janus.api.Mapper;
import es.nivel36.janus.service.TimeFormat;
import es.nivel36.janus.service.appuser.AppUser;
import es.nivel36.janus.util.EmailAddresses;
import es.nivel36.janus.service.appuser.AppUserService;

/**
 * REST controller exposing CRUD operations for {@link AppUser} entities.
 */
@RestController
public class AppUserController implements AppUserResource {

	private static final Logger logger = LoggerFactory.getLogger(AppUserController.class);

	private final AppUserService appUserService;
	private final Mapper<AppUser, AppUserResponse> appUserResponseMapper;

	/**
	 * Creates a controller that exposes application user management endpoints.
	 *
	 * @param appUserService        service handling {@link AppUser} domain
	 *                              operations; must not be {@code null}
	 * @param appUserResponseMapper mapper translating {@link AppUser} entities to
	 *                              {@link AppUserResponse} DTOs; must not be
	 *                              {@code null}
	 */
	public AppUserController( //
			final AppUserService appUserService, //
			final @Qualifier("appUserResponseMapper") Mapper<AppUser, AppUserResponse> appUserResponseMapper) {
		this.appUserService = Objects.requireNonNull( //
				appUserService, //
				"appUserService can't be null");
		this.appUserResponseMapper = Objects.requireNonNull( //
				appUserResponseMapper, //
				"appUserResponseMapper can't be null");
	}

	/**
	 * Retrieves the current authenticated {@link AppUser}, creating it from JWT
	 * claims when provisioning is allowed.
	 *
	 * @param authentication the JWT authentication containing the current user's
	 *                       identity claims; must not be {@code null}
	 * @return the current {@link AppUserResponse}
	 */
	@Override
	public ResponseEntity<AppUserResponse> findCurrentAppUser(final JwtAuthenticationToken authentication) {
		final Jwt token = authentication.getToken();
		final Object preferredUsernameClaim = token.getClaims().get("preferred_username");
		final String preferredUsername = preferredUsernameClaim instanceof final String value ? value : null;
		final Boolean emailVerified = token.getClaim("email_verified");
		final boolean isEmailVerified = Boolean.TRUE.equals(emailVerified);
		final String verifiedEmail;
		final String email = token.getClaimAsString("email");
		if (isEmailVerified && StringUtils.hasText(email)) {
			verifiedEmail = EmailAddresses.canonicalize(email);
		} else {
			verifiedEmail = null;
		}
		final String subject = token.getSubject();
		final AppUser appUser = this.appUserService.findOrCreateAppUser(subject, preferredUsername, verifiedEmail);
		final AppUserResponse appUserResponse = this.appUserResponseMapper.map(appUser);
		return ResponseEntity.ok(appUserResponse);
	}

	/**
	 * Updates the preferences of the current authenticated {@link AppUser}.
	 *
	 * @param request        the payload containing the new user preferences; must
	 *                       not be {@code null}
	 * @param authentication the current authentication used to identify the user;
	 *                       must not be {@code null}
	 * @return the updated {@link AppUserResponse}
	 */
	@Override
	public ResponseEntity<AppUserResponse> updateCurrentAppUser( //
			final UpdateAppUserRequest request, //
			final Authentication authentication) {
		final String name = authentication.getName().trim();
		final Locale forLanguageTag = Locale.forLanguageTag(request.locale().trim());
		final TimeFormat timeFormat = request.timeFormat();
		final ZoneId zoneId = ZoneId.of(request.defaultTimezone().trim());
		final AppUser updated = this.appUserService.updateCurrentAppUser(name, forLanguageTag, timeFormat, zoneId);
		final AppUserResponse appUserResponse = this.appUserResponseMapper.map(updated);
		return ResponseEntity.ok(appUserResponse);
	}

	/**
	 * Deletes an existing {@link AppUser}.
	 *
	 * @param username the username of the app user; must not be {@code null}
	 * @return an empty response with status {@link HttpStatus#NO_CONTENT}
	 */
	@Override
	public ResponseEntity<Void> deleteAppUser(final String username) {
		logger.debug("Delete app user ACTION performed");

		final AppUser appUser = this.appUserService.findAppUserByUsername(username);
		this.appUserService.deleteAppUser(appUser);
		return ResponseEntity.noContent().build();
	}

	/**
	 * Replaces the Keycloak subject associated with an application user.
	 *
	 * @param username the username of the app user; must not be {@code null}
	 * @param request  payload containing the replacement Keycloak subject; must not
	 *                 be {@code null}
	 * @return the updated {@link AppUserResponse}
	 */
	@Override
	public ResponseEntity<AppUserResponse> replaceKeycloakSubject( //
			final String username, //
			final ReplaceKeycloakSubjectRequest request) {
		final AppUser updated = this.appUserService.replaceKeycloakSubject(username, request.keycloakSubject());
		return ResponseEntity.ok(this.appUserResponseMapper.map(updated));
	}
}
