/*
 * Copyright 2026 Abel Ferrer Jiménez
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package es.nivel36.janus.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import es.nivel36.janus.service.appuser.Role;

class ActorTest {

	@Test
	void shouldDefensivelyCopyRoles() {
		final Set<Role> roles = new HashSet<>(Set.of(Role.JANUS_USER));
		final Actor actor = new Actor(7L, roles, null);

		roles.clear();

		assertThat(actor.roles()).containsExactly(Role.JANUS_USER);
		assertThatThrownBy(() -> actor.roles().clear()).isInstanceOf(UnsupportedOperationException.class);
	}
}
