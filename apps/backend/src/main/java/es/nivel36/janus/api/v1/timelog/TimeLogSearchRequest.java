/*
 * Copyright 2026 Abel Ferrer Jiménez
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package es.nivel36.janus.api.v1.timelog;

import java.time.Instant;

import es.nivel36.janus.api.validation.EmployeeNumber;
import jakarta.validation.constraints.AssertTrue;

/**
 * Optional HTTP query parameters for searching time logs. The entry-time range
 * includes {@code start} and excludes {@code end}.
 */
public record TimeLogSearchRequest(
		@EmployeeNumber //
		String employeeNumber, //

		Instant start, //

		Instant end) {

	@AssertTrue(message = "start and end must be provided together or omitted")
	public boolean isRangeComplete() {
		return (this.start == null) == (this.end == null);
	}

	@AssertTrue(message = "end must be after start")
	public boolean isRangeOrdered() {
		return this.start == null || this.end == null || this.start.isBefore(this.end);
	}
}
