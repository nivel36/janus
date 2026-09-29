/*
 * Copyright 2026 Abel Ferrer Jiménez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package es.nivel36.janus.policy;

import java.util.Objects;

import org.springframework.stereotype.Component;

import es.nivel36.janus.security.Actor;
import es.nivel36.janus.service.ResourceNotFoundException;
import es.nivel36.janus.service.employee.EmployeeService;

/** Resolves employee-number filters consistently for authorization and searches. */
@Component
public class EmployeeNumberResolver {

	private final EmployeeService employees;

	public EmployeeNumberResolver(final EmployeeService employees) {
		this.employees = Objects.requireNonNull(employees);
	}

	public boolean owns(final Actor actor, final String employeeNumber) {
		if (employeeNumber == null || actor.employeeId() == null) {
			return false;
		}
		try {
			return Objects.equals(actor.employeeId(), this.employees.findEmployeeByEmployeeNumber(employeeNumber).getId());
		} catch (final ResourceNotFoundException exception) {
			return false;
		}
	}

	public String effectiveNumber(final Actor actor, final String requested, final boolean restricted) {
		return restricted && actor.employeeId() != null
				? this.employees.findEmployeeById(actor.employeeId()).getEmployeeNumber()
				: requested;
	}
}
