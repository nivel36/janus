/*
 * Copyright 2026 Abel Ferrer Jiménez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package es.nivel36.janus.policy;

import es.nivel36.janus.service.ResourceNotFoundException;

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

	/**
	 * Returns a normalized email filter, preserving an absent filter.
	 *
	 * @param  email                    the contact email to normalize, or
	 *                                  {@code null}
	 * @return                          the trimmed, lowercase email, or
	 *                                  {@code null} for absent input
	 * @throws IllegalArgumentException if the email is blank or exceeds the
	 *                                  normalized length limit
	 * @see                             EmailAddresses#canonicalize(String)
	 */
	public String canonicalize(final String email) {
		return email == null ? null : EmailAddresses.canonicalize(email);
	}

	/**
	 * Returns whether the supplied email identifies the actor's linked employee.
	 *
	 * @param  actor                    the actor whose employee association is
	 *                                  compared
	 * @param  email                    the employee email to normalize before
	 *                                  lookup, or {@code null}
	 * @return                          {@code false} for an absent email, employee
	 *                                  link or matching employee; otherwise whether
	 *                                  the persistent identifiers match
	 * @throws IllegalArgumentException if a supplied email cannot be normalized
	 */
	public boolean owns(final Actor actor, final String email) {
		if (email == null || actor.employeeId() == null) {
			return false;
		}
		return this.employees.findEmployeeByEmail(this.canonicalize(email))
				.map(employee -> Objects.equals(actor.employeeId(), employee.getId())).orElse(false);
	}

	/**
	 * Returns the normalized email filter for the authorized employee scope.
	 *
	 * @param  actor                     the authorized caller whose employee is
	 *                                   used when restricted
	 * @param  requested                 the requested email filter, or {@code null}
	 * @param  restricted                whether the actor's linked employee must
	 *                                   determine the filter
	 * @return                           the linked employee's normalized email when
	 *                                   restricted, otherwise the normalized
	 *                                   requested filter
	 * @throws ResourceNotFoundException if the required linked employee is absent
	 */
	public String effectiveEmail(final Actor actor, final String requested, final boolean restricted) {
		return restricted ? this.canonicalize(this.employees.findEmployeeById(actor.employeeId()).getEmail())
				: this.canonicalize(requested);
	}
}
