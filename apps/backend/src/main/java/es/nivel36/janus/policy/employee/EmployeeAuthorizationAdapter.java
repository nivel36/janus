/*
 * Copyright 2026 Abel Ferrer Jiménez Licensed under the Apache License, Version
 * 2.0 (the "License");
 */
package es.nivel36.janus.policy.employee;

import java.util.Objects;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import es.nivel36.janus.policy.EmployeeAccessPolicy;
import es.nivel36.janus.policy.EmployeeNumberResolver;
import es.nivel36.janus.security.Actor;
import es.nivel36.janus.security.ActorResolver;
import es.nivel36.janus.service.ResourceNotFoundException;

/** Spring method-security adapter for employee policies. */
@Component("employeeAuthorization")
public class EmployeeAuthorizationAdapter {

	private final ActorResolver actorResolver;
	private final EmployeeNumberResolver employeeNumbers;
	private final ViewEmployeePolicy viewPolicy = new ViewEmployeePolicy();
	private final UpdateEmployeePolicy updatePolicy = new UpdateEmployeePolicy();
	private final CreateEmployeePolicy createPolicy = new CreateEmployeePolicy();
	private final DeleteEmployeePolicy deletePolicy = new DeleteEmployeePolicy();
	private final SearchEmployeePolicy searchPolicy = new SearchEmployeePolicy();

	public EmployeeAuthorizationAdapter(
		final ActorResolver actorResolver,
		final EmployeeNumberResolver employeeNumbers) {
		this.actorResolver = Objects.requireNonNull(actorResolver, "actorResolver can't be null");
		this.employeeNumbers = Objects.requireNonNull(employeeNumbers, "employeeNumbers can't be null");
	}

	public boolean canView(final Authentication authentication, final String employeeNumber) {
		final Actor actor = this.actorResolver.resolve(authentication);
		return this.viewPolicy.allows(actor, this.employeeId(actor, employeeNumber));
	}

	public boolean canSearch(final Authentication authentication) {
		return this.searchPolicy.allows(this.actorResolver.resolve(authentication), null);
	}

	public boolean canCreate(final Authentication authentication) {
		final Actor actor = this.actorResolver.resolve(authentication);
		return this.createPolicy.allows(actor, null);
	}

	public boolean canUpdate(final Authentication authentication, final String employeeNumber) {
		final Actor actor = this.actorResolver.resolve(authentication);
		return this.updatePolicy.allows(actor, this.employeeId(actor, employeeNumber));
	}

	public boolean canDelete(final Authentication authentication) {
		final Actor actor = this.actorResolver.resolve(authentication);
		return this.deletePolicy.allows(actor, null);
	}

	private long employeeId(final Actor actor, final String employeeNumber) {
		try {
			return this.employeeNumbers.requireEmployeeId(employeeNumber);
		} catch (final ResourceNotFoundException exception) {
			if (EmployeeAccessPolicy.hasElevatedAccess(actor)) {
				throw exception;
			}
			throw new AccessDeniedException("Employees can only access their own resources");
		}
	}
}
