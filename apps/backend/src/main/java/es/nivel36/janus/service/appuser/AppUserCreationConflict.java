/*
 * Copyright 2026 Abel Ferrer Jiménez
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package es.nivel36.janus.service.appuser;

/** Identifies the unique key that rejected an automatic user insertion. */
final class AppUserCreationConflict extends RuntimeException {

	private static final long serialVersionUID = 1L;

	AppUserCreationConflict(final Throwable cause) {
		super(cause);
	}
}
