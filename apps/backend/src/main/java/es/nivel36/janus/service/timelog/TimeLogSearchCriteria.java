/*
 * Copyright 2026 Abel Ferrer Jiménez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package es.nivel36.janus.service.timelog;

import java.time.Instant;

/**
 * Optional client filters, independent of the authorized search scope.
 * The entry-time range includes {@code fromInstant} and excludes {@code toInstant}.
 */
public record TimeLogSearchCriteria(String employeeEmail, Instant fromInstant, Instant toInstant) {

	public TimeLogSearchCriteria {
		if ((fromInstant == null) != (toInstant == null)) {
			throw new IllegalArgumentException("Both fromInstant and toInstant must be provided together or omitted.");
		}
		if (fromInstant != null && fromInstant.isAfter(toInstant)) {
			throw new IllegalArgumentException("toInstant must be after fromInstant");
		}
	}
}
