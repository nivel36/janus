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

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Qualifier;
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

	/**
	 * Creates a mapper without accessing persistence.
	 *
	 * @param  durationResponseMapper nonnull duration mapper
	 * @throws NullPointerException   if the mapper is null
	 */
	public TimeLogResponseMapper(
		final @Qualifier("durationResponseMapper") Mapper<Duration, DurationResponse> durationResponseMapper) {
		this.durationResponseMapper = Objects
				.requireNonNull(durationResponseMapper, "durationResponseMapper can't be null");
	}

	/**
	 * Maps stable business identifiers, worksite timezone and recorded instants.
	 *
	 * @param  entity               time log with readable employee and worksite
	 *                              associations; may be null
	 * @return                      response, or null for a null entity; open logs
	 *                              have null exit and duration
	 * @throws NullPointerException if an association is absent
	 */
	@Override
	public TimeLogResponse map(final TimeLog entity) {
		if (entity == null) {
			return null;
		}
		final Employee employee = Objects.requireNonNull(entity.getEmployee(), "Employee can't be null");
		final Worksite worksite = Objects.requireNonNull(entity.getWorksite(), "Worksite can't be null");

		final String employeeNumber = employee.getEmployeeNumber();
		final String worksiteCode = worksite.getCode();

		final ZoneId worksiteZoneId = worksite.getTimeZone();

		final Instant entryTime = entity.getEntryTime();
		final Instant exitTime = entity.getExitTime();

		final DurationResponse workTime = this.durationResponseMapper.map(entity.getWorkDuration());

		return new TimeLogResponse(employeeNumber, worksiteCode, worksiteZoneId, entryTime, exitTime, workTime);
	}

}
