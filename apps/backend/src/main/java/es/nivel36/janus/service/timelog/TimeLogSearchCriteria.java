/*
 * Copyright 2026 Abel Ferrer Jiménez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package es.nivel36.janus.service.timelog;

import java.time.Instant;

/**
 * Optional client filters, independent of the authorized search scope.
 * The entry-time range includes {@code start} and excludes {@code end}.
 */
public record TimeLogSearchCriteria(String employeeEmail, Instant start, Instant end) {

	public TimeLogSearchCriteria {
		if ((start == null) != (end == null)) {
			throw new IllegalArgumentException("Both start and end must be provided together or omitted.");
		}
		if (start != null && !start.isBefore(end)) {
			throw new IllegalArgumentException("end must be after start");
		}
	}
}
