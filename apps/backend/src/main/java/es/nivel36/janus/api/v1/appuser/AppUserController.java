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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.nivel36.janus.api.Mapper;
import es.nivel36.janus.service.TimeFormat;
import es.nivel36.janus.service.appuser.AppUser;
import es.nivel36.janus.service.appuser.AppUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;

/**
 * REST controller exposing CRUD operations for {@link AppUser} entities.
 */
@RestController
@RequestMapping({ "/api/v1/appusers", "/api/v1/app-users" })
public class AppUserController {

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
	public AppUserController(final AppUserService appUserService,
			final @Qualifier("appUserResponseMapper") Mapper<AppUser, AppUserResponse> appUserResponseMapper) {
		this.appUserService = Objects.requireNonNull(appUserService, "appUserService can't be null");
		this.appUserResponseMapper = Objects.requireNonNull(appUserResponseMapper,
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
	@PreAuthorize("@appUserProvisioningPolicy.canProvision(authentication)")
	@GetMapping("/me")
	public ResponseEntity<AppUserResponse> findCurrentAppUser(final JwtAuthenticationToken authentication) {
		final Object preferredUsernameClaim = authentication.getToken().getClaims().get("preferred_username");
		final String preferredUsername = preferredUsernameClaim instanceof final String value ? value : null;
		final Boolean emailVerified = authentication.getToken().getClaim("email_verified");
		final String email = authentication.getToken().getClaimAsString("email");
		final String verifiedEmail = Boolean.TRUE.equals(emailVerified) && StringUtils.hasText(email) ? email : null;
		return ResponseEntity.ok(this.appUserResponseMapper.map(this.appUserService
				.findOrCreateAppUser(authentication.getToken().getSubject(), preferredUsername, verifiedEmail)));
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
	@PreAuthorize("@appUserAuthorization.canUpdateCurrent(authentication)")
	@PutMapping("/me")
	public ResponseEntity<AppUserResponse> updateCurrentAppUser(@Valid @RequestBody final UpdateAppUserRequest request,
			final Authentication authentication) {
		final String name = authentication.getName().trim();
		final Locale forLanguageTag = Locale.forLanguageTag(request.locale().trim());
		final TimeFormat timeFormat = request.timeFormat();
		final ZoneId of = ZoneId.of(request.defaultTimezone().trim());
		final AppUser updated = this.appUserService.updateCurrentAppUser(name, forLanguageTag, timeFormat, of);
		return ResponseEntity.ok(this.appUserResponseMapper.map(updated));
	}

	/**
	 * Deletes an existing {@link AppUser}.
	 *
	 * @param username the username of the app user; must not be {@code null}
	 * @return an empty response with status {@link HttpStatus#NO_CONTENT}
	 */
	@PreAuthorize("@appUserAuthorization.canDelete(authentication)")
	@DeleteMapping("/{username}")
	public ResponseEntity<Void> deleteAppUser(final @PathVariable("username") //
	@Pattern(regexp = AppUser.USERNAME_PATTERN, message = AppUser.USERNAME_VALIDATION_MESSAGE) //
	String username) {
		logger.debug("Delete app user ACTION performed");

		final AppUser appUser = this.appUserService.findAppUserByUsername(username);
		this.appUserService.deleteAppUser(appUser);
		return ResponseEntity.noContent().build();
	}
}
