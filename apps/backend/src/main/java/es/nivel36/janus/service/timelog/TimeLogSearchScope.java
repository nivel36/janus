/*
 * Copyright 2026 Abel Ferrer Jiménez
 * Licensed under the Apache License, Version 2.0 (the "License");
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

	/** Only time logs belonging to this persistent employee are visible. */
	record Employee(Long employeeId) implements TimeLogSearchScope {
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
