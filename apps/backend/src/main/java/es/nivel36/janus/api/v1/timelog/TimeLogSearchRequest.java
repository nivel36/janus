/*
 * Copyright 2026 Abel Ferrer Jiménez Licensed under the Apache License, Version
 * 2.0 (the "License");
 */
package es.nivel36.janus.api.v1.timelog;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnore;

import es.nivel36.janus.validation.EmployeeNumber;
import jakarta.validation.constraints.AssertTrue;

/**
 * Optional HTTP query parameters for searching time logs. The entry-time range
 * includes {@code start} and excludes {@code end}.
 *
 * @param employeeNumber optional exact employee number; null disables the
 *                       filter
 * @param start          optional inclusive entry-time bound; requires end
 * @param end            optional exclusive entry-time bound; must be after
 *                       start
 */
public record TimeLogSearchRequest(@EmployeeNumber
String employeeNumber, Instant start, Instant end) {

	/** @return true if both bounds are present or both absent */
	@JsonIgnore
	@AssertTrue(message = "start and end must be provided together or omitted")
	public boolean isRangeComplete() {
		return this.start == null == (this.end == null);
	}

	/** @return true if bounds are incomplete or strictly increasing */
	@JsonIgnore
	@AssertTrue(message = "end must be after start")
	public boolean isRangeOrdered() {
		return this.start == null || this.end == null || this.start.isBefore(this.end);
	}
}
