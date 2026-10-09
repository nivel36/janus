/*
 * Copyright 2026 Abel Ferrer Jiménez Licensed under the Apache License, Version
 * 2.0 (the "License");
 */
package es.nivel36.janus.service.timelog;

import java.util.Objects;

/**
 * Authorized rows for a time-log search, independent of client search criteria
 * and permission to execute the search.
 */
public sealed interface TimeLogSearchScope {

	/** All time logs are visible. */
	record All() implements TimeLogSearchScope {
	}

	/**
	 * Visibility limited to the time logs of one persistent employee.
	 *
	 * @param employeeId the positive persistent employee identifier
	 */
	record Employee(Long employeeId) implements TimeLogSearchScope {
		/**
		 * Creates a visibility scope for a persistent employee.
		 *
		 * @param  employeeId               the positive persistent employee identifier
		 * @throws NullPointerException     if the identifier is {@code null}
		 * @throws IllegalArgumentException if the identifier is not positive
		 */
		public Employee {
			Objects.requireNonNull(employeeId, "employeeId can't be null");
			if (employeeId <= 0) {
				throw new IllegalArgumentException("employeeId must be a positive persistent identifier");
			}
		}
	}

	/** No time logs are visible. */
	record None() implements TimeLogSearchScope {
	}
}
