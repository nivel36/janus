/*
 * Copyright 2026 Abel Ferrer Jiménez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package es.nivel36.janus.util;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Immutable, canonical email value object.
 * <p>
 * Email addresses enter the application as strings, but their syntax,
 * normalization and decomposition belong to this type. The local name and
 * server are exposed separately while {@link #value()} provides the
 * representation used by APIs and persistence.
 * </p>
 */
public record Email(String name, String server) {

	public static final int MAX_LENGTH = 254;
	public static final String REGEXP = "^(?=.{1,254}$)([A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+)@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$";

	private static final Pattern EMAIL_PATTERN = Pattern.compile(REGEXP);

	/** Creates an email from already separated parts and canonicalizes them. */
	public Email {
		name = normalizePart(name, "email name cannot be null or blank");
		server = normalizePart(server, "email server cannot be null or blank");
		validate(name + "@" + server);
	}

	/** Parses, validates and canonicalizes an external email representation. */
	public static Email of(final String value) {
		final String canonical = Strings.requireNonBlank(value, "email cannot be null or blank").trim()
				.toLowerCase(Locale.ROOT);
		final Matcher matcher = EMAIL_PATTERN.matcher(canonical);
		if (!matcher.matches()) {
			throw new IllegalArgumentException("email must be a valid address of at most 254 characters");
		}
		return new Email(matcher.group(1), matcher.group(2));
	}

	/** Returns the canonical form suitable for transport and persistence. */
	public String value() {
		return this.name + "@" + this.server;
	}

	@Override
	public String toString() {
		return value();
	}

	private static String normalizePart(final String part, final String message) {
		return Strings.requireNonBlank(Objects.requireNonNull(part, message), message).trim().toLowerCase(Locale.ROOT);
	}

	private static void validate(final String value) {
		if (!EMAIL_PATTERN.matcher(value).matches()) {
			throw new IllegalArgumentException("email must be a valid address of at most 254 characters");
		}
	}
}
