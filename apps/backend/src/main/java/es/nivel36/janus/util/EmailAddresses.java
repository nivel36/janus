/*
 * Copyright 2026 Abel Ferrer Jiménez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package es.nivel36.janus.util;

import java.util.Locale;

/**
 * Normalizes contact email consistently without selecting or authorizing
 * identity. Inputs must be nonnull and nonblank. The result is trimmed and
 * lowercased with Locale.ROOT and limited to 254 characters; normalization
 * itself does not validate email syntax or uniqueness and does not change a
 * profile.
 */
public final class EmailAddresses {

	private EmailAddresses() {
	}

	/**
	 * Normalizes contact information without mutation or database access.
	 *
	 * @param  email                    nonnull, nonblank contact email
	 * @return                          trimmed, lowercase email using Locale.ROOT
	 * @throws NullPointerException     if email is null
	 * @throws IllegalArgumentException if email is blank or its normalized length
	 *                                  exceeds 254
	 */
	public static String canonicalize(final String email) {
		final String normalized = Strings.requireNonBlank(email, "email cannot be null or blank").trim()
				.toLowerCase(Locale.ROOT);
		if (normalized.length() > 254) {
			throw new IllegalArgumentException("email can't exceed 254 characters");
		}
		return normalized;
	}
}
