/*
 * Copyright 2026 Abel Ferrer Jiménez
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 */
package es.nivel36.janus.security;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import es.nivel36.janus.service.appuser.Role;

/**
 * Authorization gate for current-profile provisioning. Authentication must
 * already have been validated by the resource server; this policy checks
 * authentication type, authenticated status and recognized roles. It returns a
 * decision without looking up, creating or changing a local profile and does
 * not independently validate JWT claims.
 */
@Component
public class AppUserProvisioningPolicy {

	/**
	 * Creates the stateless gate for recognized Janus authorities. Construction
	 * does not authenticate a request or provision a profile.
	 */
	public AppUserProvisioningPolicy() {
	}

	private static final String ROLE_PREFIX = "ROLE_";
	private static final Set<String> JANUS_AUTHORITIES = Arrays.stream(Role.values())
			.map(role -> ROLE_PREFIX + role.name()).collect(Collectors.toUnmodifiableSet());

	/**
	 * Tests whether current-profile provisioning is permitted without side effects.
	 *
	 * @param  authentication validated resource-server authentication, or null
	 * @return                true only for an authenticated JWT with at least one
	 *                        recognized Janus authority; false for null,
	 *                        unsupported types or missing roles
	 */
	public boolean canProvision(final Authentication authentication) {
		return authentication instanceof JwtAuthenticationToken && authentication.isAuthenticated() && authentication
				.getAuthorities().stream().anyMatch(authority -> JANUS_AUTHORITIES.contains(authority.getAuthority()));
	}
}
