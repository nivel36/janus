/*
 * Copyright 2026 Abel Ferrer Jiménez
 * Licensed under the Apache License, Version 2.0
 */
package es.nivel36.janus.api.v1.appuser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.ZoneId;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import es.nivel36.janus.service.appuser.AppUser;
import es.nivel36.janus.service.employee.Employee;

class AppUserResponseMapperTest {

	@Test
	void mapsEmployeeNumberFromLinkedEmployee() {
		final AppUser appUser = mock(AppUser.class);
		final Employee employee = mock(Employee.class);
		when(appUser.getEmployee()).thenReturn(employee);
		when(employee.getEmployeeNumber()).thenReturn("EMP-0001");
		when(appUser.getLocale()).thenReturn(Locale.ENGLISH);
		when(appUser.getDefaultTimezone()).thenReturn(ZoneId.of("UTC"));

		final AppUserResponse response = new AppUserResponseMapper().map(appUser);

		assertThat(response.employeeNumber()).isEqualTo("EMP-0001");
	}
}
