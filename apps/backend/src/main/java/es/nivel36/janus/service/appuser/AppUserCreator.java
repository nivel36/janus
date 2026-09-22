/*
 * Copyright 2026 Abel Ferrer Jiménez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */
package es.nivel36.janus.service.appuser;

import java.time.ZoneId;
import java.util.Locale;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import es.nivel36.janus.service.TimeFormat;
import es.nivel36.janus.service.employee.Employee;

/** Persists an automatically provisioned user in an independent transaction. */
@Service
class AppUserCreator {

	private final AppUserRepository appUserRepository;

	AppUserCreator(final AppUserRepository appUserRepository) {
		this.appUserRepository = appUserRepository;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public AppUser create(final String email, final String keycloakSubject, final Locale locale,
			final TimeFormat timeFormat, final ZoneId defaultTimezone, final Employee employee) {
		final AppUser appUser = new AppUser(email, keycloakSubject, locale, timeFormat, defaultTimezone);
		appUser.setEmployee(employee);
		try {
			return this.appUserRepository.saveAndFlush(appUser);
		} catch (final DataAccessException failure) {
			final AppUserCreationConflict.Key key = failure instanceof DataIntegrityViolationException integrity
					? constraintKey(integrity)
					: AppUserCreationConflict.Key.UNKNOWN;
			throw new AppUserCreationConflict(key, failure);
		}
	}

	private static AppUserCreationConflict.Key constraintKey(final DataIntegrityViolationException failure) {
		Throwable cause = failure;
		while (cause != null) {
			if (cause instanceof ConstraintViolationException violation) {
				return constraintKey(violation.getConstraintName());
			}
			cause = cause.getCause();
		}
		return constraintKey(failure.getMessage());
	}

	private static AppUserCreationConflict.Key constraintKey(final String constraintName) {
		if (constraintName == null) {
			return AppUserCreationConflict.Key.UNKNOWN;
		}
		final String normalized = constraintName.toUpperCase(Locale.ROOT);
		if (normalized.contains("UK_APP_USER_KEYCLOAK_SUBJECT")) {
			return AppUserCreationConflict.Key.KEYCLOAK_SUBJECT;
		}
		if (normalized.contains("UK_APP_USER_EMPLOYEE")) {
			return AppUserCreationConflict.Key.EMPLOYEE;
		}
		return AppUserCreationConflict.Key.UNKNOWN;
	}
}
