/*
 * Copyright 2026 Abel Ferrer Jiménez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package es.nivel36.janus.policy.timelog;

import java.util.Objects;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import es.nivel36.janus.security.Actor;
import es.nivel36.janus.security.ActorResolver;
import es.nivel36.janus.service.employee.Employee;
import es.nivel36.janus.service.employee.EmployeeService;

/** Spring method-security adapter for clock-out-without-clock-in policies. */
@Component("clockOutWithoutClockInEventAuthorization")
public final class ClockOutWithoutClockInEventAuthorizationAdapter {

	private final ActorResolver actorResolver;
	private final EmployeeService employeeService;
	private final ViewClockOutWithoutClockInEventPolicy viewPolicy = new ViewClockOutWithoutClockInEventPolicy();
	private final ResolveClockOutWithoutClockInEventPolicy resolvePolicy = new ResolveClockOutWithoutClockInEventPolicy();
	private final InvalidateClockOutWithoutClockInEventPolicy invalidatePolicy = new InvalidateClockOutWithoutClockInEventPolicy();

	public ClockOutWithoutClockInEventAuthorizationAdapter(final ActorResolver actorResolver,
			final EmployeeService employeeService) {
		this.actorResolver = Objects.requireNonNull(actorResolver, "actorResolver can't be null");
		this.employeeService = Objects.requireNonNull(employeeService, "employeeService can't be null");
	}

	public boolean canView(final Authentication authentication, final String employeeEmail) {
		final Actor actor = this.actorResolver.resolve(authentication);
		return this.viewPolicy.allows(actor, this.employeeId(employeeEmail));
	}

	public boolean canResolve(final Authentication authentication, final String employeeEmail) {
		final Actor actor = this.actorResolver.resolve(authentication);
		return this.resolvePolicy.allows(actor, this.employeeId(employeeEmail));
	}

	public boolean canInvalidate(final Authentication authentication, final String employeeEmail) {
		final Actor actor = this.actorResolver.resolve(authentication);
		return this.invalidatePolicy.allows(actor, this.employeeId(employeeEmail));
	}

	private long employeeId(final String employeeEmail) {
		final Employee employee = this.employeeService.findEmployeeByEmail(employeeEmail);
		if (employee == null) {
			throw new AccessDeniedException("The employee's email is invalid");
		}
		return employee.getId();
	}
}
