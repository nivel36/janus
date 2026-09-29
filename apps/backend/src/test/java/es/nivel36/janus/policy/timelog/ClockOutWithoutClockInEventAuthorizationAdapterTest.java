/*
 * Copyright 2026 Abel Ferrer Jiménez
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package es.nivel36.janus.policy.timelog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import es.nivel36.janus.security.Actor;
import es.nivel36.janus.security.ActorResolver;
import es.nivel36.janus.service.ResourceNotFoundException;
import es.nivel36.janus.service.appuser.Role;
import es.nivel36.janus.service.employee.EmployeeService;

class ClockOutWithoutClockInEventAuthorizationAdapterTest {

	private final Authentication authentication = mock(Authentication.class);
	private final ActorResolver actors = mock(ActorResolver.class);
	private final EmployeeService employees = mock(EmployeeService.class);
	private final ClockOutWithoutClockInEventAuthorizationAdapter adapter =
			new ClockOutWithoutClockInEventAuthorizationAdapter(this.actors, this.employees);

	@Test
	void missingEmployeeIsDeniedWithoutDisclosingItsAbsence() {
		when(this.actors.resolve(this.authentication)).thenReturn(new Actor(
				UUID.fromString("11111111-1111-4111-8111-111111111111"), Set.of(Role.JANUS_ADMIN), null));
		when(this.employees.findEmployeeByEmployeeNumber("MISSING"))
				.thenThrow(new ResourceNotFoundException("There is no employee with selector MISSING"));

		assertThat(this.adapter.canView(this.authentication, "MISSING")).isFalse();
		assertThat(this.adapter.canResolve(this.authentication, "MISSING")).isFalse();
		assertThat(this.adapter.canInvalidate(this.authentication, "MISSING")).isFalse();
	}
}
