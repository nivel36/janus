/*
 * Copyright 2026 Abel Ferrer Jiménez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package es.nivel36.janus.security;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import es.nivel36.janus.service.appuser.Role;

/**
 * Immutable authorization snapshot of a previously provisioned caller.
 * Construction requires a nonnull persistent profile UUID and a nonnull role
 * set without {@code null} elements. The optional employee identifier may be
 * {@code null}. The role set is defensively copied, so later changes to the
 * supplied set do not change this actor. Construction neither authenticates nor
 * provisions the caller.
 *
 * @param id         persistent application-profile UUID
 * @param roles      recognized provider roles, defensively copied to an
 *                   immutable set
 * @param employeeId persisted employee identifier, or {@code null} when
 *                   unlinked
 */
public record Actor(UUID id, Set<Role> roles, Long employeeId) {

	/**
	 * Creates an immutable snapshot without persistence effects.
	 *
	 * @param  id                   nonnull persistent profile UUID
	 * @param  roles                nonnull recognized role set without {@code null}
	 *                              elements
	 * @param  employeeId           optional employee identifier
	 * @throws NullPointerException if id, roles or any role element is {@code null}
	 */
	public Actor {
		Objects.requireNonNull(id, "id can't be null");
		roles = Set.copyOf(Objects.requireNonNull(roles, "roles can't be null"));
	}

	/**
	 * Tests role membership without changing the actor.
	 *
	 * @param  role                 nonnull recognized role to test
	 * @return                      {@code true} exactly when this snapshot contains
	 *                              the role
	 * @throws NullPointerException if role is {@code null}
	 */
	public boolean hasRole(final Role role) {
		return this.roles.contains(Objects.requireNonNull(role, "role can't be null"));
	}
}
