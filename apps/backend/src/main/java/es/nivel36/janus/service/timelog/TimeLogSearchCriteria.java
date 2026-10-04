/*
 * Copyright 2026 Abel Ferrer Jiménez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package es.nivel36.janus.service.timelog;

import java.time.Instant;

/**
 * Optional client filters, independent of the authorized search scope. The
 * entry-time range includes {@code start} and excludes {@code end}.
 */
public record TimeLogSearchCriteria(String employeeNumber, //

		Instant start, //

		Instant end) {
}
