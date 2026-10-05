/*
 * Copyright 2026 Abel Ferrer Jiménez
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package es.nivel36.janus.service.appuser;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import es.nivel36.janus.config.UserProvisioningProperties;
import es.nivel36.janus.service.ResourceNotFoundException;
import es.nivel36.janus.service.TimeFormat;
import es.nivel36.janus.service.employee.Employee;
import es.nivel36.janus.service.employee.EmployeeService;
import es.nivel36.janus.util.LikePatterns;
import es.nivel36.janus.validation.EmployeeNumber;
import es.nivel36.janus.validation.KeycloakSubject;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Transactional entry point for provisioning, lookup, search, preference
 * changes and deletion of local application profiles. Dependencies must be
 * nonnull and provisioning defaults validated before use. Method contracts
 * define input validation and database effects; authorization belongs to the
 * resource policies and is not performed by this service. Provisioning alone
 * uses an independent insert transaction to reconcile races. Parameter
 * constraints are enforced when invoked through Spring's validated proxy, which
 * rejects invalid arguments with {@link ConstraintViolationException}; direct
 * Java calls do not activate this validation. Lookup and search never provision
 * profiles or change associations.
 */
@Validated
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
	private final int maxPageSize;

	/**
	 * Creates a service with the dependencies needed for profile operations.
	 * Construction requires nonnull dependencies and a positive page-size limit,
	 * and performs no database access.
	 *
	 * @param  appUserRepository        nonnull profile repository
	 * @param  appUserCreator           nonnull independent-transaction creator
	 * @param  provisioningDefaults     nonnull, validated initial preferences
	 * @param  employeeService          nonnull employee lookup service
	 * @param  maxPageSize              positive limit from
	 *                                  spring.data.rest.max-page-size
	 * @throws NullPointerException     if any dependency is null
	 * @throws IllegalArgumentException if maxPageSize is not positive
	 */
	public AppUserService(
		final AppUserRepository appUserRepository,
		final AppUserCreator appUserCreator,
		final UserProvisioningProperties provisioningDefaults,
		final EmployeeService employeeService,
		final @Value("${spring.data.rest.max-page-size}") int maxPageSize) {
		this.appUserRepository = Objects.requireNonNull(appUserRepository, "AppUserRepository cannot be null.");
		this.appUserCreator = Objects.requireNonNull(appUserCreator, "AppUserCreator cannot be null.");
		this.provisioningDefaults = Objects
				.requireNonNull(provisioningDefaults, "UserProvisioningProperties cannot be null.");
		this.employeeService = Objects.requireNonNull(employeeService, "EmployeeService cannot be null.");
		if (maxPageSize < 1) {
			throw new IllegalArgumentException("maxPageSize must be positive");
		}
		this.maxPageSize = maxPageSize;
	}

	/**
	 * Finds a subject profile or provisions one on first access.
	 * <p>
	 * The caller must supply a trusted, verified email and subject and authorize
	 * provisioning. Subject must match {@code [A-Za-z0-9_-]{1,255}}; email must be
	 * nonblank and satisfy {@code @Email}. An optional employee number must match
	 * {@code [A-Za-z0-9_-]{1,50}} without trimming.
	 * </p>
	 * <p>
	 * An existing profile is returned unchanged, including its email, preferences
	 * and employee association. On creation, AppUser trims and lowercases the email
	 * and limits it to 255 normalized characters. A new profile uses configured
	 * defaults and DARK theme, and links only a known unclaimed employee. A
	 * competing subject insert returns the winning profile; a competing employee
	 * claim falls back to an unlinked profile. Successful inserts commit
	 * independently of the caller's transaction.
	 * </p>
	 *
	 * @param  keycloakSubject              immutable opaque provider subject
	 * @param  email                        verified contact email; normalized only
	 *                                      when creating a profile
	 * @param  employeeNumber               optional initial employee number; null
	 *                                      omits association
	 * @return                              persisted profile belonging to the
	 *                                      subject, with employee data readable
	 * @throws ConstraintViolationException if a parameter constraint fails through
	 *                                      the Spring proxy
	 * @throws IllegalArgumentException     if the email exceeds the normalized
	 *                                      length limit when creating a profile
	 * @throws AppUserCreationConflict      if an insert conflict cannot be
	 *                                      reconciled
	 */
	@Transactional
	public AppUser findOrCreateAppUser(
			final @NotBlank @KeycloakSubject String keycloakSubject,
			final @NotBlank @Email String email,
			final @EmployeeNumber String employeeNumber) {
		final Optional<AppUser> existing = this.appUserRepository.findByKeycloakSubject(keycloakSubject);
		if (existing.isPresent()) {
			return existing.get();
		}
		final Employee employee = this.findUnlinkedEmployee(employeeNumber, keycloakSubject);
		return this.insertAndReconcile(email, keycloakSubject, employee);
	}

	private Employee findUnlinkedEmployee(final String employeeNumber, final String keycloakSubject) {
		if (employeeNumber == null) {
			return null;
		}
		final Employee employee;
		try {
			employee = this.employeeService.findEmployeeByEmployeeNumber(employeeNumber);
		} catch (final ResourceNotFoundException notFound) {
			logger.info("No employee {} found; provisioning an unlinked account", employeeNumber);
			return null;
		}
		final Optional<AppUser> linkedUser = this.appUserRepository.findByEmployee(employee);
		if (linkedUser.isPresent()) {
			// Ops. We have another user linked to this employee
			logger.warn(
					"Employee link conflict for employee {} and keycloakSubject {}; keeping existing link",
					employee,
					keycloakSubject);
			return null;
		}
		return employee;
	}

	private AppUser insertAndReconcile(final String email, final String keycloakSubject, final Employee employee) {
		try {
			// We delegate to another bean so creation runs in its own transaction
			// and a conflict does not roll back ours.
			return this.appUserCreator.create(
					email,
					keycloakSubject,
					this.provisioningDefaults.locale(),
					this.provisioningDefaults.getTimeFormat(),
					this.provisioningDefaults.defaultTimezone(),
					employee == null ? null : employee.getId());
		} catch (final AppUserCreationConflict conflict) {
			// Somehow, we’re experiencing a conflict when creating the user.
			// Another request may have created the profile while we were checking.
			// Let's see whether a profile now exists for the same subject.
			final Optional<AppUser> subjectWinner = this.appUserRepository.findByKeycloakSubject(keycloakSubject);
			if (subjectWinner.isPresent()) {
				return subjectWinner.get();
			}

			if (employee != null) {
				// Let's try it again, but now without trying to link the user to an employee.
				logger.warn(
						"User creation conflict with employee {} and keycloakSubject {}; retrying without linking the employee",
						employee,
						keycloakSubject);
				return this.insertAndReconcile(email, keycloakSubject, null);
			}
			throw conflict;
		}
	}

	/**
	 * Looks up a profile by its exact immutable provider subject. Subject must
	 * match {@code [A-Za-z0-9_-]{1,255}} without trimming. A successful lookup
	 * loads the employee association without modifying or provisioning a profile.
	 * Authorization callers must translate absence into their own denial contract.
	 *
	 * @param  keycloakSubject              opaque subject scoped to the configured
	 *                                      issuer
	 * @return                              matching profile with its optional
	 *                                      employee association loaded
	 * @throws ConstraintViolationException if subject is invalid when invoked
	 *                                      through the Spring proxy
	 * @throws ResourceNotFoundException    if no profile exists
	 */
	@Transactional(readOnly = true)
	public AppUser findAppUserByKeycloakSubject(final @NotBlank @KeycloakSubject String keycloakSubject) {
		return this.appUserRepository.findByKeycloakSubject(keycloakSubject)
				.orElseThrow(() -> new ResourceNotFoundException("There is no application user for this subject"));
	}

	/**
	 * Replaces a profile's preferences in one transaction. The caller must
	 * authorize the change. All arguments must be nonnull and the profile must
	 * exist. All preference values are checked before mutation. On success the
	 * returned profile has the new preferences; subject, email and employee
	 * association are preserved. Invalid values leave preferences unchanged.
	 *
	 * @param  id                           persistent UUID of the target profile
	 * @param  newLocale                    replacement locale
	 * @param  newTimeFormat                replacement time display format
	 * @param  newDefaultTimezone           replacement timezone
	 * @param  newTheme                     replacement color theme
	 * @return                              updated profile whose changes commit
	 *                                      with the transaction
	 * @throws ConstraintViolationException if id or a preference is null when
	 *                                      invoked through the Spring proxy
	 * @throws ResourceNotFoundException    if the target is absent
	 */
	@Transactional
	public AppUser updatePreferences(
			final @NotNull UUID id,
			final @NotNull Locale newLocale,
			final @NotNull TimeFormat newTimeFormat,
			final @NotNull ZoneId newDefaultTimezone,
			final @NotNull Theme newTheme) {
		final AppUser appUser = this.findAppUserById(id);
		appUser.updatePreferences(newLocale, newTimeFormat, newDefaultTimezone, newTheme);
		return appUser;
	}
	
	private AppUser findAppUserById(final UUID id) {
		return this.appUserRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("There is no application user with id " + id));
	}

	/**
	 * Deletes an existing local profile and clears its bidirectional employee link.
	 * The caller must authorize deletion and supply a nonnull existing UUID. After
	 * a successful transaction the profile is absent while the employee and
	 * identity-provider account remain unchanged.
	 *
	 * @param  id                           persistent UUID of the profile to delete
	 * @throws ConstraintViolationException if id is null when invoked through the
	 *                                      Spring proxy
	 * @throws ResourceNotFoundException    if the target is absent
	 */
	@Transactional
	public void deleteAppUser(final @NotNull UUID id) {
		final AppUser appUser = this.findAppUserById(id);
		appUser.setEmployee(null);
		this.appUserRepository.delete(appUser);
		logger.debug("Deleted AppUser {}", id);
	}

	/**
	 * Searches profiles without changing them or provisioning an account.
	 * <p>
	 * The caller must authorize the search and supply a nonnull, paged request. The
	 * optional employee number must satisfy its declared constraint. Only id, email
	 * and employeeNumber may be used as public sort fields.
	 * </p>
	 * <p>
	 * Filters combine with AND. Absent filters include every profile, including
	 * unlinked ones. Email matching is literal, partial and case-insensitive;
	 * employee number matches exactly without trimming. Page size is capped at the
	 * configured {@code spring.data.rest.max-page-size}. Default ordering is email
	 * ascending, with ascending UUID as tie-breaker unless UUID is explicitly
	 * ordered. Employee associations are loaded.
	 * </p>
	 *
	 * @param  emailFilter                  optional literal fragment, used without
	 *                                      trimming or length validation; null or
	 *                                      empty disables the email restriction
	 * @param  employeeNumber               optional employee number matching
	 *                                      {@code [A-Za-z0-9_-]{1,50}} without
	 *                                      trimming
	 * @param  pageable                     requested page and public ordering
	 * @return                              page of matching profiles, possibly
	 *                                      empty, with employee data readable
	 * @throws ConstraintViolationException if employeeNumber is invalid or pageable
	 *                                      is null when invoked through the Spring
	 *                                      proxy
	 * @throws IllegalArgumentException     if pageable is unpaged or a sort field
	 *                                      is unsupported
	 */
	@Transactional(readOnly = true)
	public Page<AppUser> searchAppUsers(
			final String emailFilter,
			final @EmployeeNumber String employeeNumber,
			final @NotNull Pageable pageable) {
		final Pageable normalizedPageable = this.normalizePageable(pageable);
		final String escapedEmailFilter = this.escapeEmailFilter(emailFilter);
		return this.appUserRepository.search(escapedEmailFilter, employeeNumber, normalizedPageable);
	}

	private String escapeEmailFilter(final String emailFilter) {
		return emailFilter == null ? "" : LikePatterns.escape(emailFilter);
	}

	private Sort normalizeSort(final Sort sort) {
		final List<Order> orders = new ArrayList<>();
		for (final Order sortOrder : sort) {
			final String property = this.resolveSortProperty(sortOrder.getProperty());
			final Order normalizedSortProperty = sortOrder.withProperty(property);
			orders.add(normalizedSortProperty);
		}
		if (orders.isEmpty()) {
			orders.add(Order.asc("email"));
		}
		// Add the ID as a secondary sort criterion to ensure deterministic ordering
		// when multiple records have the same value for the primary sort field.
		if (orders.stream().noneMatch(order -> "id".equals(order.getProperty()))) {
			orders.add(Order.asc("id"));
		}
		return Sort.by(orders);
	}

	private Pageable normalizePageable(final Pageable pageable) {
		if (pageable.isUnpaged()) {
			throw new IllegalArgumentException("Must be paged");
		}
		final int pageNumber = pageable.getPageNumber();
		final int pageSize = Math.min(pageable.getPageSize(), this.maxPageSize);
		final Sort normalizedSort = this.normalizeSort(pageable.getSort());
		return PageRequest.of(pageNumber, pageSize, normalizedSort);

	}

	private String resolveSortProperty(final String property) {
		return switch (property) {
		case "id", "email" -> property;
		case "employeeNumber" -> "employee.employeeNumber";
		default -> throw new IllegalArgumentException("Unsupported AppUser sort field: " + property);
		};
	}
}
