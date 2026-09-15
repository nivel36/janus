/* Copyright 2026 Abel Ferrer Jiménez. Licensed under the Apache License, Version 2.0. */
package es.nivel36.janus.service.appuser;

/** Raised when an identity subject is already linked to another local account. */
public final class KeycloakSubjectConflictException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public KeycloakSubjectConflictException(final String keycloakSubject) {
		super("Keycloak subject '" + keycloakSubject + "' is already linked to another application user");
	}
}
