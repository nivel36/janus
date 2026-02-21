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

import java.time.Instant;
import java.time.ZoneId;

import org.openapitools.jackson.nullable.JsonNullable;

import com.fasterxml.jackson.annotation.JsonInclude;

import es.nivel36.janus.service.employee.Employee;
import es.nivel36.janus.service.worksite.Worksite;

/**
 * Response record representing a single time log entry for an employee.
 * <p>
 * This DTO is returned by REST controllers to expose information about an
 * employee's work session at a specific worksite. It contains the mandatory
 * entry time and an optional exit time, along with identifiers of the
 * {@link Employee} and the {@link Worksite}.
 * </p>
 *
 * @param employeeEmail  the unique email address of the {@link Employee}
 *                       associated with this time log; must not be {@code null}
 * @param worksiteCode   the unique code identifying the {@link Worksite} where
 *                       the time log was recorded; must not be {@code null}
 * @param worksiteZoneId the zone id associated with the {@link Worksite} where
 *                       the time log was recorded; must not be {@code null}
 * @param entryTime      the timestamp when the employee clocked in; must not be
 *                       {@code null}
 * @param exitTime       the timestamp when the employee clocked out; may be
 *                       absent if the employee is still working;
 */
public record TimeLogResponse(String employeeEmail, String worksiteCode, ZoneId worksiteTimeZone, Instant entryTime,
		@JsonInclude(JsonInclude.Include.NON_ABSENT) JsonNullable<Instant> exitTime,
		JsonNullable<DurationResponse> workTime) {
}
