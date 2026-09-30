/*
 * Copyright 2026 Abel Ferrer Jiménez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package es.nivel36.janus.policy.timelog;

import java.util.Objects;
import java.util.OptionalLong;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import es.nivel36.janus.policy.EmployeeNumberResolver;
import es.nivel36.janus.security.Actor;
import es.nivel36.janus.security.ActorResolver;
import es.nivel36.janus.service.employee.EmployeeService;

/** Spring method-security adapter for clock-out-without-clock-in policies. */
@Component("clockOutWithoutClockInEventAuthorization")
public final class ClockOutWithoutClockInEventAuthorizationAdapter {

	private final ActorResolver actorResolver;
	private final EmployeeService employeeService;
	private final EmployeeNumberResolver employeeNumbers;
	private final ViewClockOutWithoutClockInEventPolicy viewPolicy = new ViewClockOutWithoutClockInEventPolicy();
	private final ResolveClockOutWithoutClockInEventPolicy resolvePolicy = new ResolveClockOutWithoutClockInEventPolicy();
	private final InvalidateClockOutWithoutClockInEventPolicy invalidatePolicy = new InvalidateClockOutWithoutClockInEventPolicy();

	public ClockOutWithoutClockInEventAuthorizationAdapter(final ActorResolver actorResolver,
			final EmployeeService employeeService, final EmployeeNumberResolver employeeNumbers) {
		this.actorResolver = Objects.requireNonNull(actorResolver, "actorResolver can't be null");
		this.employeeService = Objects.requireNonNull(employeeService, "employeeService can't be null");
		this.employeeNumbers = Objects.requireNonNull(employeeNumbers, "employeeNumbers can't be null");
	}

	public boolean canView(final Authentication authentication, final String employeeNumber) {
		final Actor actor = this.actorResolver.resolve(authentication);
		final OptionalLong employeeId = this.employeeNumbers.employeeId(employeeNumber);
		return employeeId.isPresent() && this.viewPolicy.allows(actor, employeeId.getAsLong());
	}

	public boolean canResolve(final Authentication authentication, final String employeeNumber) {
		final Actor actor = this.actorResolver.resolve(authentication);
		final OptionalLong employeeId = this.employeeNumbers.employeeId(employeeNumber);
		return employeeId.isPresent() && this.resolvePolicy.allows(actor, employeeId.getAsLong());
	}

	public boolean canInvalidate(final Authentication authentication, final String employeeNumber) {
		final Actor actor = this.actorResolver.resolve(authentication);
		final OptionalLong employeeId = this.employeeNumbers.employeeId(employeeNumber);
		return employeeId.isPresent() && this.invalidatePolicy.allows(actor, employeeId.getAsLong());
	}

	public boolean canViewByEmail(final Authentication authentication, final String email) {
		final Actor actor = this.actorResolver.resolve(authentication);
		final OptionalLong employeeId = this.employeeIdByEmail(email);
		return employeeId.isPresent() && this.viewPolicy.allows(actor, employeeId.getAsLong());
	}

	public boolean canResolveByEmail(final Authentication authentication, final String email) {
		final Actor actor = this.actorResolver.resolve(authentication);
		final OptionalLong employeeId = this.employeeIdByEmail(email);
		return employeeId.isPresent() && this.resolvePolicy.allows(actor, employeeId.getAsLong());
	}

	public boolean canInvalidateByEmail(final Authentication authentication, final String email) {
		final Actor actor = this.actorResolver.resolve(authentication);
		final OptionalLong employeeId = this.employeeIdByEmail(email);
		return employeeId.isPresent() && this.invalidatePolicy.allows(actor, employeeId.getAsLong());
	}

	private OptionalLong employeeIdByEmail(final String email) {
		return this.employeeService.findEmployeeByEmail(email).map(employee -> OptionalLong.of(employee.getId()))
				.orElseGet(OptionalLong::empty);
	}
}
