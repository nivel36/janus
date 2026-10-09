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
 * <p>
 * The constraints below apply during Bean Validation. Construction alone stores
 * the supplied values without validation or normalization.
 *
 * @param employeeNumber optional exact employee number; {@code null} disables
 *                       the filter
 * @param start          optional inclusive entry-time bound; requires end
 * @param end            optional exclusive entry-time bound; must be after
 *                       start
 */
public record TimeLogSearchRequest(@EmployeeNumber
String employeeNumber, Instant start, Instant end) {

	/**
	 * Returns whether the entry-time bounds are both present or both absent.
	 *
	 * @return {@code true} if both bounds are present or both are {@code null};
	 *         {@code false} otherwise
	 */
	@JsonIgnore
	@AssertTrue(message = "start and end must be provided together or omitted")
	public boolean isRangeComplete() {
		return this.start == null == (this.end == null);
	}

	/**
	 * Returns whether supplied bounds are strictly increasing.
	 *
	 * @return {@code true} if either bound is {@code null} or {@code start} is
	 *         strictly before {@code end}; {@code false} otherwise
	 */
	@JsonIgnore
	@AssertTrue(message = "end must be after start")
	public boolean isRangeOrdered() {
		return this.start == null || this.end == null || this.start.isBefore(this.end);
	}
}
