/*
 * Copyright 2026 Abel Ferrer Jiménez
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package es.nivel36.janus.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import es.nivel36.janus.service.appuser.AppUser;
import es.nivel36.janus.service.appuser.AppUserService;
import es.nivel36.janus.service.appuser.Role;
import es.nivel36.janus.service.employee.Employee;

class ActorResolverTest {

	private static final String SUBJECT = "stable-provider-subject";

	@Test
	void shouldResolveJwtIdentityRolesAndInternalAttributes() {
		final AppUserService appUserService = mock(AppUserService.class);
		final AppUser appUser = mock(AppUser.class);
		final Employee employee = mock(Employee.class);
		when(appUserService.findAppUserByKeycloakSubject(SUBJECT)).thenReturn(appUser);
		when(appUser.getId()).thenReturn(42L);
		when(appUser.getEmployee()).thenReturn(employee);
		when(employee.getId()).thenReturn(84L);
		final JwtAuthenticationToken authentication = jwtAuthentication(List.of(
				new SimpleGrantedAuthority("ROLE_JANUS_ADMIN"), new SimpleGrantedAuthority("ROLE_UNKNOWN"),
				new SimpleGrantedAuthority("SCOPE_openid")));

		final Actor actor = new ActorResolver(appUserService).resolve(authentication);

		assertThat(actor.id()).isEqualTo(42L);
		assertThat(actor.employeeId()).isEqualTo(84L);
		assertThat(actor.roles()).containsExactly(Role.JANUS_ADMIN);
		verify(appUserService).findAppUserByKeycloakSubject(SUBJECT);
	}

	@Test
	void shouldResolveProvisionedActorWithoutEmployeeOrRoles() {
		final AppUserService appUserService = mock(AppUserService.class);
		final AppUser appUser = mock(AppUser.class);
		when(appUserService.findAppUserByKeycloakSubject(SUBJECT)).thenReturn(appUser);
		when(appUser.getId()).thenReturn(12L);

		final Actor actor = new ActorResolver(appUserService).resolve(jwtAuthentication(List.of()));

		assertThat(actor.roles()).isEmpty();
		assertThat(actor.employeeId()).isNull();
	}

	@Test
	void shouldPropagateControlledDenialForUnprovisionedSubject() {
		final AppUserService appUserService = mock(AppUserService.class);
		when(appUserService.findAppUserByKeycloakSubject(SUBJECT))
				.thenThrow(new AccessDeniedException("not provisioned"));

		assertThatThrownBy(() -> new ActorResolver(appUserService).resolve(jwtAuthentication(List.of())))
				.isInstanceOf(AccessDeniedException.class);
	}

	@Test
	void shouldResolveUnprovisionedElevatedIdentityFromTrustedAuthorities() {
		final AppUserService appUserService = mock(AppUserService.class);
		when(appUserService.findAppUserByKeycloakSubject(SUBJECT))
				.thenThrow(new AccessDeniedException("not provisioned"));

		final Actor actor = new ActorResolver(appUserService).resolve(jwtAuthentication(
				List.of(new SimpleGrantedAuthority("ROLE_JANUS_ADMIN"))));

		assertThat(actor.id()).isNull();
		assertThat(actor.employeeId()).isNull();
		assertThat(actor.roles()).containsExactly(Role.JANUS_ADMIN);
	}

	@Test
	void shouldRejectAuthenticationTypesNotIssuedByTheResourceServer() {
		final TestingAuthenticationToken authentication = new TestingAuthenticationToken("client-value", "password",
				"ROLE_JANUS_ADMIN");

		assertThatThrownBy(() -> new ActorResolver(mock(AppUserService.class)).resolve(authentication))
				.isInstanceOf(AccessDeniedException.class);
	}

	@Test
	void shouldRejectUnauthenticatedJwt() {
		final JwtAuthenticationToken authentication = jwtAuthentication(List.of());
		authentication.setAuthenticated(false);

		assertThatThrownBy(() -> new ActorResolver(mock(AppUserService.class)).resolve(authentication))
				.isInstanceOf(AccessDeniedException.class);
	}

	private static JwtAuthenticationToken jwtAuthentication(
			final List<SimpleGrantedAuthority> authorities) {
		final Instant now = Instant.now();
		final Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject(SUBJECT).issuedAt(now)
				.expiresAt(now.plusSeconds(300)).build();
		return new JwtAuthenticationToken(jwt, authorities);
	}
}
