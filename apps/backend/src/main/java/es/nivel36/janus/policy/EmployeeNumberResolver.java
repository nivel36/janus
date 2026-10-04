/*
 * Copyright 2026 Abel Ferrer Jiménez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package es.nivel36.janus.policy;

import java.util.Objects;
import java.util.OptionalLong;

import org.springframework.stereotype.Component;

import es.nivel36.janus.security.Actor;
import es.nivel36.janus.service.ResourceNotFoundException;
import es.nivel36.janus.service.employee.EmployeeService;

/**
 * Resolves employee-number filters consistently for authorization and searches.
 */
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
			return Objects.equals(actor.employeeId(), this.requireEmployeeId(employeeNumber));
		} catch (final ResourceNotFoundException exception) {
			return false;
		}
	}

	/**
	 * Resolves an identifier, preserving resource-not-found errors for the caller.
	 */
	public long requireEmployeeId(final String employeeNumber) {
		return this.employees.findEmployeeByEmployeeNumber(employeeNumber).getId();
	}

	/** Missing references are data absence; technical failures still propagate. */
	public OptionalLong employeeId(final String employeeNumber) {
		if (employeeNumber == null) {
			return OptionalLong.empty();
		}
		try {
			return OptionalLong.of(this.requireEmployeeId(employeeNumber));
		} catch (final ResourceNotFoundException exception) {
			return OptionalLong.empty();
		}
	}

	/**
	 * Builds filter facts without looking up references for unrestricted callers.
	 */
	public EmployeeSearchPolicy.Context searchContext(
			final Actor actor,
			final String requested,
			final boolean restricted) {
		return new EmployeeSearchPolicy.Context(requested != null, restricted && this.owns(actor, requested));
	}

	/**
	 * Supplies the linked employee only when an authorized request has no filter.
	 */
	public String effectiveNumber(final Actor actor, final String requested, final boolean restricted) {
		return requested == null && restricted && actor.employeeId() != null
				? this.employees.findEmployeeById(actor.employeeId()).getEmployeeNumber()
				: requested;
	}
}
