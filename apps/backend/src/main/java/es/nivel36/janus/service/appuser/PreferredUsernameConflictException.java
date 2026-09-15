/* Copyright 2026 Abel Ferrer Jiménez. Licensed under the Apache License, Version 2.0. */
package es.nivel36.janus.service.appuser;

/** Raised when first-access provisioning cannot use an already-owned username. */
public final class PreferredUsernameConflictException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public PreferredUsernameConflictException(final String username, final Throwable cause) {
		super("The preferred username '" + username
				+ "' belongs to another identity; an administrator must recover the existing account", cause);
	}
}
