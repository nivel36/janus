/*
 * Copyright 2025 Abel Ferrer Jiménez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package es.nivel36.janus.api.v1.timelog;

import java.time.Instant;
import java.util.Objects;

import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.stereotype.Component;

import es.nivel36.janus.api.Mapper;
import es.nivel36.janus.service.employee.Employee;
import es.nivel36.janus.service.timelog.ClockOutWithoutClockInEvent;
import es.nivel36.janus.service.timelog.TimeLog;
import es.nivel36.janus.service.worksite.Worksite;

/**
 * Maps {@link ClockOutWithoutClockInEvent} domain entities to their API
 * representation.
 */
@Component
public class ClockOutWithoutClockInEventResponseMapper
		implements Mapper<ClockOutWithoutClockInEvent, ClockOutWithoutClockInEventResponse> {

	@Override
	public ClockOutWithoutClockInEventResponse map(final ClockOutWithoutClockInEvent entity) {
		if (entity == null) {
			return null;
		}
		final Employee employee = Objects.requireNonNull(entity.getEmployee(), "employee can't be null");
		final Worksite worksite = Objects.requireNonNull(entity.getWorksite(), "worksite can't be null");

		final String employeeEmail = employee.getEmail();
		final String worksiteCode = worksite.getCode();
		final Instant exitTime = entity.getExitTime();
		final Instant detectedAt = entity.getDetectedAt();
		final boolean resolved = entity.isResolved();
		final boolean invalidated = entity.isInvalidated();
		final JsonNullable<String> reason = toJsonNullable(entity.getReason());

		final TimeLog resolvedTimeLog = entity.getResolvedTimeLog();
		final JsonNullable<Instant> resolvedTimeLogEntry = resolvedTimeLog == null ? JsonNullable.undefined()
				: toJsonNullable(resolvedTimeLog.getEntryTime());
		final JsonNullable<Instant> resolvedTimeLogExit = resolvedTimeLog == null ? JsonNullable.undefined()
				: toJsonNullable(resolvedTimeLog.getExitTime());

		return new ClockOutWithoutClockInEventResponse(employeeEmail, worksiteCode, exitTime, detectedAt, resolved,
				invalidated, reason, resolvedTimeLogEntry, resolvedTimeLogExit);
	}

	private static <T> JsonNullable<T> toJsonNullable(final T value) {
		return value == null ? JsonNullable.undefined() : JsonNullable.of(value);
	}
}
