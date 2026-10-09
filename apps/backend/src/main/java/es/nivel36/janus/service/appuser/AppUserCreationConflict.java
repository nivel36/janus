/*
 * Copyright 2026 Abel Ferrer Jiménez Licensed under the Apache License, Version
 * 2.0 (the "License");
 */
package es.nivel36.janus.service.appuser;

/**
 * Internal signal that a first-access insert must be reconciled by the service.
 * It carries the employee-claim or integrity-violation cause. When thrown by
 * the transactional creator, that insert is rolled back; this exception itself
 * does not select a winning profile or change any association.
 */
final class AppUserCreationConflict extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * Records the rejected insertion cause for service-level reconciliation.
	 * Construction itself performs no rollback or profile lookup.
	 *
	 * @param cause employee-claim or database-integrity failure; may be
	 *              {@code null}
	 */
	AppUserCreationConflict(final Throwable cause) {
		super(cause);
	}
}
