/*
 * Copyright 2026 Abel Ferrer Jiménez Licensed under the Apache License, Version
 * 2.0 (the "License");
 */
package es.nivel36.janus.policy.timelog;

import java.util.Objects;

import es.nivel36.janus.policy.EmployeeAccessPolicy;
import es.nivel36.janus.policy.EmployeeSearchPolicy;
import es.nivel36.janus.policy.Policy;
import es.nivel36.janus.security.Actor;
import es.nivel36.janus.service.timelog.TimeLogSearchScope;

/** Separates permission to execute a search from the scope of visible rows. */
public final class SearchTimeLogPolicy implements Policy<EmployeeSearchPolicy.Context> {

	private final EmployeeSearchPolicy search = new EmployeeSearchPolicy();

	@Override
	public boolean allows(final Actor actor, final EmployeeSearchPolicy.Context context) {
		return this.search.allows(actor, context);
	}

	private final ViewTimeLogPolicy view = new ViewTimeLogPolicy();

	/**
	 * Returns the row visibility scope for a provisioned actor.
	 *
	 * @param  actor                the actor whose roles and employee link define
	 *                              visibility; must not be {@code null}
	 * @return                      all rows for elevated callers, the linked
	 *                              employee's rows for a restricted employee with a
	 *                              positive identifier, or no rows otherwise
	 * @throws NullPointerException if {@code actor} is {@code null}
	 */
	public TimeLogSearchScope scope(final Actor actor) {
		Objects.requireNonNull(actor, "actor can't be null");
		if (this.view.allows(actor, new EmployeeAccessPolicy.Context(false))) {
			return new TimeLogSearchScope.All();
		}
		if (this.view.allows(actor, new EmployeeAccessPolicy.Context(true)) && actor.employeeId() != null
				&& actor.employeeId() > 0) {
			return new TimeLogSearchScope.Employee(actor.employeeId());
		}
		return new TimeLogSearchScope.None();
	}
}
