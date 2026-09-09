/*
 * Copyright 2026 Abel Ferrer Jiménez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */
package es.nivel36.janus.service.appuser;

import java.time.ZoneId;
import java.util.Locale;

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
	public AppUser create(final String username, final String keycloakSubject, final Locale locale,
			final TimeFormat timeFormat, final ZoneId defaultTimezone, final Employee employee) {
		final AppUser appUser = new AppUser(username, keycloakSubject, locale, timeFormat, defaultTimezone);
		appUser.setEmployee(employee);
		return this.appUserRepository.saveAndFlush(appUser);
	}
}
