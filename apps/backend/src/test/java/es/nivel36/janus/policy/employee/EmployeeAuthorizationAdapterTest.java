/*
 * Copyright 2026 Abel Ferrer Jiménez
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package es.nivel36.janus.policy.employee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import es.nivel36.janus.security.Actor;
import es.nivel36.janus.security.ActorResolver;
import es.nivel36.janus.service.appuser.Role;
import es.nivel36.janus.service.employee.Employee;
import es.nivel36.janus.service.employee.EmployeeService;

class EmployeeAuthorizationAdapterTest {

	private static final String EMPLOYEE_EMAIL = "employee@internal.test";
	private static final long EMPLOYEE_ID = 11L;

	@Test
	void canViewResolvesTheActorAndRequestedEmployeeIdentifier() {
		final Authentication authentication = mock(Authentication.class);
		final ActorResolver actorResolver = mock(ActorResolver.class);
		final EmployeeService employeeService = mock(EmployeeService.class);
		final Employee employee = mock(Employee.class);
		when(actorResolver.resolve(authentication))
				.thenReturn(new Actor(1L, Set.of(Role.JANUS_EMPLOYEE), EMPLOYEE_ID));
		when(employeeService.findEmployeeByEmail(EMPLOYEE_EMAIL)).thenReturn(employee);
		when(employee.getId()).thenReturn(EMPLOYEE_ID);

		final EmployeeAuthorizationAdapter adapter = new EmployeeAuthorizationAdapter(actorResolver, employeeService);

		assertThat(adapter.canView(authentication, EMPLOYEE_EMAIL)).isTrue();
		verify(actorResolver).resolve(authentication);
		verify(employeeService).findEmployeeByEmail(EMPLOYEE_EMAIL);
		verify(employee).getId();
	}

	@Test
	void canViewDeniesWhenResolvedActorDoesNotOwnTheRequestedEmployee() {
		final Authentication authentication = mock(Authentication.class);
		final ActorResolver actorResolver = mock(ActorResolver.class);
		final EmployeeService employeeService = mock(EmployeeService.class);
		final Employee employee = mock(Employee.class);
		when(actorResolver.resolve(authentication))
				.thenReturn(new Actor(1L, Set.of(Role.JANUS_EMPLOYEE), EMPLOYEE_ID));
		when(employeeService.findEmployeeByEmail(EMPLOYEE_EMAIL)).thenReturn(employee);
		when(employee.getId()).thenReturn(12L);

		final EmployeeAuthorizationAdapter adapter = new EmployeeAuthorizationAdapter(actorResolver, employeeService);

		assertThat(adapter.canView(authentication, EMPLOYEE_EMAIL)).isFalse();
	}
}
