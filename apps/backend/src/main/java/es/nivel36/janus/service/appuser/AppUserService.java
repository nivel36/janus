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
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.nivel36.janus.config.UserProvisioningProperties;
import es.nivel36.janus.service.ResourceNotFoundException;
import es.nivel36.janus.service.TimeFormat;
import es.nivel36.janus.service.employee.Employee;
import es.nivel36.janus.service.employee.EmployeeService;
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
	 * <p>
	 * The initial preferences come from the provisioning defaults. When an account
	 * must be created, {@code preferred_username} must satisfy the same rule as
	 * usernames accepted by the administration API.
	 * </p>
	 */
	@Transactional
	public AppUser findOrCreateAppUser(final String keycloakSubject, final String preferredUsername,
			final String verifiedEmail) {
		Strings.requireNonBlank(keycloakSubject, "keycloakSubject cannot be null or blank.");

		final var existing = this.appUserRepository.findByKeycloakSubject(keycloakSubject);
		if (existing.isPresent()) {
			return existing.get();
		}

		final String username = validatePreferredUsername(preferredUsername);
		final Employee employee = this.findUnlinkedEmployee(verifiedEmail, keycloakSubject);
		return this.insertAndReconcile(username, keycloakSubject, employee);
	}

	private AppUser insertAndReconcile(final String username, final String keycloakSubject, final Employee employee) {
		try {
			return this.appUserCreator.create(username, keycloakSubject, this.provisioningDefaults.locale(),
					this.provisioningDefaults.getTimeFormat(), this.provisioningDefaults.defaultTimezone(), employee);
		} catch (final AppUserCreationConflict conflict) {
			// Subject reconciliation always comes first: it makes repeated requests
			// idempotent even if a driver did not expose the violated constraint name.
			final Optional<AppUser> subjectWinner = this.appUserRepository.findByKeycloakSubject(keycloakSubject);
			if (subjectWinner.isPresent()) {
				return requireRequestedSubject(subjectWinner.get(), keycloakSubject);
			}
			if (employee != null && (conflict.key() == AppUserCreationConflict.Key.EMPLOYEE
					|| conflict.key() == AppUserCreationConflict.Key.UNKNOWN)
					&& this.appUserRepository.existsByEmployee(employee)) {
				this.logEmployeeConflict(employee, keycloakSubject);
				return this.insertAndReconcile(username, keycloakSubject, null);
			}
			if (conflict.key() == AppUserCreationConflict.Key.USERNAME
					|| this.appUserRepository.existsByUsername(username)) {
				throw usernameConflict(username, conflict);
			}
			throw conflict;
		}
	}

	private static AppUser requireRequestedSubject(final AppUser appUser, final String keycloakSubject) {
		if (!keycloakSubject.equals(appUser.getKeycloakSubject())) {
			throw new IllegalStateException("Subject lookup returned a profile for a different identity");
		}
		return appUser;
	}

	private static PreferredUsernameConflictException usernameConflict(final String username, final Throwable cause) {
		return new PreferredUsernameConflictException(username, cause);
	}

	/** Replaces the subject after an administrator verifies the new identity. */
	@Transactional
	public AppUser replaceKeycloakSubject(final String username, final String newKeycloakSubject) {
		Strings.requireNonBlank(newKeycloakSubject, "newKeycloakSubject cannot be null or blank.");
		final AppUser appUser = this.findAppUserByUsername(username);
		this.appUserRepository.findByKeycloakSubject(newKeycloakSubject).filter(other -> other != appUser)
				.ifPresent(other -> {
					throw new KeycloakSubjectConflictException(newKeycloakSubject);
				});
		appUser.replaceKeycloakSubject(newKeycloakSubject);
		return appUser;
	}

	private Employee findUnlinkedEmployee(final String verifiedEmail, final String keycloakSubject) {
		if (verifiedEmail == null) {
			return null;
		}
		return this.employeeService.findEmployeeForProvisioning(verifiedEmail)
				.filter(employee -> {
					final var linkedUser = this.appUserRepository.findByEmployee(employee);
					if (linkedUser.isPresent()) {
						this.logEmployeeConflict(employee, keycloakSubject);
						return false;
					}
					return true;
				}).orElse(null);
	}

	private void logEmployeeConflict(final Employee employee, final String keycloakSubject) {
		logger.warn("Employee identity link conflict for employeeId={} and keycloakSubject={}; keeping existing link",
				employee.getId(), keycloakSubject);
	}

	private static String validatePreferredUsername(final String preferredUsername) {
		if (preferredUsername == null) {
			throw new IllegalArgumentException("preferred_username claim is required");
		}
		final String username = preferredUsername;
		if (!username.matches("[A-Za-z0-9_.@-]{3,50}")) {
			throw new IllegalArgumentException(
					"preferred_username claim is invalid: " + "username must contain only letters, digits, dots, underscores, hyphens or at signs (3-50 characters)");
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

	@Transactional(readOnly = true)
	public AppUser findAppUserByKeycloakSubject(final String keycloakSubject) {
		Strings.requireNonBlank(keycloakSubject, "keycloakSubject cannot be null or blank.");
		return this.appUserRepository.findByKeycloakSubject(keycloakSubject)
				.orElseThrow(() -> new org.springframework.security.access.AccessDeniedException(
						"The authenticated identity has not been provisioned"));
	}

	@Transactional
	public AppUser updateCurrentAppUser(final String keycloakSubject, final Locale newLocale,
			final TimeFormat newTimeFormat, final ZoneId newDefaultTimezone) {
		final AppUser appUser = this.findAppUserByKeycloakSubject(keycloakSubject);
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
