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
package es.nivel36.janus.api.v1.appuser;

import java.util.Locale;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.nivel36.janus.api.Mapper;
import es.nivel36.janus.service.appuser.AppUser;
import es.nivel36.janus.service.appuser.AppUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;

/**
 * REST controller exposing CRUD operations for {@link AppUser} entities.
 */
@RestController
@RequestMapping("/api/v1/appusers")
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
			final Mapper<AppUser, AppUserResponse> appUserResponseMapper) {
		this.appUserService = Objects.requireNonNull(appUserService, "appUserService can't be null");
		this.appUserResponseMapper = Objects.requireNonNull(appUserResponseMapper,
				"appUserResponseMapper can't be null");
	}

	/**
	 * Retrieves an {@link AppUser} by its username.
	 *
	 * @param username the unique username of the user; must not be {@code null}
	 * @return the {@link AppUserResponse} matching the username
	 */
	@GetMapping("/{username}")
	public ResponseEntity<AppUserResponse> findAppUser(final @PathVariable("username") //
	@Pattern(regexp = "[A-Za-z0-9_.@-]{3,50}", //
			message = "username must contain only letters, digits, dots, underscores, hyphens or at signs (3-50 characters)") //
	String username) {
		logger.debug("Find app user ACTION performed");

		final AppUser appUser = this.appUserService.findAppUserByUsername(username);
		final AppUserResponse response = this.appUserResponseMapper.map(appUser);
		return ResponseEntity.ok(response);
	}

	/**
	 * Creates a new {@link AppUser} using the provided payload.
	 *
	 * @param request the data describing the app user to create; must not be
	 *                {@code null}
	 * @return the created {@link AppUserResponse}
	 */
	@PostMapping
	public ResponseEntity<AppUserResponse> createAppUser(@Valid @RequestBody final CreateAppUserRequest request) {
		logger.debug("Create app user ACTION performed");

		final Locale locale = Locale.forLanguageTag(request.locale());
		final AppUser createdAppUser = this.appUserService.createAppUser(request.username(), request.name(),
				request.surname(), locale, request.timeFormat());
		final AppUserResponse response = this.appUserResponseMapper.map(createdAppUser);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	/**
	 * Updates an existing {@link AppUser} identified by its username.
	 *
	 * @param username the username of the app user to update; must not be
	 *                 {@code null}
	 * @param request  the payload containing the new data; must not be {@code null}
	 * @return the updated {@link AppUserResponse}
	 */
	@PutMapping("/{username}")
	public ResponseEntity<AppUserResponse> updateAppUser(final @PathVariable("username") //
	@Pattern(regexp = "[A-Za-z0-9_.@-]{3,50}", //
			message = "username must contain only letters, digits, dots, underscores, hyphens or at signs (3-50 characters)") //
	String username, @Valid @RequestBody final UpdateAppUserRequest request) {
		logger.debug("Update app user ACTION performed");

		final Locale locale = Locale.forLanguageTag(request.locale());
		final AppUser updatedAppUser = this.appUserService.updateAppUser(username, request.name(), request.surname(),
				locale, request.timeFormat());
		final AppUserResponse response = this.appUserResponseMapper.map(updatedAppUser);
		return ResponseEntity.ok(response);
	}

	/**
	 * Deletes an existing {@link AppUser}.
	 *
	 * @param username the username of the app user; must not be {@code null}
	 * @return an empty response with status {@link HttpStatus#NO_CONTENT}
	 */
	@DeleteMapping("/{username}")
	public ResponseEntity<Void> deleteAppUser(final @PathVariable("username") //
	@Pattern(regexp = "[A-Za-z0-9_.@-]{3,50}", //
			message = "username must contain only letters, digits, dots, underscores, hyphens or at signs (3-50 characters)") //
	String username) {
		logger.debug("Delete app user ACTION performed");

		final AppUser appUser = this.appUserService.findAppUserByUsername(username);
		this.appUserService.deleteAppUser(appUser);
		return ResponseEntity.noContent().build();
	}
}
