/*
 * Copyright 2026 Abel Ferrer Jiménez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package es.nivel36.janus.util;

import java.util.Locale;

/** Utilities for Janus' canonical, case-insensitive email identifiers. */
public final class EmailAddresses {

	private EmailAddresses() {
	}

	public static String canonicalize(final String email) {
		return Strings.requireNonBlank(email, "email cannot be null or blank").trim().toLowerCase(Locale.ROOT);
	}
}
