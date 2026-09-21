/*
 * Copyright 2026 Abel Ferrer Jiménez
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package es.nivel36.janus.policy.timelog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.security.core.Authentication;

import es.nivel36.janus.security.Actor;
import es.nivel36.janus.security.ActorResolver;
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
	private final TimeLogAuthorizationAdapter adapter = new TimeLogAuthorizationAdapter(this.actors, this.employees,
			this.settings);

	@ParameterizedTest
	@EnumSource(Role.class)
	void searchPermissionDoesNotRequireEmployeeAssociation(final Role role) {
		when(this.actors.resolve(this.authentication)).thenReturn(new Actor(java.util.UUID.fromString("11111111-1111-4111-8111-111111111111"), Set.of(role), null));

		assertThat(this.adapter.canSearch(this.authentication)).isTrue();
		verify(this.actors).resolve(this.authentication);
		verifyNoInteractions(this.employees, this.settings);
	}

	@Test
	void actorsWithoutRolesCannotExecuteSearches() {
		when(this.actors.resolve(this.authentication)).thenReturn(new Actor(java.util.UUID.fromString("11111111-1111-4111-8111-111111111111"), Set.of(), 84L));

		assertThat(this.adapter.canSearch(this.authentication)).isFalse();
	}

	@Test
	void scopeUsesTheEmployeeAssociationOfTheResolvedActor() {
		when(this.actors.resolve(this.authentication)).thenReturn(new Actor(java.util.UUID.fromString("11111111-1111-4111-8111-111111111111"), Set.of(Role.JANUS_EMPLOYEE), 84L));

		assertThat(this.adapter.searchScope(this.authentication)).isEqualTo(new TimeLogSearchScope.Employee(84L));
		verify(this.actors).resolve(this.authentication);
		verifyNoInteractions(this.employees, this.settings);
	}

	@Test
	void permittedEmployeeSearchCanHaveNoVisibleRows() {
		when(this.actors.resolve(this.authentication)).thenReturn(new Actor(java.util.UUID.fromString("11111111-1111-4111-8111-111111111111"), Set.of(Role.JANUS_EMPLOYEE), null));

		assertThat(this.adapter.canSearch(this.authentication)).isTrue();
		assertThat(this.adapter.searchScope(this.authentication)).isEqualTo(new TimeLogSearchScope.None());
	}

	@Test
	void ownerRemainsAuthorizedWhenRequestedEmailHasMixedCapitalization() {
		final Employee employee = mock(Employee.class);
		when(this.actors.resolve(this.authentication))
				.thenReturn(new Actor(java.util.UUID.fromString("11111111-1111-4111-8111-111111111111"), Set.of(Role.JANUS_EMPLOYEE), 84L));
		when(this.employees.findEmployeeByEmail("employee@internal.test")).thenReturn(employee);
		when(employee.getId()).thenReturn(84L);

		assertThat(this.adapter.canView(this.authentication, "Employee@Internal.Test")).isTrue();
		verify(this.employees).findEmployeeByEmail("employee@internal.test");
	}

	@Test
	void otherEmployeeRemainsDeniedWhenRequestedEmailHasMixedCapitalization() {
		final Employee employee = mock(Employee.class);
		when(this.actors.resolve(this.authentication))
				.thenReturn(new Actor(java.util.UUID.fromString("11111111-1111-4111-8111-111111111111"), Set.of(Role.JANUS_EMPLOYEE), 84L));
		when(this.employees.findEmployeeByEmail("other@internal.test")).thenReturn(employee);
		when(employee.getId()).thenReturn(85L);

		assertThat(this.adapter.canView(this.authentication, "OtHeR@Internal.Test")).isFalse();
		verify(this.employees).findEmployeeByEmail("other@internal.test");
	}
}
