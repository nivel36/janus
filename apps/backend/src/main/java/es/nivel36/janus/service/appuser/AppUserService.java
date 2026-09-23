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
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

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
	public AppUserService( //
			final AppUserRepository appUserRepository, //
			final AppUserCreator appUserCreator, //
			final UserProvisioningProperties provisioningDefaults, //
			final EmployeeService employeeService) {
		this.appUserRepository = Objects.requireNonNull( //
				appUserRepository, //
				"AppUserRepository cannot be null.");
		this.appUserCreator = Objects.requireNonNull( //
				appUserCreator, //
				"AppUserCreator cannot be null.");
		this.provisioningDefaults = Objects.requireNonNull( //
				provisioningDefaults, //
				"UserProvisioningProperties cannot be null.");
		this.employeeService = Objects.requireNonNull( //
				employeeService, //
				"EmployeeService cannot be null.");
	}

	/**
	 * Finds the account linked to a Keycloak subject or provisions it on first
	 * access. The subject is the sole identity-linking key and the verified email
	 * is stored as contact information without being used as a unique identifier.
	 */
	@Transactional
	public AppUser findOrCreateAppUser( //
			final String keycloakSubject, //
			final String email) {
		Strings.requireNonBlank(keycloakSubject, "keycloakSubject cannot be null or blank.");
		Strings.requireNonBlank(email, "email cannot be null or blank.");
		final Optional<AppUser> existing = this.appUserRepository.findByKeycloakSubject(keycloakSubject);
		if (existing.isPresent()) {
			return existing.get();
		}

		final Employee employee = this.findUnlinkedEmployee(email, keycloakSubject);
		return this.insertAndReconcile(email, keycloakSubject, employee);
	}

	private AppUser insertAndReconcile(final String email, final String keycloakSubject, final Employee employee) {
		try {
			return this.appUserCreator.create(email, keycloakSubject, this.provisioningDefaults.locale(),
					this.provisioningDefaults.getTimeFormat(), this.provisioningDefaults.defaultTimezone(), employee);
		} catch (final AppUserCreationConflict conflict) {
			final Optional<AppUser> subjectWinner = this.appUserRepository.findByKeycloakSubject(keycloakSubject);
			if (subjectWinner.isPresent()) {
				return requireRequestedSubject(subjectWinner.get(), keycloakSubject);
			}
			if (employee != null) {
				this.logEmployeeConflict(employee, keycloakSubject);
				return this.insertAndReconcile(email, keycloakSubject, null);
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

	private Employee findUnlinkedEmployee(final String email, final String keycloakSubject) {
		return this.employeeService.findEmployeeForProvisioning(email).filter(employee -> {
			final Optional<AppUser> linkedUser = this.appUserRepository.findByEmployee(employee);
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

	@Transactional(readOnly = true)
	public AppUser findAppUserById(final UUID id) {
		Objects.requireNonNull(id, "id cannot be null.");
		return this.appUserRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("There is no application user with id " + id));
	}

	@Transactional(readOnly = true)
	public List<AppUser> findAppUsersByEmail(final String email) {
		Strings.requireNonBlank(email, "email cannot be null or blank.");
		return this.appUserRepository.findByEmail(email);
	}

	@Transactional(readOnly = true)
	public AppUser findAppUserByKeycloakSubject(final String keycloakSubject) {
		Strings.requireNonBlank(keycloakSubject, "keycloakSubject cannot be null or blank.");
		return this.appUserRepository.findByKeycloakSubject(keycloakSubject)
				.orElseThrow(() -> new org.springframework.security.access.AccessDeniedException(
						"The authenticated identity has not been provisioned"));
	}

	@Transactional
	public AppUser updateAppUser( //
			final UUID id, //
			final Locale newLocale, //
			final TimeFormat newTimeFormat, //
			final ZoneId newDefaultTimezone) {
		final AppUser appUser = this.findAppUserById(id);
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

}
