/*
 * Copyright 2026 Abel Ferrer Jiménez
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

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;

import org.springframework.stereotype.Component;

import es.nivel36.janus.api.Mapper;
import es.nivel36.janus.service.employee.Employee;
import es.nivel36.janus.service.timelog.TimeLog;
import es.nivel36.janus.service.worksite.Worksite;

/**
 * {@link Mapper} implementation that converts a {@link TimeLog} entity into a
 * {@link TimeLogResponse} record.
 */
@Component
public class TimeLogResponseMapper implements Mapper<TimeLog, TimeLogResponse> {

	private final Mapper<Duration, DurationResponse> durationResponseMapper;

	public TimeLogResponseMapper(final Mapper<Duration, DurationResponse> durationResponseMapper) {
		this.durationResponseMapper = Objects.requireNonNull(durationResponseMapper,
				"durationResponseMapper can't be null");
	}

	@Override
	public TimeLogResponse map(final TimeLog entity) {
		if (entity == null) {
			return null;
		}
		final Employee employee = Objects.requireNonNull(entity.getEmployee(), "Employee can't be null");
		final Worksite worksite = Objects.requireNonNull(entity.getWorksite(), "Worksite can't be null");

		final String employeeEmail = employee.getEmail();
		final String worksiteCode = worksite.getCode();

		final ZoneId worksiteZoneId = worksite.getTimeZone();

		final Instant entryTime = entity.getEntryTime();
		final Instant exitTimeValue = entity.getExitTime();
		final Instant exitTime = exitTimeValue;

		final Duration workDurationValue = entity.getWorkDuration();
		final DurationResponse workDurationResponse = mapWorkDuration(workDurationValue);
		final DurationResponse workTime = workDurationResponse;

		return new TimeLogResponse(employeeEmail, worksiteCode, worksiteZoneId, entryTime, exitTime, workTime);
	}

	private DurationResponse mapWorkDuration(final Duration duration) {
		return this.durationResponseMapper.map(duration);
	}

}
