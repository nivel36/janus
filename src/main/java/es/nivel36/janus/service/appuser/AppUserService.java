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
package es.nivel36.janus.service.appuser;

import java.util.Locale;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import es.nivel36.janus.service.ResourceAlreadyExistsException;
import es.nivel36.janus.service.ResourceNotFoundException;
import es.nivel36.janus.service.TimeFormat;

/**
 * Service class responsible for managing {@link AppUser} entities and
 * interacting with the {@link AppUserRepository}.
 */
@Service
public class AppUserService {

	private static final Logger logger = LoggerFactory.getLogger(AppUserService.class);

	private final AppUserRepository appUserRepository;

	public AppUserService(final AppUserRepository appUserRepository) {
		this.appUserRepository = Objects.requireNonNull(appUserRepository, "AppUserRepository cannot be null.");
	}

	/**
	 * Finds an {@link AppUser} by username.
	 *
	 * @param username the username of the user to find
	 * @return the application user with the specified username
	 * @throws NullPointerException      if the username is null
	 * @throws ResourceNotFoundException if the user is not found
	 */
        public AppUser findAppUserByUsername(final String username) {
                Objects.requireNonNull(username, "username cannot be null.");
                logger.debug("Finding AppUser by username {}", username);

                return this.findAppUser(username);
        }

        private AppUser findAppUser(final String username) {
                final AppUser appUser = this.appUserRepository.findByUsername(username);
		if (appUser == null) {
			logger.warn("No application user found with username {}", username);
			throw new ResourceNotFoundException("There is no application user with username " + username);
		}
		return appUser;
	}

	/**
	 * Creates a new {@link AppUser}.
	 *
	 * @param username   unique username
	 * @param name       first name
	 * @param surname    last name
	 * @param locale     preferred locale
	 * @param timeFormat preferred time format
	 * @return the created AppUser
	 * @throws NullPointerException           if any argument is null
	 * @throws ResourceAlreadyExistsException if the username already exists
	 */
	public AppUser createAppUser(final String username, final String name, final String surname, final Locale locale,
			final TimeFormat timeFormat) {

		Objects.requireNonNull(username, "username cannot be null.");
		Objects.requireNonNull(name, "name cannot be null.");
		Objects.requireNonNull(surname, "surname cannot be null.");
		Objects.requireNonNull(locale, "locale cannot be null.");
		Objects.requireNonNull(timeFormat, "timeFormat cannot be null.");

		logger.debug("Creating new application user {}", username);

		final boolean usernameInUse = this.appUserRepository.existsByUsername(username);
		if (usernameInUse) {
			logger.warn("Application user with username {} already exists", username);
			throw new ResourceAlreadyExistsException("Application user with username " + username + " already exists");
		}

		final AppUser appUser = new AppUser(username.trim(), name.trim(), surname.trim(), locale, timeFormat);
		final AppUser savedAppUser = this.appUserRepository.save(appUser);
		logger.trace("Application user {} created successfully", savedAppUser);

		return savedAppUser;
	}

	/**
	 * Updates an existing {@link AppUser}.
	 *
	 * <p>
	 * Validates and normalizes inputs. Replaces name, surname, locale and time
	 * format atomically.
	 * </p>
	 *
	 * @param username      unique username of the user to update
	 * @param newName       new first name
	 * @param newSurname    new surname
	 * @param newLocale     new preferred locale
	 * @param newTimeFormat new preferred time format
	 * @return the updated AppUser
	 *
	 * @throws IllegalArgumentException  if any string parameter is blank
	 * @throws ResourceNotFoundException if no AppUser exists with the given
	 *                                   username
	 */
	public AppUser updateAppUser(final String username, final String newName, final String newSurname,
			final Locale newLocale, final TimeFormat newTimeFormat) {

		Objects.requireNonNull(username, "username cannot be null.");
		Objects.requireNonNull(newName, "newName cannot be null.");
		Objects.requireNonNull(newSurname, "newSurname cannot be null.");
		Objects.requireNonNull(newLocale, "newLocale cannot be null.");
		Objects.requireNonNull(newTimeFormat, "newTimeFormat cannot be null.");

		logger.debug("Updating AppUser {}", username);

		final AppUser appUser = this.findAppUser(username);

		if (newName.isBlank() || newSurname.isBlank()) {
			throw new IllegalArgumentException("Name and surname cannot be blank.");
		}

		appUser.setName(newName.trim());
		appUser.setSurname(newSurname.trim());
		appUser.setLocale(newLocale);
		appUser.setTimeFormat(newTimeFormat);

		final AppUser savedAppUser = this.appUserRepository.save(appUser);
		logger.trace("AppUser {} updated successfully", savedAppUser);

		return savedAppUser;
	}

	/**
	 * Deletes an existing {@link AppUser}.
	 *
	 * @param appUser the AppUser to be deleted
	 * @throws NullPointerException if the user is null
	 */
	public void deleteAppUser(final AppUser appUser) {
		Objects.requireNonNull(appUser, "appUser cannot be null.");
		logger.debug("Deleting AppUser {}", appUser);

		this.appUserRepository.delete(appUser);

		logger.trace("AppUser {} deleted successfully", appUser);
	}
}
