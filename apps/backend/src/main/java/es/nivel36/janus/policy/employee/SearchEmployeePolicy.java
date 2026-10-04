/*
 * Copyright 2026 Abel Ferrer Jiménez Licensed under the Apache License, Version
 * 2.0 (the "License");
 */
package es.nivel36.janus.policy.employee;

import java.util.Objects;

import es.nivel36.janus.policy.EmployeeAccessPolicy;
import es.nivel36.janus.policy.Policy;
import es.nivel36.janus.security.Actor;

/** Prevents restricted employees from enumerating other employee identities. */
public final class SearchEmployeePolicy implements Policy<Void> {

	@Override
	public boolean allows(final Actor actor, final Void context) {
		Objects.requireNonNull(actor, "actor can't be null");
		return EmployeeAccessPolicy.hasElevatedAccess(actor);
	}
}
