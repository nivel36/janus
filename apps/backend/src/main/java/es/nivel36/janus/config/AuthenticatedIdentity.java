/*
 * Copyright 2026 Abel Ferrer Jiménez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package es.nivel36.janus.config;

import java.util.Locale;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Defines the link between an identity-provider account and Janus' email-based
 * {@code AppUser}/{@code Employee} records.
 *
 * <p>The current domain model uses email as its natural identifier. Consequently,
 * only the provider's verified {@code email} claim is accepted; display-oriented
 * claims such as {@code preferred_username} are deliberately ignored. Email
 * comparisons are normalized here so every authorization decision has identical
 * semantics.</p>
 */
@Component
public final class AuthenticatedIdentity {

	public static String email(final Authentication authentication) {
		if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
			throw new BadCredentialsException("A JWT authentication is required");
		}
		final String email = jwtAuthentication.getToken().getClaimAsString("email");
		final Boolean verified = jwtAuthentication.getToken().getClaim("email_verified");
		if (!StringUtils.hasText(email) || !Boolean.TRUE.equals(verified)) {
			throw new BadCredentialsException("A verified email claim is required");
		}
		return normalizeEmail(email);
	}

	public static boolean matchesEmail(final Authentication authentication, final String candidate) {
		return email(authentication).equals(normalizeEmail(candidate));
	}

	public static String normalizeEmail(final String email) {
		if (!StringUtils.hasText(email)) {
			throw new IllegalArgumentException("email cannot be null or blank");
		}
		return email.trim().toLowerCase(Locale.ROOT);
	}
}
