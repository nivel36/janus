/*
 * Copyright 2026 Abel Ferrer Jiménez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package es.nivel36.janus.security;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import es.nivel36.janus.service.appuser.AppUser;
import es.nivel36.janus.service.appuser.AppUserService;
import es.nivel36.janus.service.appuser.Role;

/** Converts the trusted Spring Security authentication into a domain actor. */
@Component
public final class ActorResolver {

	private static final String ROLE_PREFIX = "ROLE_";

	private final AppUserService appUserService;

	public ActorResolver(final AppUserService appUserService) {
		this.appUserService = Objects.requireNonNull(appUserService, "appUserService can't be null");
	}

	/**
	 * Resolves a validated bearer JWT to its provisioned internal identity.
	 * Unsupported, unauthenticated and unprovisioned identities are denied rather
	 * than represented as anonymous or accepted from operation input.
	 */
	@Transactional(readOnly = true)
	public Actor resolve(final Authentication authentication) {
		if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)
				|| !authentication.isAuthenticated()) {
			throw new AccessDeniedException("The authenticated identity is not supported");
		}

		final String subject = jwtAuthentication.getToken().getSubject();
		if (!StringUtils.hasText(subject) || !subject.equals(authentication.getName())) {
			throw new AccessDeniedException("The authenticated identity is invalid");
		}

		final AppUser appUser = this.appUserService.findAppUserByKeycloakSubject(subject);
		if (appUser.getId() == null) {
			throw new AccessDeniedException("The authenticated identity has not been provisioned");
		}
		final Long employeeId = appUser.getEmployee() == null ? null : appUser.getEmployee().getId();
		return new Actor(appUser.getId(), recognizedRoles(authentication), employeeId);
	}

	private static Set<Role> recognizedRoles(final Authentication authentication) {
		final EnumSet<Role> roles = EnumSet.noneOf(Role.class);
		authentication.getAuthorities().forEach(authority -> {
			for (final Role role : Role.values()) {
				if ((ROLE_PREFIX + role.name()).equals(authority.getAuthority())) {
					roles.add(role);
				}
			}
		});
		return roles;
	}
}
