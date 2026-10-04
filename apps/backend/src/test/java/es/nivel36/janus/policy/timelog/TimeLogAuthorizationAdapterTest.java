/*
 * Copyright 2026 Abel Ferrer Jiménez
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package es.nivel36.janus.policy.timelog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.security.core.Authentication;

import es.nivel36.janus.policy.EmployeeNumberResolver;
import es.nivel36.janus.security.Actor;
import es.nivel36.janus.security.ActorResolver;
import es.nivel36.janus.service.ResourceNotFoundException;
import es.nivel36.janus.service.applicationsettings.ApplicationSettingsService;
import es.nivel36.janus.service.appuser.Role;
import es.nivel36.janus.service.employee.Employee;
import es.nivel36.janus.service.employee.EmployeeService;
import es.nivel36.janus.service.timelog.TimeLogSearchScope;

class TimeLogAuthorizationAdapterTest {

	private final Authentication authentication = mock(Authentication.class);
	private final ActorResolver actors = mock(ActorResolver.class);
	private final EmployeeService employees = mock(EmployeeService.class);
	private final ApplicationSettingsService settings = mock(ApplicationSettingsService.class);
	private final TimeLogAuthorizationAdapter adapter = new TimeLogAuthorizationAdapter(this.actors, this.settings,
			new EmployeeNumberResolver(this.employees));

	@ParameterizedTest
	@EnumSource(value = Role.class, names = { "JANUS_USER", "JANUS_ADMIN" })
	void elevatedSearchPermissionDoesNotRequireEmployeeAssociation(final Role role) {
		when(this.actors.resolve(this.authentication)).thenReturn(
				new Actor(java.util.UUID.fromString("11111111-1111-4111-8111-111111111111"), Set.of(role), null));

		assertThat(this.adapter.canSearch(this.authentication, null)).isTrue();
		verify(this.actors).resolve(this.authentication);
		verifyNoInteractions(this.employees, this.settings);
	}

	@Test
	void actorsWithoutRolesCannotExecuteSearches() {
		when(this.actors.resolve(this.authentication)).thenReturn(
				new Actor(java.util.UUID.fromString("11111111-1111-4111-8111-111111111111"), Set.of(), 84L));

		assertThat(this.adapter.canSearch(this.authentication, null)).isFalse();
	}

	@Test
	void scopeUsesTheEmployeeAssociationOfTheResolvedActor() {
		when(this.actors.resolve(this.authentication)).thenReturn(new Actor(
				java.util.UUID.fromString("11111111-1111-4111-8111-111111111111"), Set.of(Role.JANUS_EMPLOYEE), 84L));

		assertThat(this.adapter.searchScope(this.authentication)).isEqualTo(new TimeLogSearchScope.Employee(84L));
		verify(this.actors).resolve(this.authentication);
		verifyNoInteractions(this.employees, this.settings);
	}

	@Test
	void employeeWithoutAssociationCannotSearch() {
		when(this.actors.resolve(this.authentication)).thenReturn(new Actor(
				java.util.UUID.fromString("11111111-1111-4111-8111-111111111111"), Set.of(Role.JANUS_EMPLOYEE), null));

		assertThat(this.adapter.canSearch(this.authentication, null)).isFalse();
		assertThat(this.adapter.searchScope(this.authentication)).isEqualTo(new TimeLogSearchScope.None());
	}

	@Test
	void employeeAccessUsesPersistentIdRatherThanNumberEquality() {
		final Employee employee = mock(Employee.class);
		when(this.actors.resolve(this.authentication)).thenReturn(new Actor(
				java.util.UUID.fromString("11111111-1111-4111-8111-111111111111"), Set.of(Role.JANUS_EMPLOYEE), 84L));
		when(employee.getId()).thenReturn(84L);
		when(this.employees.findEmployeeByEmployeeNumber("EMP-0001")).thenReturn(employee);

		assertThat(this.adapter.canView(this.authentication, "EMP-0001")).isTrue();
		assertThat(this.adapter.canSearch(this.authentication, "EMP-0001")).isTrue();
	}

	@Test
	void employeeWithoutAssociationCannotOperate() {
		when(this.actors.resolve(this.authentication)).thenReturn(new Actor(
				java.util.UUID.fromString("11111111-1111-4111-8111-111111111111"), Set.of(Role.JANUS_EMPLOYEE), null));

		assertThat(this.adapter.canOperate(this.authentication, "old-address@internal.test", false)).isFalse();
		verifyNoInteractions(this.employees);
	}

	@Test
	void missingEmployeeIsDeniedWithoutDisclosingItsAbsence() {
		when(this.actors.resolve(this.authentication)).thenReturn(new Actor(
				java.util.UUID.fromString("11111111-1111-4111-8111-111111111111"), Set.of(Role.JANUS_USER), 84L));
		when(this.employees.findEmployeeByEmployeeNumber("MISSING"))
				.thenThrow(new ResourceNotFoundException("There is no employee with selector missing@example.test"));

		assertThat(this.adapter.canOperate(this.authentication, "MISSING", false)).isFalse();
	}

	@Test
	void explicitOtherAndMissingEmployeesAreDenied() {
		when(this.actors.resolve(this.authentication))
				.thenReturn(new Actor(java.util.UUID.randomUUID(), Set.of(Role.JANUS_EMPLOYEE), 84L));
		final Employee other = mock(Employee.class);
		when(other.getId()).thenReturn(85L);
		when(this.employees.findEmployeeByEmployeeNumber("OTHER")).thenReturn(other);
		when(this.employees.findEmployeeByEmployeeNumber("MISSING"))
				.thenThrow(new ResourceNotFoundException("missing"));
		assertThat(this.adapter.canSearch(this.authentication, "OTHER")).isFalse();
		assertThat(this.adapter.canSearch(this.authentication, "MISSING")).isFalse();
		assertThat(this.adapter.canSearch(this.authentication, null)).isTrue();
	}

	@Test
	void technicalFailuresAreNotConvertedToDeniedAccess() {
		when(this.actors.resolve(this.authentication))
				.thenReturn(new Actor(java.util.UUID.randomUUID(), Set.of(Role.JANUS_EMPLOYEE), 84L));
		when(this.employees.findEmployeeByEmployeeNumber("OWN"))
				.thenThrow(new IllegalStateException("database unavailable"));
		assertThatThrownBy(() -> this.adapter.canSearch(this.authentication, "OWN"))
				.isInstanceOf(IllegalStateException.class).hasMessage("database unavailable");
	}

}
