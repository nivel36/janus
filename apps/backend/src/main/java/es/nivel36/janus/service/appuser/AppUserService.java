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
package es.nivel36.janus.service.appuser;

import java.util.Locale;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.nivel36.janus.service.ResourceAlreadyExistsException;
import es.nivel36.janus.service.ResourceNotFoundException;
import es.nivel36.janus.service.TimeFormat;
import es.nivel36.janus.util.Strings;

/**
 * Service responsible for managing {@link AppUser} entities.
 *
 * <p>
 * This service acts as the application-layer entry point for operations related
 * to {@link AppUser} lifecycle management, such as creation, retrieval, update,
 * and deletion.
 * </p>
 *
 * <p>
 * It encapsulates validation rules and delegates persistence operations to the
 * underlying {@link AppUserRepository}.
 * </p>
 */
@Service
public class AppUserService {

	private static final Logger logger = LoggerFactory.getLogger(AppUserService.class);

	/**
	 * Repository used to access {@link AppUser} persistence operations.
	 */
	private final AppUserRepository appUserRepository;

	/**
	 * Creates a new {@code AppUserService}.
	 *
	 * @param appUserRepository repository used to manage {@link AppUser} entities.
	 *                          Can't be {@code null}.
	 *
	 * @throws NullPointerException if {@code appUserRepository} is {@code null}
	 */
	public AppUserService(final AppUserRepository appUserRepository) {
		this.appUserRepository = Objects.requireNonNull(appUserRepository, "AppUserRepository cannot be null.");
	}

	/**
	 * Retrieves an {@link AppUser} identified by the given username.
	 *
	 * @param username the unique username of the user to retrieve. Can't be
	 *                 {@code null} or blank.
	 *
	 * @return the {@link AppUser} associated with the given username
	 *
	 * @throws NullPointerException      if {@code username} is {@code null}
	 * @throws IllegalArgumentException  if {@code username} is blank
	 * @throws ResourceNotFoundException if no user exists with the given username
	 */
	@Transactional(readOnly = true)
	public AppUser findAppUserByUsername(final String username) {
		Strings.requireNonBlank(username, "username cannot be null or blank.");
		logger.debug("Finding AppUser by username {}", username);

		return this.findAppUser(username);
	}

	/**
	 * Creates and persists a new {@link AppUser}.
	 *
	 * <p>
	 * The username must be unique. If a user with the same username already exists,
	 * the operation will fail.
	 * </p>
	 *
	 * @param username   the unique username of the user. Can't be {@code null} or
	 *                   blank.
	 * @param locale     the preferred {@link Locale} of the user. Can't be
	 *                   {@code null}.
	 * @param timeFormat the preferred {@link TimeFormat} of the user. Can't be
	 *                   {@code null}.
	 *
	 * @return the newly created {@link AppUser}
	 *
	 * @throws NullPointerException           if any parameter is {@code null}
	 * @throws IllegalArgumentException       if any string parameter is blank
	 * @throws ResourceAlreadyExistsException if a user with the given username
	 *                                        already exists
	 */
	@Transactional
	public AppUser createAppUser(final String username, final Locale locale, final TimeFormat timeFormat) {

		Strings.requireNonBlank(username, "username cannot be null or blank.");
		Objects.requireNonNull(locale, "locale cannot be null.");
		Objects.requireNonNull(timeFormat, "timeFormat cannot be null.");

		logger.debug("Creating new application user {}", username);

		final boolean usernameInUse = this.appUserRepository.existsByUsername(username);
		if (usernameInUse) {
			throw new ResourceAlreadyExistsException("Application user with username " + username + " already exists");
		}

		final AppUser appUser = new AppUser(username.trim(), locale, timeFormat);

		final AppUser savedAppUser = this.appUserRepository.save(appUser);
		logger.trace("Application user {} created successfully", savedAppUser);

		return savedAppUser;
	}

	/**
	 * Updates an existing {@link AppUser}.
	 *
	 * <p>
	 * Replaces the user's personal data and preferences atomically. The username is
	 * used as the immutable identifier of the user.
	 * </p>
	 *
	 * @param username      the unique username of the user to update. Can't be
	 *                      {@code null} or blank.
	 * @param newLocale     the new preferred {@link Locale}. Can't be {@code null}.
	 * @param newTimeFormat the new preferred {@link TimeFormat}. Can't be
	 *                      {@code null}.
	 *
	 * @return the updated {@link AppUser}
	 *
	 * @throws NullPointerException      if any parameter is {@code null}
	 * @throws IllegalArgumentException  if any string parameter is blank
	 * @throws ResourceNotFoundException if no user exists with the given username
	 */
	@Transactional
	public AppUser updateAppUser(final String username, final Locale newLocale, final TimeFormat newTimeFormat) {
		Strings.requireNonBlank(username, "username cannot be null or blank.");
		Objects.requireNonNull(newLocale, "newLocale cannot be null.");
		Objects.requireNonNull(newTimeFormat, "newTimeFormat cannot be null.");
		logger.debug("Updating AppUser {}", username);

		final AppUser appUser = this.findAppUser(username);
		appUser.setLocale(newLocale);
		appUser.setTimeFormat(newTimeFormat);
		return appUser;
	}

	/**
	 * Deletes the given {@link AppUser}.
	 *
	 * @param appUser the user to delete. Can't be {@code null}.
	 *
	 * @throws NullPointerException if {@code appUser} is {@code null}
	 */
	@Transactional
	public void deleteAppUser(final AppUser appUser) {
		Objects.requireNonNull(appUser, "appUser cannot be null.");
		logger.debug("Deleting AppUser {}", appUser);

		this.appUserRepository.delete(appUser);
		logger.trace("AppUser {} deleted successfully", appUser);
	}

	private AppUser findAppUser(final String username) {
		final AppUser appUser = this.appUserRepository.findByUsername(username);
		if (appUser == null) {
			throw new ResourceNotFoundException("There is no application user with username " + username);
		}
		return appUser;
	}
}
