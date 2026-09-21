/*
 * Copyright 2026 Abel Ferrer Jiménez
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package es.nivel36.janus.policy.timelog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import es.nivel36.janus.security.Actor;
import es.nivel36.janus.service.appuser.Role;
import es.nivel36.janus.service.timelog.TimeLogSearchScope;

class SearchTimeLogPolicyTest {

	private final SearchTimeLogPolicy policy = new SearchTimeLogPolicy();

	@ParameterizedTest
	@MethodSource("elevatedRoles")
	void elevatedRolesCanSearchAllTimeLogsRegardlessOfEmployeeAssociation(final Set<Role> roles) {
		assertThat(this.policy.scope(new Actor(java.util.UUID.fromString("11111111-1111-4111-8111-111111111111"), roles, null))).isEqualTo(new TimeLogSearchScope.All());
		assertThat(this.policy.scope(new Actor(java.util.UUID.fromString("11111111-1111-4111-8111-111111111111"), roles, 84L))).isEqualTo(new TimeLogSearchScope.All());
	}

	@Test
	void employeesCanSearchOnlyTheirOwnTimeLogs() {
		assertThat(this.policy.scope(new Actor(java.util.UUID.fromString("11111111-1111-4111-8111-111111111111"), Set.of(Role.JANUS_EMPLOYEE), 84L)))
				.isEqualTo(new TimeLogSearchScope.Employee(84L));
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(longs = { 0L, -1L })
	void employeesWithoutAPersistentEmployeeHaveNoVisibleTimeLogs(final Long employeeId) {
		assertThat(this.policy.scope(new Actor(java.util.UUID.fromString("11111111-1111-4111-8111-111111111111"), Set.of(Role.JANUS_EMPLOYEE), employeeId)))
				.isEqualTo(new TimeLogSearchScope.None());
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(longs = { 84L })
	void actorsWithoutRolesHaveNoVisibleTimeLogs(final Long employeeId) {
		assertThat(this.policy.scope(new Actor(java.util.UUID.fromString("11111111-1111-4111-8111-111111111111"), Set.of(), employeeId))).isEqualTo(new TimeLogSearchScope.None());
	}

	@ParameterizedTest
	@MethodSource("actors")
	void searchScopeAgreesWithViewPolicyForOwnAndOtherEmployees(final Actor actor) {
		final ViewTimeLogPolicy view = new ViewTimeLogPolicy();
		final TimeLogSearchScope scope = this.policy.scope(actor);
		for (final Long employeeId : new Long[] { 84L, 85L }) {
			final boolean visible = switch (scope) {
				case TimeLogSearchScope.All _ -> true;
				case TimeLogSearchScope.Employee employee -> employee.employeeId().equals(employeeId);
				case TimeLogSearchScope.None _ -> false;
			};
			assertThat(visible).as("actor %s viewing employee %s", actor, employeeId)
					.isEqualTo(view.allows(actor, employeeId.equals(actor.employeeId())));
		}
	}

	@Test
	void rejectsMissingActor() {
		assertThatThrownBy(() -> this.policy.scope(null)).isInstanceOf(NullPointerException.class);
	}

	private static Stream<Set<Role>> elevatedRoles() {
		return Stream.of(Set.of(Role.JANUS_USER), Set.of(Role.JANUS_ADMIN),
				Set.of(Role.JANUS_EMPLOYEE, Role.JANUS_USER), Set.of(Role.JANUS_EMPLOYEE, Role.JANUS_ADMIN),
				Set.of(Role.JANUS_USER, Role.JANUS_ADMIN), Set.of(Role.values()));
	}

	private static Stream<Arguments> actors() {
		return Stream.concat(elevatedRoles(), Stream.of(Set.<Role>of(), Set.of(Role.JANUS_EMPLOYEE)))
				.flatMap(roles -> Stream.of(null, -1L, 0L, 84L).map(employeeId -> Arguments.of(new Actor(java.util.UUID.fromString("11111111-1111-4111-8111-111111111111"), roles, employeeId))));
	}
}
