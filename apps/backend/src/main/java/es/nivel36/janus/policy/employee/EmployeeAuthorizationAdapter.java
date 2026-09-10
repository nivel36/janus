/*
 * Copyright 2026 Abel Ferrer Jiménez
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package es.nivel36.janus.policy.employee;

import java.util.Objects;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import es.nivel36.janus.security.Actor;
import es.nivel36.janus.security.ActorResolver;
import es.nivel36.janus.service.employee.Employee;
import es.nivel36.janus.service.employee.EmployeeService;

/** Spring method-security adapter for employee policies. */
@Component("employeeAuthorization")
public class EmployeeAuthorizationAdapter {

	private final ActorResolver actorResolver;
	private final EmployeeService employeeService;
	private final ViewEmployeePolicy viewPolicy = new ViewEmployeePolicy();
	private final UpdateEmployeePolicy updatePolicy = new UpdateEmployeePolicy();
	private final CreateEmployeePolicy createPolicy = new CreateEmployeePolicy();
	private final DeleteEmployeePolicy deletePolicy = new DeleteEmployeePolicy();

	public EmployeeAuthorizationAdapter(final ActorResolver actorResolver, final EmployeeService employeeService) {
		this.actorResolver = Objects.requireNonNull(actorResolver, "actorResolver can't be null");
		this.employeeService = Objects.requireNonNull(employeeService, "employeeService can't be null");
	}

	public boolean canView(final Authentication authentication, final String employeeEmail) {
		final Actor actor = this.actorResolver.resolve(authentication);
		return this.viewPolicy.allows(actor, this.employeeId(employeeEmail));
	}

	public boolean canCreate(final Authentication authentication) {
		final Actor actor = this.actorResolver.resolve(authentication);
		return this.createPolicy.allows(actor, null);
	}

	public boolean canUpdate(final Authentication authentication, final String employeeEmail) {
		final Actor actor = this.actorResolver.resolve(authentication);
		return this.updatePolicy.allows(actor, this.employeeId(employeeEmail));
	}

	public boolean canDelete(final Authentication authentication) {
		final Actor actor = this.actorResolver.resolve(authentication);
		return this.deletePolicy.allows(actor, null);
	}

	private long employeeId(final String employeeEmail) {
		final Employee employee = this.employeeService.findEmployeeByEmail(employeeEmail);
		if(employee == null) {
			throw new AccessDeniedException("The employee's email is invalid");
		}
		return employee.getId();
		
	}
}
