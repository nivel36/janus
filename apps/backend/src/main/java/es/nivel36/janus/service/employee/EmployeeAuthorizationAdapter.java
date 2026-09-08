/*
 * Copyright 2026 Abel Ferrer Jiménez
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package es.nivel36.janus.service.employee;

import java.util.Objects;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import es.nivel36.janus.policy.UpdateEmployeePolicy;
import es.nivel36.janus.policy.ViewEmployeePolicy;
import es.nivel36.janus.security.Actor;
import es.nivel36.janus.security.ActorResolver;
import es.nivel36.janus.util.EmailAddresses;

/** Spring method-security adapter for employee policies. */
@Component("employeeAuthorization")
public class EmployeeAuthorizationAdapter {

	private static final long UNKNOWN_EMPLOYEE_ID = 0L;

	private final ActorResolver actorResolver;
	private final EmployeeRepository employeeRepository;
	private final ViewEmployeePolicy viewPolicy = new ViewEmployeePolicy();
	private final UpdateEmployeePolicy updatePolicy = new UpdateEmployeePolicy();

	public EmployeeAuthorizationAdapter(final ActorResolver actorResolver, final EmployeeRepository employeeRepository) {
		this.actorResolver = Objects.requireNonNull(actorResolver, "actorResolver can't be null");
		this.employeeRepository = Objects.requireNonNull(employeeRepository, "employeeRepository can't be null");
	}

	public boolean canView(final Authentication authentication, final String employeeEmail) {
		final Actor actor = this.actorResolver.resolve(authentication);
		return this.viewPolicy.allows(actor, this.employeeId(employeeEmail));
	}

	public boolean canUpdate(final Authentication authentication, final String employeeEmail) {
		final Actor actor = this.actorResolver.resolve(authentication);
		return this.updatePolicy.allows(actor, this.employeeId(employeeEmail));
	}

	private long employeeId(final String employeeEmail) {
		final String canonicalEmail = EmailAddresses.canonicalize(employeeEmail);
		return this.employeeRepository.findIdByEmail(canonicalEmail).orElse(UNKNOWN_EMPLOYEE_ID);
	}
}
