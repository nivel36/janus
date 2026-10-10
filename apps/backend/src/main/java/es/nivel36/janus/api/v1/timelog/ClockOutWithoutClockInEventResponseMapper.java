/*
 * Copyright 2026 Abel Ferrer Jiménez
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package es.nivel36.janus.api.v1.timelog;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Qualifier;
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

	private final Mapper<TimeLog, TimeLogResponse> timeLogResponseMapper;

	/**
	 * Creates a mapper that includes the resolving time log when present.
	 *
	 * @param  timeLogResponseMapper nonnull time log mapper
	 * @throws NullPointerException  if the mapper is {@code null}
	 */
	public ClockOutWithoutClockInEventResponseMapper(
		final @Qualifier("timeLogResponseMapper") Mapper<TimeLog, TimeLogResponse> timeLogResponseMapper) {
		this.timeLogResponseMapper = Objects
				.requireNonNull(timeLogResponseMapper, "timeLogResponseMapper can't be null");
	}

	@Override
	public ClockOutWithoutClockInEventResponse map(final ClockOutWithoutClockInEvent entity) {
		if (entity == null) {
			return null;
		}
		final Employee employee = Objects.requireNonNull(entity.getEmployee(), "employee can't be null");
		final Worksite worksite = Objects.requireNonNull(entity.getWorksite(), "worksite can't be null");

		final String employeeNumber = employee.getEmployeeNumber();
		final String employeeFullName = employee.getName() + " " + employee.getSurname();
		final TimeLogEmployeeResponse employeeResponse = new TimeLogEmployeeResponse(employeeNumber, employeeFullName);
		final String worksiteCode = worksite.getCode();
		final String worksiteName = worksite.getName();
		final ZoneId worksiteZoneId = worksite.getTimeZone();
		final TimeLogWorksiteResponse worksiteResponse = new TimeLogWorksiteResponse(
				worksiteCode,
				worksiteName,
				worksiteZoneId);
		final Instant exitTime = entity.getExitTime();
		final Instant detectedAt = entity.getDetectedAt();
		final boolean resolved = entity.isResolved();
		final boolean invalidated = entity.isInvalidated();
		final String reason = entity.getReason();

		final TimeLog resolvedTimeLog = entity.getResolvedTimeLog();
		final TimeLogResponse resolvedTimeLogResponse = this.timeLogResponseMapper.map(resolvedTimeLog);

		return new ClockOutWithoutClockInEventResponse(
				employeeResponse,
				worksiteResponse,
				exitTime,
				detectedAt,
				resolved,
				invalidated,
				reason,
				resolvedTimeLogResponse);
	}

}
