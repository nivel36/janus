/*
 * Copyright 2026 Abel Ferrer Jiménez
 * Licensed under the Apache License, Version 2.0
 */
package es.nivel36.janus.config;

import java.util.Objects;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import es.nivel36.janus.service.ResourceNotFoundException;
import es.nivel36.janus.service.employee.Employee;
import es.nivel36.janus.service.employee.EmployeeService;

/** Resolves the provisioned employee from the immutable OIDC subject. */
@Component
public class AuthenticatedEmployee {

	private final EmployeeService employeeService;

	public AuthenticatedEmployee(final EmployeeService employeeService) {
		this.employeeService = Objects.requireNonNull(employeeService, "employeeService can't be null");
	}

	public Employee resolve(final Authentication authentication) {
		Objects.requireNonNull(authentication, "authentication can't be null");
		try {
			return this.employeeService.findEmployeeByKeycloakSubject(authentication.getName());
		} catch (ResourceNotFoundException exception) {
			throw new AccessDeniedException("The authenticated account has no employee assigned");
		}
	}

	/** Resolves the caller before the requested resource to avoid disclosing its existence. */
	public Employee assertOwnsEmail(final Authentication authentication, final String requestedEmail) {
		final Employee authenticated = this.resolve(authentication);
		final Employee requested = this.employeeService.findEmployeeByEmail(requestedEmail);
		if (!Objects.equals(authenticated.getId(), requested.getId())) {
			throw new AccessDeniedException("Employees can only access their own resources");
		}
		return authenticated;
	}
}
