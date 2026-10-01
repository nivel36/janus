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

import java.net.URI;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.RestController;

import es.nivel36.janus.api.Mapper;
import es.nivel36.janus.api.validation.EmployeeNumber;
import es.nivel36.janus.service.TimeFormat;
import es.nivel36.janus.service.appuser.AppUser;
import es.nivel36.janus.service.appuser.AppUserService;
import es.nivel36.janus.service.appuser.Theme;
import es.nivel36.janus.util.EmailAddresses;

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
	 * Retrieves the current authenticated {@link AppUser} without modifying it.
	 *
	 * @param authentication the JWT authentication containing the current user's
	 *                       identity claims; must not be {@code null}
	 * @return the current {@link AppUserResponse}
	 */
	@Override
	public ResponseEntity<AppUserResponse> findCurrentAppUser(final JwtAuthenticationToken authentication) {
		final AppUser appUser = this.appUserService.findAppUserByKeycloakSubject(authentication.getToken().getSubject());
		return ResponseEntity.ok(this.appUserResponseMapper.map(appUser));
	}

	/**
	 * Provisions the current authenticated identity, or returns its existing profile.
	 *
	 * @param authentication the JWT authentication containing the identity claims
	 * @return {@code 201 Created} for a new profile, or {@code 200 OK} when the
	 *         profile already existed
	 */
	@Override
	public ResponseEntity<AppUserResponse> provisionCurrentAppUser(final JwtAuthenticationToken authentication) {
		final Jwt token = authentication.getToken();
		final String email = token.getClaimAsString("email");
		if (email == null || email.isBlank()) {
			throw new IllegalArgumentException("email claim is required");
		}
		final String subject = token.getSubject();
		final String employeeNumber = normalizeEmployeeNumber(token.getClaimAsString("employeeNumber"));
		final boolean existed = this.profileExists(subject);
		final AppUser appUser = this.appUserService.findOrCreateAppUser(subject, EmailAddresses.canonicalize(email),
				employeeNumber);
		final AppUserResponse appUserResponse = this.appUserResponseMapper.map(appUser);
		return existed ? ResponseEntity.ok(appUserResponse)
				: ResponseEntity.created(URI.create("/api/v1/app-users/me")).body(appUserResponse);
	}

	private boolean profileExists(final String subject) {
		try {
			this.appUserService.findAppUserByKeycloakSubject(subject);
			return true;
		} catch (final AccessDeniedException notProvisioned) {
			return false;
		}
	}

	private static String normalizeEmployeeNumber(final String claim) {
		if (claim == null) {
			return null;
		}
		final String normalized = claim.trim();
		if (!normalized.matches(EmployeeNumber.PATTERN)) {
			throw new IllegalArgumentException(
					"employeeNumber must contain only letters, digits, underscores or hyphens (1-50 characters)");
		}
		return normalized;
	}

	/**
	 * Updates the preferences of the current authenticated {@link AppUser}.
	 *
	 * @param request        the payload containing the new user preferences; must
	 *                       not be {@code null}
	 * @return the updated {@link AppUserResponse}
	 */
	@Override
	public ResponseEntity<AppUserResponse> updateAppUser( //
			final UUID id, //
			final UpdateAppUserRequest request) {
		final Locale forLanguageTag = Locale.forLanguageTag(request.locale().trim());
		final TimeFormat timeFormat = request.timeFormat();
		final ZoneId zoneId = ZoneId.of(request.defaultTimezone().trim());
		final Theme theme = request.theme();
		final AppUser updated = this.appUserService.updateAppUser(id, forLanguageTag, timeFormat, zoneId, theme);
		final AppUserResponse appUserResponse = this.appUserResponseMapper.map(updated);
		return ResponseEntity.ok(appUserResponse);
	}

	/**
	 * Deletes an existing {@link AppUser}.
	 *
	 * @param id the UUID of the app user; must not be {@code null}
	 * @return an empty response with status {@link HttpStatus#NO_CONTENT}
	 */
	@Override
	public ResponseEntity<Void> deleteAppUser(final UUID id) {
		logger.debug("Delete app user ACTION performed");

		final AppUser appUser = this.appUserService.findAppUserById(id);
		this.appUserService.deleteAppUser(appUser);
		return ResponseEntity.noContent().build();
	}

}
