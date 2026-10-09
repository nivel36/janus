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
	 * Returns the persistent identifier of an employee with the exact number.
	 *
	 * @param  employeeNumber            the exact employee number
	 * @return                           the matching employee's persistent
	 *                                   identifier
	 * @throws ResourceNotFoundException if the employee is absent
	 */
	public long requireEmployeeId(final String employeeNumber) {
		return this.employees.findEmployeeByEmployeeNumber(employeeNumber).getId();
	}

	/**
	 * Returns an employee identifier if the supplied number identifies an employee.
	 * <p>
	 * Missing references produce an empty optional; other lookup failures
	 * propagate.
	 *
	 * @param  employeeNumber the exact employee number, or {@code null}
	 * @return                the employee identifier, or an empty optional for
	 *                        absent input or employee
	 */
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
	 * Returns filter-presence and ownership facts for an employee search.
	 * <p>
	 * Ownership is checked only for restricted callers.
	 *
	 * @param  actor      the caller whose employee association is considered
	 * @param  requested  the requested employee number, or {@code null} for no
	 *                    filter
	 * @param  restricted whether ownership must constrain the caller's search
	 * @return            the filter facts used by {@link EmployeeSearchPolicy}
	 */
	public EmployeeSearchPolicy.Context searchContext(
			final Actor actor,
			final String requested,
			final boolean restricted) {
		return new EmployeeSearchPolicy.Context(requested != null, restricted && this.owns(actor, requested));
	}

	/**
	 * Returns the employee filter to apply after the caller has authorized the
	 * request.
	 * <p>
	 * An omitted filter is replaced by the actor's employee number only for a
	 * restricted actor with an employee link. Explicit filters are returned
	 * unchanged.
	 *
	 * @param  actor                     the authorized caller
	 * @param  requested                 the explicit employee number, or
	 *                                   {@code null}
	 * @param  restricted                whether the caller is restricted to their
	 *                                   own employee
	 * @return                           the explicit, derived or absent employee
	 *                                   filter
	 * @throws ResourceNotFoundException if the linked employee needed for
	 *                                   derivation is absent
	 */
	public String effectiveNumber(final Actor actor, final String requested, final boolean restricted) {
		return requested == null && restricted && actor.employeeId() != null
				? this.employees.findEmployeeById(actor.employeeId()).getEmployeeNumber()
				: requested;
	}
}
