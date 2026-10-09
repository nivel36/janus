/*
 * Copyright 2026 Abel Ferrer Jiménez
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 */
package es.nivel36.janus.service.appuser;

import java.time.ZoneId;
import java.util.Locale;
import java.util.Objects;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import es.nivel36.janus.service.TimeFormat;
import es.nivel36.janus.service.employee.Employee;
import es.nivel36.janus.validation.KeycloakSubject;
import jakarta.persistence.EntityManager;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Internal first-access writer invoked through Spring's transactional proxy.
 */
@Validated
@Service
class AppUserCreator {

	private final AppUserRepository appUserRepository;
	private final EntityManager entityManager;

	/**
	 * Creates the internal writer without database access. Both dependencies must
	 * be non-null and are checked during construction.
	 *
	 * @param  appUserRepository    profile repository. Must be nonnull.
	 * @param  entityManager        transaction-bound persistence context Must be
	 *                              nonnull
	 * @throws NullPointerException if entityManager or appUserRepository are
	 *                              {@code null}
	 */
	AppUserCreator(final AppUserRepository appUserRepository, final EntityManager entityManager) {
		this.appUserRepository = Objects.requireNonNull(appUserRepository, "appUserRepository cannot be null");
		this.entityManager = Objects.requireNonNull(entityManager, "entityManager cannot be null");
	}

	/**
	 * Inserts a profile in an independent transaction.
	 * <p>
	 * The caller must validate trusted claims and initial preferences and invoke
	 * the Spring proxy. The optional employee must still be unclaimed.
	 * </p>
	 * <p>
	 * A successful insert commits the new profile's UUID, preferences and optional
	 * employee link. A missing employee row produces an unlinked profile. Conflicts
	 * roll back this transaction without altering the outer transaction's employee
	 * entities; the caller is responsible for reconciling the conflict.
	 * </p>
	 *
	 * @param  email                    nonblank contact email, at most 254
	 *                                  normalized characters
	 * @param  keycloakSubject          nonblank opaque subject, at most 255
	 *                                  characters
	 * @param  locale                   nonnull initial locale
	 * @param  timeFormat               nonnull initial time format
	 * @param  defaultTimezone          nonnull initial time zone
	 * @param  employeeId               optional employee database identifier;
	 *                                  {@code null} creates unlinked
	 * @return                          newly persisted profile with readable
	 *                                  employee data
	 * @throws NullPointerException     if a required profile value is {@code null}
	 * @throws IllegalArgumentException if email or subject is blank or oversized
	 * @throws AppUserCreationConflict  if the employee was claimed or insertion
	 *                                  violates a database integrity constraint
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public AppUser create(
			final @NotEmpty @Email @Size(max = 254) String email,
			final @NotEmpty @KeycloakSubject String keycloakSubject,
			final @NotNull Locale locale,
			final @NotNull TimeFormat timeFormat,
			final @NotNull ZoneId defaultTimezone,
			final Long employeeId) {
		final AppUser appUser = new AppUser(email, keycloakSubject, locale, timeFormat, defaultTimezone);
		if (employeeId != null) {
			// Resolve the association in this transaction, never mutate an outer entity.
			final Employee employee = this.entityManager.find(Employee.class, employeeId);
			if (employee != null && employee.getAppUser() != null) {
				throw new AppUserCreationConflict(new IllegalStateException("Employee is already linked"));
			}
			appUser.setEmployee(employee);
		}
		try {
			return this.appUserRepository.saveAndFlush(appUser);
		} catch (final DataIntegrityViolationException conflict) {
			throw new AppUserCreationConflict(conflict);
		}
	}
}
