/*
 * Copyright 2026 Abel Ferrer Jiménez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package es.nivel36.janus.config;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import es.nivel36.janus.util.EmailAddresses;

/**
 * Extracts security-relevant identity claims from an authenticated JWT.
 *
 * <p>An {@code AppUser} is identified only by {@code (iss, sub)}. Email remains a
 * contact attribute and its use by Employee authorization is transitional until
 * AppUser and Employee have an explicit relationship.</p>
 */
@Component
public final class AuthenticatedIdentity {

	public static ExternalIdentity externalIdentity(final Authentication authentication) {
		if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
			throw new BadCredentialsException("A JWT authentication is required");
		}
		final String issuer = jwtAuthentication.getToken().getClaimAsString("iss");
		final String subject = jwtAuthentication.getToken().getSubject();
		if (!StringUtils.hasText(issuer) || !StringUtils.hasText(subject)) {
			throw new BadCredentialsException("Non-blank iss and sub claims are required");
		}
		return new ExternalIdentity(issuer.trim(), subject.trim());
	}

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
		return EmailAddresses.canonicalize(email);
	}
}
