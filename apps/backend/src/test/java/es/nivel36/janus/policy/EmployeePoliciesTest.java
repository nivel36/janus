/*
 * Copyright 2026 Abel Ferrer Jiménez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package es.nivel36.janus.policy;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;

import es.nivel36.janus.security.Actor;
import es.nivel36.janus.service.appuser.Role;

class EmployeePoliciesTest {

	private static final long OWN_EMPLOYEE_ID = 11L;
	private static final long OTHER_EMPLOYEE_ID = 12L;

	private final UpdateEmployeePolicy updatePolicy = new UpdateEmployeePolicy();
	private final ViewEmployeePolicy viewPolicy = new ViewEmployeePolicy();
	private final UpdateEmployeeRolesPolicy updateRolesPolicy = new UpdateEmployeeRolesPolicy();

	@Test
	void employeeCanViewAndUpdateOwnProfileOnly() {
		final Actor employee = actorWith(Role.JANUS_EMPLOYEE, OWN_EMPLOYEE_ID);

		assertThat(this.viewPolicy.allows(employee, OWN_EMPLOYEE_ID)).isTrue();
		assertThat(this.updatePolicy.allows(employee, OWN_EMPLOYEE_ID)).isTrue();
		assertThat(this.viewPolicy.allows(employee, OTHER_EMPLOYEE_ID)).isFalse();
		assertThat(this.updatePolicy.allows(employee, OTHER_EMPLOYEE_ID)).isFalse();
	}

	@Test
	void userCanViewAndUpdateEveryEmployee() {
		final Actor user = actorWith(Role.JANUS_USER, null);

		assertThat(this.viewPolicy.allows(user, OTHER_EMPLOYEE_ID)).isTrue();
		assertThat(this.updatePolicy.allows(user, OTHER_EMPLOYEE_ID)).isTrue();
	}

	@Test
	void administratorCanViewAndUpdateEveryEmployee() {
		final Actor administrator = actorWith(Role.JANUS_ADMIN, null);

		assertThat(this.viewPolicy.allows(administrator, OTHER_EMPLOYEE_ID)).isTrue();
		assertThat(this.updatePolicy.allows(administrator, OTHER_EMPLOYEE_ID)).isTrue();
	}

	@Test
	void ownershipWithoutAnEmployeeRoleDoesNotGrantAccess() {
		final Actor ownerWithoutRoles = new Actor(1L, Set.of(), OWN_EMPLOYEE_ID);

		assertThat(this.viewPolicy.allows(ownerWithoutRoles, OWN_EMPLOYEE_ID)).isFalse();
		assertThat(this.updatePolicy.allows(ownerWithoutRoles, OWN_EMPLOYEE_ID)).isFalse();
	}

	@Test
	void onlyAdministratorCanChangeEmployeeRoles() {
		assertThat(this.updateRolesPolicy.allows(actorWith(Role.JANUS_EMPLOYEE, OWN_EMPLOYEE_ID), OWN_EMPLOYEE_ID))
				.isFalse();
		assertThat(this.updateRolesPolicy.allows(actorWith(Role.JANUS_USER, OWN_EMPLOYEE_ID), OWN_EMPLOYEE_ID)).isFalse();
		assertThat(this.updateRolesPolicy.allows(actorWith(Role.JANUS_ADMIN, null), OTHER_EMPLOYEE_ID)).isTrue();
	}

	private static Actor actorWith(final Role role, final Long employeeId) {
		return new Actor(1L, Set.of(role), employeeId);
	}
}
