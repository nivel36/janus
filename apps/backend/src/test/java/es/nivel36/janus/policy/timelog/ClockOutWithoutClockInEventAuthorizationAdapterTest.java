/*
 * Copyright 2026 Abel Ferrer Jiménez Licensed under the Apache License, Version
 * 2.0 (the "License");
 */
package es.nivel36.janus.policy.timelog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import es.nivel36.janus.api.v1.timelog.ClockOutWithoutClockInEventAction;
import es.nivel36.janus.policy.EmployeeNumberResolver;
import es.nivel36.janus.policy.Policy;
import es.nivel36.janus.security.Actor;
import es.nivel36.janus.security.ActorResolver;
import es.nivel36.janus.service.ResourceNotFoundException;
import es.nivel36.janus.service.appuser.Role;
import es.nivel36.janus.service.employee.Employee;
import es.nivel36.janus.service.employee.EmployeeService;

class ClockOutWithoutClockInEventAuthorizationAdapterTest {

	private final Authentication authentication = mock(Authentication.class);
	private final ActorResolver actors = mock(ActorResolver.class);
	private final EmployeeService employees = mock(EmployeeService.class);
	private final ClockOutWithoutClockInEventAuthorizationAdapter adapter = new ClockOutWithoutClockInEventAuthorizationAdapter(
			this.actors,
			this.employees,
			new EmployeeNumberResolver(this.employees));

	@Test
	void missingEmployeeIsDeniedWithoutDisclosingItsAbsence() {
		when(this.actors.resolve(this.authentication)).thenReturn(
				new Actor(UUID.fromString("11111111-1111-4111-8111-111111111111"), Set.of(Role.JANUS_ADMIN), null));
		when(this.employees.findEmployeeByEmployeeNumber("MISSING"))
				.thenThrow(new ResourceNotFoundException("There is no employee with selector MISSING"));

		assertThat(this.adapter.canView(this.authentication, "MISSING")).isFalse();
		assertThat(this.adapter.canResolve(this.authentication, "MISSING")).isFalse();
		assertThat(this.adapter.canInvalidate(this.authentication, "MISSING")).isFalse();
	}

	@Test
	void resolveTransitionOnlyRequiresResolvePolicy() {
		final Policy<Long> resolvePolicy = policyAllowing(true);
		final Policy<Long> invalidatePolicy = policyAllowing(false);
		final ClockOutWithoutClockInEventAuthorizationAdapter adapter = this
				.adapterWith(resolvePolicy, invalidatePolicy);

		assertThat(adapter.canTransition(this.authentication, "EMP-0001", ClockOutWithoutClockInEventAction.RESOLVE))
				.isTrue();
		verify(resolvePolicy).allows(actor(), 42L);
		verify(invalidatePolicy, never()).allows(actor(), 42L);
	}

	@Test
	void invalidateTransitionOnlyRequiresInvalidatePolicy() {
		final Policy<Long> resolvePolicy = policyAllowing(false);
		final Policy<Long> invalidatePolicy = policyAllowing(true);
		final ClockOutWithoutClockInEventAuthorizationAdapter adapter = this
				.adapterWith(resolvePolicy, invalidatePolicy);

		assertThat(adapter.canTransition(this.authentication, "EMP-0001", ClockOutWithoutClockInEventAction.INVALIDATE))
				.isTrue();
		verify(invalidatePolicy).allows(actor(), 42L);
		verify(resolvePolicy, never()).allows(actor(), 42L);
	}

	@Test
	void transitionWithoutActionIsDeniedWithoutConsultingPolicies() {
		final Policy<Long> resolvePolicy = mock();
		final Policy<Long> invalidatePolicy = mock();
		final ClockOutWithoutClockInEventAuthorizationAdapter adapter = this
				.adapterWith(resolvePolicy, invalidatePolicy);

		assertThat(adapter.canTransition(this.authentication, "EMP-0001", null)).isFalse();
		verify(resolvePolicy, never()).allows(actor(), 42L);
		verify(invalidatePolicy, never()).allows(actor(), 42L);
	}

	private ClockOutWithoutClockInEventAuthorizationAdapter adapterWith(
			final Policy<Long> resolvePolicy,
			final Policy<Long> invalidatePolicy) {
		final Employee employee = employee();
		when(this.actors.resolve(this.authentication)).thenReturn(actor());
		when(this.employees.findEmployeeByEmployeeNumber("EMP-0001")).thenReturn(employee);
		return new ClockOutWithoutClockInEventAuthorizationAdapter(
				this.actors,
				this.employees,
				new EmployeeNumberResolver(this.employees),
				resolvePolicy,
				invalidatePolicy);
	}

	@SuppressWarnings("unchecked")
	private static Policy<Long> policyAllowing(final boolean result) {
		final Policy<Long> policy = mock(Policy.class);
		when(policy.allows(actor(), 42L)).thenReturn(result);
		return policy;
	}

	private static Actor actor() {
		return new Actor(UUID.fromString("11111111-1111-4111-8111-111111111111"), Set.of(Role.JANUS_ADMIN), null);
	}

	private static Employee employee() {
		final Employee employee = mock(Employee.class);
		when(employee.getId()).thenReturn(42L);
		return employee;
	}
}
