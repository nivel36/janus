/*
 * Copyright 2026 Abel Ferrer Jiménez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package es.nivel36.janus.security;

import java.util.Objects;
import java.util.Set;

import es.nivel36.janus.service.appuser.Role;

/**
 * Application-owned representation of an authenticated caller.
 *
 * <p>This type deliberately has no dependency on Spring Security. Policy code can
 * use the persistent application-user identifier, the optional associated
 * employee identifier and the roles granted to the caller.</p>
 *
 * @param id persistent identifier of the provisioned application user
 * @param roles recognized Janus roles granted by the identity provider
 * @param employeeId persistent employee identifier, or {@code null} when the user
 *                   is not associated with an employee
 */
public record Actor(Long id, Set<Role> roles, Long employeeId) {

	public Actor {
		Objects.requireNonNull(id, "id can't be null");
		if (id <= 0) {
			throw new IllegalArgumentException("id must be a positive persistent identifier");
		}
		roles = Set.copyOf(Objects.requireNonNull(roles, "roles can't be null"));
	}

	public boolean hasRole(final Role role) {
		return this.roles.contains(Objects.requireNonNull(role, "role can't be null"));
	}
}
