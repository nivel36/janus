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
 * lowercased with Locale.ROOT; normalization itself does not validate email
 * syntax, column length or uniqueness and does not change a profile.
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
	 * @throws IllegalArgumentException if email is blank
	 */
	public static String canonicalize(final String email) {
		return Strings.requireNonBlank(email, "email cannot be null or blank").trim().toLowerCase(Locale.ROOT);
	}
}
