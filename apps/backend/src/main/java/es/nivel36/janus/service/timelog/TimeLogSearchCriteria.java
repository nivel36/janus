/*
 * Copyright 2026 Abel Ferrer Jiménez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package es.nivel36.janus.service.timelog;

import java.time.Instant;

import es.nivel36.janus.validation.EmployeeNumber;

/**
 * Optional filters for time-log searches, independent of the authorized scope.
 * <p>
 * Construction does not validate the filter combination. The search service
 * requires both bounds to be absent or both present with start strictly before
 * end.
 *
 * @param employeeNumber the optional exact employee-number filter
 * @param start          the optional inclusive entry-time bound
 * @param end            the optional exclusive entry-time bound
 */
public record TimeLogSearchCriteria(@EmployeeNumber
String employeeNumber,

		Instant start,

		Instant end) {
}
