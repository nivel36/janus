/*
 * Copyright 2026 Abel Ferrer Jiménez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package es.nivel36.janus.policy.employee;

import java.util.Objects;

import es.nivel36.janus.policy.Policy;
import es.nivel36.janus.security.Actor;
import es.nivel36.janus.service.appuser.Role;

/** Authorizes access to an employee's profile. */
public final class ViewEmployeePolicy implements Policy<Long> {

	@Override
	public boolean allows(final Actor actor, final Long employeeId) {
		Objects.requireNonNull(actor, "actor can't be null");
		Objects.requireNonNull(employeeId, "employeeId can't be null");

		if (actor.hasRole(Role.JANUS_ADMIN) || actor.hasRole(Role.JANUS_USER)) {
			return true;
		}
		return actor.hasRole(Role.JANUS_EMPLOYEE) && employeeId.equals(actor.employeeId());
	}
}
