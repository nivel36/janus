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
public class ActorResolver {

	private static final String ROLE_PREFIX = "ROLE_";

	private final AppUserService appUserService;

	public ActorResolver(final AppUserService appUserService) {
		this.appUserService = Objects.requireNonNull(appUserService, "appUserService can't be null");
	}

	/**
	 * Resolves a validated bearer JWT to an application actor. Employee-only
	 * identities must be provisioned so ownership can be established. Trusted user
	 * and administrator authorities retain their role-only access when the caller
	 * does not yet have a local account.
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

		final Set<Role> roles = recognizedRoles(authentication);
		final AppUser appUser;
		try {
			appUser = this.appUserService.findAppUserByKeycloakSubject(subject);
		} catch (final AccessDeniedException ex) {
			if (roles.contains(Role.JANUS_ADMIN) || roles.contains(Role.JANUS_USER)) {
				return new Actor(null, roles, null);
			}
			throw ex;
		}
		if (appUser.getId() == null) {
			throw new AccessDeniedException("The authenticated identity has not been provisioned");
		}
		final Long employeeId = appUser.getEmployee() == null ? null : appUser.getEmployee().getId();
		return new Actor(appUser.getId(), roles, employeeId);
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
