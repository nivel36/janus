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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.nivel36.janus.config.UserProvisioningProperties;
import es.nivel36.janus.service.ResourceNotFoundException;
import es.nivel36.janus.service.TimeFormat;
import es.nivel36.janus.service.employee.Employee;
import es.nivel36.janus.service.employee.EmployeeService;
import es.nivel36.janus.util.LikePatterns;
import es.nivel36.janus.validation.EmployeeNumber;

/**
 * Transactional entry point for provisioning, lookup, search, preference
 * changes and deletion of local application profiles. Dependencies must be
 * nonnull and provisioning defaults validated before use. Method contracts
 * define input validation and database effects; authorization belongs to the
 * resource policies and is not performed by this service. Provisioning alone
 * uses an independent insert transaction to reconcile races; lookup and search
 * never provision profiles or change associations.
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
		@Value("${spring.data.rest.max-page-size}")
		final int maxPageSize) {
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
	 * provisioning. Subject is nonnull, nonblank and at most 255 characters;
	 * normalized email is nonblank and at most 255 characters. An optional employee
	 * number must match {@code [A-Za-z0-9_-]{1,50}} after trimming.
	 * </p>
	 * <p>
	 * An existing profile refreshes email and preserves preferences and employee
	 * association. A new profile uses configured defaults and DARK theme, and links
	 * only a known unclaimed employee. A competing subject insert returns the
	 * winning profile; a competing employee claim falls back to an unlinked
	 * profile. Successful inserts commit independently of the caller's transaction.
	 * </p>
	 *
	 * @param  keycloakSubject          immutable opaque provider subject
	 * @param  email                    verified contact email, trimmed and
	 *                                  lowercased
	 * @param  employeeNumber           optional initial employee number; null omits
	 *                                  association
	 * @return                          persisted profile belonging to the subject,
	 *                                  with employee data readable
	 * @throws NullPointerException     if subject is null
	 * @throws IllegalArgumentException if email, subject or employee number is
	 *                                  invalid
	 * @throws AppUserCreationConflict  if an insert conflict cannot be reconciled
	 */
	@Transactional
	public AppUser findOrCreateAppUser(final String keycloakSubject, final String email, final String employeeNumber) {
		if (email == null || email.isBlank()) {
			throw new IllegalArgumentException("email claim is required");
		}
		AppUser.validateKeycloakSubject(keycloakSubject);
		final String normalizedEmail = AppUser.validateEmail(email);
		final String normalizedEmployeeNumber = normalizeEmployeeNumber(employeeNumber);
		final Optional<AppUser> existing = this.appUserRepository.findByKeycloakSubject(keycloakSubject);
		if (existing.isPresent()) {
			final AppUser appUser = existing.get();
			appUser.setEmail(normalizedEmail);
			return appUser;
		}

		final Employee employee = this.findUnlinkedEmployee(normalizedEmployeeNumber, keycloakSubject);
		return this.insertAndReconcile(normalizedEmail, keycloakSubject, employee);
	}

	private AppUser insertAndReconcile(final String email, final String keycloakSubject, final Employee employee) {
		try {
			return this.appUserCreator.create(
					email,
					keycloakSubject,
					this.provisioningDefaults.locale(),
					this.provisioningDefaults.getTimeFormat(),
					this.provisioningDefaults.defaultTimezone(),
					employee == null ? null : employee.getId());
		} catch (final AppUserCreationConflict conflict) {
			final Optional<AppUser> subjectWinner = this.appUserRepository.findByKeycloakSubject(keycloakSubject);
			if (subjectWinner.isPresent()) {
				final AppUser appUser = subjectWinner.get();
				if (!keycloakSubject.equals(appUser.getKeycloakSubject())) {
					throw new IllegalStateException("Subject lookup returned a profile for a different identity");
				}
				return appUser;
			}
			if (employee != null) {
				this.logEmployeeConflict(employee, keycloakSubject);
				return this.insertAndReconcile(email, keycloakSubject, null);
			}
			throw conflict;
		}
	}

	private Employee findUnlinkedEmployee(final String employeeNumber, final String keycloakSubject) {
		if (employeeNumber == null) {
			return null;
		}
		final Employee employee;
		try {
			employee = this.employeeService.findEmployeeByEmployeeNumber(employeeNumber);
		} catch (final ResourceNotFoundException notFound) {
			logger.info(
					"No employee found for employeeNumber claim {}; provisioning an unlinked account",
					employeeNumber);
			return null;
		}
		final Optional<AppUser> linkedUser = this.appUserRepository.findByEmployee(employee);

		if (linkedUser.isPresent()) {
			this.logEmployeeConflict(employee, keycloakSubject);
			return null;
		}

		return employee;
	}

	private void logEmployeeConflict(final Employee employee, final String keycloakSubject) {
		logger.warn(
				"Employee identity link conflict for employeeId={} and keycloakSubject={}; keeping existing link",
				employee.getId(),
				keycloakSubject);
	}

	/**
	 * Looks up an existing profile without changing or provisioning it. The id must
	 * be nonnull. A successful lookup loads the optional employee association so it
	 * remains readable after the transaction ends.
	 *
	 * @param  id                        persistent profile UUID
	 * @return                           matching profile with its optional employee
	 *                                   association loaded
	 * @throws NullPointerException      if id is null
	 * @throws ResourceNotFoundException if no profile exists
	 */
	@Transactional(readOnly = true)
	public AppUser findAppUserById(final UUID id) {
		Objects.requireNonNull(id, "id cannot be null.");
		return this.appUserRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("There is no application user with id " + id));
	}

	/**
	 * Looks up a profile by its exact immutable provider subject. Subject must be
	 * nonnull, nonblank and at most 255 characters. A successful lookup loads the
	 * employee association without modifying or provisioning a profile.
	 * Authorization callers must translate absence into their own denial contract.
	 *
	 * @param  keycloakSubject           opaque subject scoped to the configured
	 *                                   issuer
	 * @return                           matching profile with its optional employee
	 *                                   association loaded
	 * @throws NullPointerException      if subject is null
	 * @throws IllegalArgumentException  if subject is blank or oversized
	 * @throws ResourceNotFoundException if no profile exists
	 */
	@Transactional(readOnly = true)
	public AppUser findAppUserByKeycloakSubject(final String keycloakSubject) {
		AppUser.validateKeycloakSubject(keycloakSubject);
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
	 * @param  id                        persistent UUID of the target profile
	 * @param  newLocale                 replacement locale
	 * @param  newTimeFormat             replacement time display format
	 * @param  newDefaultTimezone        replacement timezone
	 * @param  newTheme                  replacement color theme
	 * @return                           updated profile whose changes commit with
	 *                                   the transaction
	 * @throws NullPointerException      if id or any preference is null
	 * @throws ResourceNotFoundException if the target is absent
	 */
	@Transactional
	public AppUser updateAppUser(
			final UUID id,
			final Locale newLocale,
			final TimeFormat newTimeFormat,
			final ZoneId newDefaultTimezone,
			final Theme newTheme) {
		final AppUser appUser = this.findAppUserById(id);
		appUser.updatePreferences(newLocale, newTimeFormat, newDefaultTimezone, newTheme);
		return appUser;
	}

	/**
	 * Deletes an existing local profile and clears its bidirectional employee link.
	 * The caller must authorize deletion and supply a nonnull existing UUID. After
	 * a successful transaction the profile is absent while the employee and
	 * identity-provider account remain unchanged.
	 *
	 * @param  id                        persistent UUID of the profile to delete
	 * @throws NullPointerException      if id is null
	 * @throws ResourceNotFoundException if the target is absent
	 */
	@Transactional
	public void deleteAppUser(final UUID id) {
		final AppUser appUser = this.findAppUserById(id);
		appUser.setEmployee(null);
		this.appUserRepository.delete(appUser);
		logger.debug("Deleted AppUser {}", id);
	}

	/**
	 * Searches profiles without changing them or provisioning an account.
	 * <p>
	 * The caller must authorize the search and supply a nonnull, paged request.
	 * Optional filters must satisfy the rules below. Only id, email and
	 * employeeNumber may be used as public sort fields.
	 * </p>
	 * <p>
	 * Filters combine with AND. Absent filters include every profile, including
	 * unlinked ones. Email matching is literal, partial and case-insensitive;
	 * employee number matches exactly after trimming. Page size is capped at the
	 * configured {@code spring.data.rest.max-page-size}. Default ordering is email
	 * ascending, with ascending UUID as tie-breaker unless UUID is explicitly
	 * ordered. Employee associations are loaded.
	 * </p>
	 *
	 * @param  email                    optional nonblank single-line fragment of at
	 *                                  most 255 characters
	 * @param  employeeNumber           optional employee number matching
	 *                                  {@code [A-Za-z0-9_-]{1,50}} after trimming
	 * @param  pageable                 requested page and public ordering
	 * @return                          page of matching profiles, possibly empty,
	 *                                  with employee data readable
	 * @throws NullPointerException     if pageable is null
	 * @throws IllegalArgumentException if a filter or sort field is invalid
	 */
	@Transactional(readOnly = true)
	public Page<AppUser> searchAppUsers(final String email, final String employeeNumber, final Pageable pageable) {
		final String normalizedEmail = normalizeEmailFilter(email);
		final String normalizedEmployeeNumber = normalizeEmployeeNumber(employeeNumber);
		Objects.requireNonNull(pageable, "pageable can't be null");
		final List<Sort.Order> orders = new ArrayList<>();
		for (final Sort.Order order : pageable.getSort()) {
			final String property = this.resolveSortProperty(order.getProperty());
			orders.add(order.withProperty(property));
		}
		if (orders.isEmpty()) {
			orders.add(Sort.Order.asc("email"));
		}
		if (orders.stream().noneMatch(order -> "id".equals(order.getProperty()))) {
			orders.add(Sort.Order.asc("id"));
		}
		final int pageNumber = pageable.getPageNumber();
		final int pageSize = Math.min(pageable.getPageSize(), this.maxPageSize);
		final Sort by = Sort.by(orders);
		final Pageable sorted = PageRequest.of(pageNumber, pageSize, by);
		return this.appUserRepository.search(
				normalizedEmail == null ? "" : LikePatterns.escape(normalizedEmail),
				normalizedEmployeeNumber,
				sorted);
	}

	private String resolveSortProperty(final String property) {
		return switch (property) {
		case "id", "email" -> property;
		case "employeeNumber" -> "employee.employeeNumber";
		default -> throw new IllegalArgumentException("Unsupported AppUser sort field: " + property);
		};
	}

	private static String normalizeEmailFilter(final String email) {
		if (email == null) {
			return null;
		}
		if (email.length() > 255 || !email.matches("[\\p{L}\\p{M}\\p{N}\\p{Zs}\\p{P}\\p{S}]+")) {
			throw new IllegalArgumentException(
					"email filter must be single-line text containing at most 255 characters");
		}
		return AppUser.validateEmail(email);
	}

	private static String normalizeEmployeeNumber(final String employeeNumber) {
		if (employeeNumber == null) {
			return null;
		}
		final String normalized = employeeNumber.trim();
		if (!normalized.matches(EmployeeNumber.PATTERN)) {
			throw new IllegalArgumentException(
					"employeeNumber must contain only letters, digits, underscores or hyphens (1-50 characters)");
		}
		return normalized;
	}
}
