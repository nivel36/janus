/*
 * Copyright 2026 Abel Ferrer Jiménez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */
package es.nivel36.janus.security;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import es.nivel36.janus.service.appuser.Role;

/** Authorization policy for automatic application-user provisioning. */
@Component
public class AppUserProvisioningPolicy {

	private static final String ROLE_PREFIX = "ROLE_";
	private static final Set<String> JANUS_AUTHORITIES = Arrays.stream(Role.values())
			.map(role -> ROLE_PREFIX + role.name()).collect(Collectors.toUnmodifiableSet());

	/**
	 * Allows provisioning only for a validated resource-server JWT carrying at
	 * least one authority corresponding to a role supported by Janus.
	 */
	public boolean canProvision(final Authentication authentication) {
		return authentication instanceof JwtAuthenticationToken && authentication.isAuthenticated() && authentication
				.getAuthorities().stream().anyMatch(authority -> JANUS_AUTHORITIES.contains(authority.getAuthority()));
	}
}
