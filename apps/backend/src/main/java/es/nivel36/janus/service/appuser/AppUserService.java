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

import java.time.ZoneId;
import java.util.Locale;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.nivel36.janus.config.UserProvisioningProperties;
import es.nivel36.janus.service.ResourceAlreadyExistsException;
import es.nivel36.janus.service.ResourceNotFoundException;
import es.nivel36.janus.service.TimeFormat;
import es.nivel36.janus.service.employee.Employee;
import es.nivel36.janus.service.employee.EmployeeService;
import es.nivel36.janus.util.EmailAddresses;
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
	private final AppUserCreator appUserCreator;
	private final UserProvisioningProperties provisioningDefaults;
	private final EmployeeService employeeService;

	/**
	 * Creates a new {@code AppUserService}.
	 *
	 * @param appUserRepository repository used to manage {@link AppUser} entities.
	 *                          Can't be {@code null}.
	 *
	 * @throws NullPointerException if {@code appUserRepository} is {@code null}
	 */
	public AppUserService(final AppUserRepository appUserRepository, final AppUserCreator appUserCreator,
			final UserProvisioningProperties provisioningDefaults, final EmployeeService employeeService) {
		this.appUserRepository = Objects.requireNonNull(appUserRepository, "AppUserRepository cannot be null.");
		this.appUserCreator = Objects.requireNonNull(appUserCreator, "AppUserCreator cannot be null.");
		this.provisioningDefaults = Objects.requireNonNull(provisioningDefaults,
				"UserProvisioningProperties cannot be null.");
		this.employeeService = Objects.requireNonNull(employeeService, "EmployeeService cannot be null.");
	}

	/**
	 * Finds the account linked to a Keycloak subject or provisions it on first
	 * access. The subject is the sole identity-linking key; the preferred username
	 * is used only as the new account's visible name.
	 *
	 * <p>The initial preferences come from the provisioning defaults. When an
	 * account must be created, {@code preferred_username} must satisfy the same
	 * rule as usernames accepted by the administration API.</p>
	 */
	@Transactional
	public AppUser findOrCreateAppUser(final String keycloakSubject, final String preferredUsername) {
		return findOrCreateAppUser(keycloakSubject, preferredUsername, null);
	}

	@Transactional
	public AppUser findOrCreateAppUser(final String keycloakSubject, final String preferredUsername,
			final String verifiedEmail) {
		Strings.requireNonBlank(keycloakSubject, "keycloakSubject cannot be null or blank.");

		final var existing = this.appUserRepository.findByKeycloakSubject(keycloakSubject.trim());
		if (existing.isPresent()) {
			return existing.get();
		}

		final String username = validatePreferredUsername(preferredUsername);
		final Employee employee = findUnlinkedEmployee(verifiedEmail, keycloakSubject.trim());
		try {
			return this.appUserCreator.create(username, keycloakSubject.trim(), this.provisioningDefaults.locale(),
					this.provisioningDefaults.getTimeFormat(), this.provisioningDefaults.defaultTimezone(), employee);
		} catch (final DataIntegrityViolationException raceOrDuplicate) {
			// Another request may have committed the same subject while this request was
			// provisioning it. The failed insert ran in REQUIRES_NEW, so this transaction
			// remains usable and can read the winning row.
			final var concurrentlyCreated = this.appUserRepository.findByKeycloakSubject(keycloakSubject.trim());
			if (concurrentlyCreated.isPresent()) {
				return concurrentlyCreated.get();
			}
			if (employee != null && this.appUserRepository.existsByEmployee(employee)) {
				logEmployeeConflict(employee, keycloakSubject.trim());
				return createWithoutEmployeeAfterConflict(username, keycloakSubject.trim());
			}
			throw new ResourceAlreadyExistsException("Application user with username " + username + " already exists");
		}
	}

	private AppUser createWithoutEmployeeAfterConflict(final String username, final String keycloakSubject) {
		try {
			return this.appUserCreator.create(username, keycloakSubject, this.provisioningDefaults.locale(),
					this.provisioningDefaults.getTimeFormat(), this.provisioningDefaults.defaultTimezone(), null);
		} catch (final DataIntegrityViolationException raceOrDuplicate) {
			return this.appUserRepository.findByKeycloakSubject(keycloakSubject)
					.orElseThrow(() -> new ResourceAlreadyExistsException(
							"Application user with username " + username + " already exists"));
		}
	}

	private Employee findUnlinkedEmployee(final String verifiedEmail, final String keycloakSubject) {
		if (verifiedEmail == null) {
			return null;
		}
		return this.employeeService.findEmployeeForProvisioning(EmailAddresses.canonicalize(verifiedEmail))
				.filter(employee -> {
					final var linkedUser = this.appUserRepository.findByEmployee(employee);
					if (linkedUser.isPresent()) {
						logEmployeeConflict(employee, keycloakSubject);
						return false;
					}
					return true;
				})
				.orElse(null);
	}

	private void logEmployeeConflict(final Employee employee, final String keycloakSubject) {
		logger.warn("Employee identity link conflict for employeeId={} and keycloakSubject={}; keeping existing link",
				employee.getId(), keycloakSubject);
	}

	private static String validatePreferredUsername(final String preferredUsername) {
		if (preferredUsername == null) {
			throw new IllegalArgumentException("preferred_username claim is required");
		}
		final String username = preferredUsername.trim();
		if (!username.matches(AppUser.USERNAME_PATTERN)) {
			throw new IllegalArgumentException("preferred_username claim is invalid: "
					+ AppUser.USERNAME_VALIDATION_MESSAGE);
		}
		return username;
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
	 * @param username        the unique username of the user. Can't be {@code null}
	 *                        or blank.
	 * @param locale          the preferred {@link Locale} of the user. Can't be
	 *                        {@code null}.
	 * @param timeFormat      the preferred {@link TimeFormat} of the user. Can't be
	 *                        {@code null}.
	 * @param defaultTimezone the preferred default timezone of the user. Can't be
	 *                        {@code null} or blank.
	 *
	 * @return the newly created {@link AppUser}
	 *
	 * @throws NullPointerException           if any parameter is {@code null}
	 * @throws IllegalArgumentException       if any string parameter is blank or if
	 *                                        {@code defaultTimezone} is invalid
	 * @throws ResourceAlreadyExistsException if a user with the given username
	 *                                        already exists
	 */
	@Transactional
	public AppUser createAppUser(final String username, final String keycloakSubject, final Locale locale,
			final TimeFormat timeFormat, final ZoneId defaultTimezone) {
		return createAppUser(username, keycloakSubject, locale, timeFormat, defaultTimezone, null);
	}

	@Transactional
	public AppUser createAppUser(final String username, final String keycloakSubject, final Locale locale,
			final TimeFormat timeFormat, final ZoneId defaultTimezone, final Employee employee) {

		Strings.requireNonBlank(username, "username cannot be null or blank.");
		Strings.requireNonBlank(keycloakSubject, "keycloakSubject cannot be null or blank.");
		Objects.requireNonNull(locale, "locale cannot be null.");
		Objects.requireNonNull(timeFormat, "timeFormat cannot be null.");
		Objects.requireNonNull(defaultTimezone, "defaultTimezone cannot be null.");

		logger.debug("Creating new application user {}", username);

		final boolean usernameInUse = this.appUserRepository.existsByUsername(username);
		if (usernameInUse) {
			throw new ResourceAlreadyExistsException("Application user with username " + username + " already exists");
		}
		if (this.appUserRepository.existsByKeycloakSubject(keycloakSubject)) {
			throw new ResourceAlreadyExistsException("Keycloak subject is already linked to an application user");
		}
		if (employee != null && this.appUserRepository.existsByEmployee(employee)) {
			throw new ResourceAlreadyExistsException("Employee is already linked to an application user");
		}

		final AppUser appUser = new AppUser(username.trim(), keycloakSubject.trim(), locale, timeFormat,
				defaultTimezone);
		appUser.setEmployee(employee);

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
	 * @param username           the unique username of the user to update. Can't be
	 *                           {@code null} or blank.
	 * @param newLocale          the new preferred {@link Locale}. Can't be
	 *                           {@code null}.
	 * @param newTimeFormat      the new preferred {@link TimeFormat}. Can't be
	 *                           {@code null}.
	 * @param newDefaultTimezone the new preferred default timezone. Can't be
	 *                           {@code null} or blank.
	 *
	 * @return the updated {@link AppUser}
	 *
	 * @throws NullPointerException      if any parameter is {@code null}
	 * @throws IllegalArgumentException  if any string parameter is blank or if
	 *                                   {@code newDefaultTimezone} is invalid
	 * @throws ResourceNotFoundException if no user exists with the given username
	 */
	@Transactional
	public AppUser updateAppUser(final String username, final Locale newLocale, final TimeFormat newTimeFormat,
			final ZoneId newDefaultTimezone) {
		return updateAppUser(username, newLocale, newTimeFormat, newDefaultTimezone, null, false);
	}

	@Transactional
	public AppUser updateAppUser(final String username, final Locale newLocale, final TimeFormat newTimeFormat,
			final ZoneId newDefaultTimezone, final Employee employee, final boolean updateEmployee) {
		Strings.requireNonBlank(username, "username cannot be null or blank.");
		Objects.requireNonNull(newLocale, "newLocale cannot be null.");
		Objects.requireNonNull(newTimeFormat, "newTimeFormat cannot be null.");
		Objects.requireNonNull(newDefaultTimezone, "newDefaultTimezone cannot be null.");
		logger.debug("Updating AppUser {}", username);

		final AppUser appUser = this.findAppUser(username);
		appUser.setLocale(newLocale);
		appUser.setTimeFormat(newTimeFormat);
		appUser.setDefaultTimezone(newDefaultTimezone);
		if (updateEmployee && !samePersistentEmployee(employee, appUser.getEmployee())) {
			if (employee != null && this.appUserRepository.existsByEmployee(employee)) {
				throw new ResourceAlreadyExistsException("Employee is already linked to an application user");
			}
			appUser.setEmployee(employee);
		}
		return appUser;
	}

	private static boolean samePersistentEmployee(final Employee first, final Employee second) {
		if (first == second) {
			return true;
		}
		return first != null && second != null && first.getId() != null
				&& Objects.equals(first.getId(), second.getId());
	}

	@Transactional(readOnly = true)
	public AppUser findAppUserByKeycloakSubject(final String keycloakSubject) {
		Strings.requireNonBlank(keycloakSubject, "keycloakSubject cannot be null or blank.");
		return this.appUserRepository.findByKeycloakSubject(keycloakSubject)
				.orElseThrow(() -> new org.springframework.security.access.AccessDeniedException(
						"The authenticated identity has not been provisioned"));
	}

	@Transactional
	public AppUser updateCurrentAppUser(final String keycloakSubject, final Locale newLocale, final TimeFormat newTimeFormat,
			final ZoneId newDefaultTimezone) {
		final AppUser appUser = findAppUserByKeycloakSubject(keycloakSubject);
		appUser.setLocale(newLocale);
		appUser.setTimeFormat(newTimeFormat);
		appUser.setDefaultTimezone(newDefaultTimezone);
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
