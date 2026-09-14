/*
 * Copyright 2026 Abel Ferrer Jiménez
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package es.nivel36.janus.policy.timelog;

import java.util.Objects;

import es.nivel36.janus.security.Actor;
import es.nivel36.janus.service.timelog.TimeLogSearchScope;

/** Determines visible time logs without granting permission to execute a search. */
public final class SearchTimeLogPolicy {

	private final ViewTimeLogPolicy view = new ViewTimeLogPolicy();

	public TimeLogSearchScope scope(final Actor actor) {
		Objects.requireNonNull(actor, "actor can't be null");
		if (this.view.allows(actor, false)) {
			return new TimeLogSearchScope.All();
		}
		if (this.view.allows(actor, true) && actor.employeeId() != null && actor.employeeId() > 0) {
			return new TimeLogSearchScope.Employee(actor.employeeId());
		}
		return new TimeLogSearchScope.None();
	}
}
