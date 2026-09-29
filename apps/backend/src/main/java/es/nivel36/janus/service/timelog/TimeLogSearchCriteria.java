/*
 * Copyright 2026 Abel Ferrer Jiménez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package es.nivel36.janus.service.timelog;

import java.time.Instant;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;

/**
 * Optional client filters, independent of the authorized search scope.
 * The entry-time range includes {@code start} and excludes {@code end}.
 */
public record TimeLogSearchCriteria(
		@Pattern(regexp = "^[ \\t]*(?=[^ \\t]{1,254}[ \\t]*$)[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}[ \\t]*$", message = "must be a valid and safe email address (max 254)") String employeeEmail,
		Instant start, Instant end) {

	@AssertTrue(message = "start and end must be provided together or omitted")
	public boolean isRangeComplete() {
		return (this.start == null) == (this.end == null);
	}

	@AssertTrue(message = "end must be after start")
	public boolean isRangeOrdered() {
		return this.start == null || this.end == null || this.start.isBefore(this.end);
	}
}
