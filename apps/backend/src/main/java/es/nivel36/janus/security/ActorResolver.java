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

import es.nivel36.janus.service.ResourceNotFoundException;
import es.nivel36.janus.service.appuser.AppUser;
import es.nivel36.janus.service.appuser.AppUserService;
import es.nivel36.janus.service.appuser.Role;

/**
 * Converts trusted resource-server authentication into a persisted actor
 * snapshot. The JWT must already have passed issuer, audience and
 * email-verification checks; its subject must match Authentication.getName and
 * identify an existing profile. Resolution reads local identity and employee
 * data without provisioning, updating email or relinking an employee.
 * Unsupported or unprovisioned identities are denied with a generic message
 * rather than disclosing profile lookup details.
 */
@Component
public class ActorResolver {

	private static final String ROLE_PREFIX = "ROLE_";

	private final AppUserService appUserService;

	/**
	 * Creates a resolver backed by a nonnull profile service. Construction performs
	 * no authentication or database lookup.
	 *
	 * @param  appUserService       nonnull application-profile lookup service
	 * @throws NullPointerException if appUserService is null
	 */
	public ActorResolver(final AppUserService appUserService) {
		this.appUserService = Objects.requireNonNull(appUserService, "appUserService can't be null");
	}

	/**
	 * Resolves a validated JWT to an immutable authorization snapshot.
	 * <p>
	 * Authentication must be an authenticated JwtAuthenticationToken from the
	 * trusted resource server; its nonblank subject must match the principal name
	 * and satisfy the profile subject length limit. The profile must already exist
	 * with a persistence UUID.
	 * </p>
	 * <p>
	 * Returns the persisted UUID, optional employee database id and recognized
	 * Janus roles. Mutable token email, username and employeeNumber do not select
	 * the actor or replace its employee association. No profile is created or
	 * modified; unrecognized authorities are ignored.
	 * </p>
	 *
	 * @param  authentication           trusted resource-server authentication;
	 *                                  unsupported values including null are denied
	 * @return                          provisioned actor snapshot, possibly with no
	 *                                  employee or recognized roles
	 * @throws AccessDeniedException    if authentication is unsupported, invalid,
	 *                                  unauthenticated or unprovisioned
	 * @throws IllegalArgumentException if the subject exceeds the profile column
	 *                                  limit
	 */
	@Transactional(readOnly = true)
	public Actor resolve(final Authentication authentication) {
		if (!(authentication instanceof final JwtAuthenticationToken jwtAuthentication)
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
		} catch (final ResourceNotFoundException notFound) {
			throw new AccessDeniedException("The authenticated identity has not been provisioned");
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
