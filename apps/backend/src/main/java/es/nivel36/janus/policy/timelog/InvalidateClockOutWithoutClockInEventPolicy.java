/*
 * Copyright 2026 Abel Ferrer Jiménez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package es.nivel36.janus.policy.timelog;

import java.util.Objects;

import es.nivel36.janus.policy.Policy;
import es.nivel36.janus.security.Actor;
import es.nivel36.janus.service.appuser.Role;

/** Authorizes invalidating a clock-out-without-clock-in incident. */
public final class InvalidateClockOutWithoutClockInEventPolicy implements Policy<Long> {

	@Override
	public boolean allows(final Actor actor, final Long employeeId) {
		Objects.requireNonNull(actor, "actor can't be null");
		Objects.requireNonNull(employeeId, "employeeId can't be null");
		return actor.hasRole(Role.JANUS_ADMIN) || actor.hasRole(Role.JANUS_USER)
				|| actor.hasRole(Role.JANUS_EMPLOYEE) && employeeId.equals(actor.employeeId());
	}
}
