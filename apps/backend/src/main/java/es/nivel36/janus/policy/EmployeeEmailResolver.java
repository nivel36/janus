/*
 * Copyright 2026 Abel Ferrer Jiménez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package es.nivel36.janus.policy;

import java.util.Objects;

import org.springframework.stereotype.Component;

import es.nivel36.janus.security.Actor;
import es.nivel36.janus.service.employee.EmployeeService;
import es.nivel36.janus.util.EmailAddresses;

/**
 * Resolves API email parameters consistently for authorization and searches.
 */
@Component
public class EmployeeEmailResolver {

	private final EmployeeService employees;

	public EmployeeEmailResolver(final EmployeeService employees) {
		this.employees = Objects.requireNonNull(employees);
	}

	public String canonicalize(final String email) {
		return email == null ? null : EmailAddresses.canonicalize(email);
	}

	public boolean owns(final Actor actor, final String email) {
		if (email == null || actor.employeeId() == null) {
			return false;
		}
		return this.employees.findEmployeeByEmail(this.canonicalize(email))
				.map(employee -> Objects.equals(actor.employeeId(), employee.getId())).orElse(false);
	}

	public String effectiveEmail(final Actor actor, final String requested, final boolean restricted) {
		return restricted ? this.canonicalize(this.employees.findEmployeeById(actor.employeeId()).getEmail())
				: this.canonicalize(requested);
	}
}
