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

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.security.access.AccessDeniedException;
import es.nivel36.janus.service.ResourceNotFoundException;
import es.nivel36.janus.service.timelog.ClockOutWithoutClockInException;
import es.nivel36.janus.service.timelog.TimeLogModificationNotAllowedException;
import es.nivel36.janus.service.timelog.TimeLogFutureTimeException;
import es.nivel36.janus.service.timelog.TimeLogChronologyException;
import es.nivel36.janus.service.timelog.WorksiteMismatchOnClockOutException;
import es.nivel36.janus.service.worksite.WorksiteAccessDeniedException;
import es.nivel36.janus.validation.EmployeeNumber;
import es.nivel36.janus.validation.WorksiteCode;
import jakarta.validation.Valid;

/**
 * HTTP contract for employee time logs under {@code /api/v1/employees}.
 * Requests require a validated bearer JWT and a provisioned actor.
 * Authorization is declared here and precedes execution. Employee numbers and
 * worksite codes match {@code [A-Za-z0-9_-]{1,50}} without trimming. Instants
 * use ISO-8601.
 */
@RequestMapping("/api/v1")
public interface TimeLogResource {
	/**
	 * Creates an open time log for the caller's linked employee.
	 * <p>
	 * The caller must have {@code JANUS_EMPLOYEE}. Explicit entry times
	 * additionally require manual entry to be enabled. The employee and worksite
	 * must exist and the employee must be allowed to use the worksite. A missing
	 * entry time uses the injected clock. Times are truncated to seconds, must not
	 * be in the future and must be strictly after the configured lock threshold.
	 * Duplicate active employee/entry-time pairs are rejected.
	 * </p>
	 *
	 * @param  employeeNumber                         exact employee identifier
	 * @param  entryTime                              optional entry instant;
	 *                                                {@code null} uses the current
	 *                                                time
	 * @param  worksiteCode                           exact accessible worksite code
	 * @return                                        HTTP 201 containing the
	 *                                                persisted open time log
	 * @throws ResourceNotFoundException              if employee or worksite is
	 *                                                absent
	 * @throws AccessDeniedException                  if the caller cannot operate
	 *                                                on this employee
	 * @throws WorksiteAccessDeniedException          if the worksite is
	 *                                                inaccessible
	 * @throws TimeLogFutureTimeException             if the truncated entry time is
	 *                                                in the future
	 * @throws TimeLogModificationNotAllowedException if entry is locked or
	 *                                                duplicated
	 */
	@PostMapping({ "/employees/{employeeNumber}/time-logs/clock-in" })
	@PreAuthorize("@timeLogAuthorization.canOperate(authentication, #employeeNumber, #entryTime != null)")
	ResponseEntity<TimeLogResponse> clockIn(
			@PathVariable("employeeNumber")
			@EmployeeNumber
			String employeeNumber,
			@RequestParam(value = "entryTime", required = false)
			Instant entryTime,
			@RequestParam("worksiteCode")
			@WorksiteCode
			String worksiteCode);

	/**
	 * Closes the employee's most recent open time log.
	 * <p>
	 * The caller must have {@code JANUS_EMPLOYEE} and own the employee association;
	 * explicit exit times require manual entry to be enabled. Null exit time uses
	 * the injected clock. The truncated exit time must be editable, not in the
	 * future and strictly after entry. The worksite must exist. If an open log
	 * exists, clock-out is allowed after worksite access is revoked; switching
	 * worksites still requires the configured permission. When no open log exists,
	 * an anomaly event is persisted before reporting the conflict.
	 * </p>
	 *
	 * @param  employeeNumber                         exact employee identifier
	 * @param  exitTime                               optional exit instant;
	 *                                                {@code null} uses the current
	 *                                                time
	 * @param  worksiteCode                           exact worksite code
	 * @return                                        HTTP 200 containing the closed
	 *                                                time log
	 * @throws ResourceNotFoundException              if employee or worksite is
	 *                                                absent
	 * @throws AccessDeniedException                  if the caller cannot operate
	 *                                                on this employee
	 * @throws ClockOutWithoutClockInException        if no open log exists
	 * @throws TimeLogFutureTimeException             if the truncated exit time is
	 *                                                in the future
	 * @throws TimeLogChronologyException             if truncated exit is not
	 *                                                strictly after entry
	 * @throws WorksiteMismatchOnClockOutException    if worksite changes are
	 *                                                disabled
	 * @throws TimeLogModificationNotAllowedException if exit is locked
	 */
	@PostMapping({ "/employees/{employeeNumber}/time-logs/clock-out" })
	@PreAuthorize("@timeLogAuthorization.canOperate(authentication, #employeeNumber, #exitTime != null)")
	ResponseEntity<TimeLogResponse> clockOut(
			@PathVariable("employeeNumber")
			@EmployeeNumber
			String employeeNumber,
			@RequestParam(value = "exitTime", required = false)
			Instant exitTime,
			@RequestParam("worksiteCode")
			@WorksiteCode
			String worksiteCode) throws ClockOutWithoutClockInException;

	/**
	 * Creates a closed time log for the caller's linked employee.
	 * <p>
	 * The caller must have {@code JANUS_EMPLOYEE} and manual entry must be enabled.
	 * Both payload times are required and exit must be strictly after entry,
	 * including after truncation to seconds. Both must be editable and not in the
	 * future. The employee and accessible worksite must exist. Duplicate active
	 * employee/entry-time pairs are rejected.
	 * </p>
	 *
	 * @param  employeeNumber                         exact employee identifier
	 * @param  worksiteCode                           exact accessible worksite code
	 * @param  timeLog                                complete validated entry and
	 *                                                exit times
	 * @return                                        HTTP 201 containing the
	 *                                                persisted closed time log
	 * @throws ResourceNotFoundException              if employee or worksite is
	 *                                                absent
	 * @throws AccessDeniedException                  if manual creation is
	 *                                                unauthorized
	 * @throws WorksiteAccessDeniedException          if the worksite is
	 *                                                inaccessible
	 * @throws TimeLogFutureTimeException             if a truncated time is in the
	 *                                                future
	 * @throws TimeLogChronologyException             if truncated exit is not
	 *                                                strictly after entry
	 * @throws TimeLogModificationNotAllowedException if a time is locked or
	 *                                                duplicated
	 */
	@PostMapping({ "/employees/{employeeNumber}/time-logs" })
	@PreAuthorize("@timeLogAuthorization.canOperate(authentication, #employeeNumber, true)")
	ResponseEntity<TimeLogResponse> createTimeLog(
			@PathVariable("employeeNumber")
			@EmployeeNumber
			String employeeNumber,
			@RequestParam("worksiteCode")
			@WorksiteCode
			String worksiteCode,
			@RequestBody
			@Valid
			CreateTimeLogRequest timeLog);

	/**
	 * Retrieves an active time log by its employee and exact entry instant.
	 * <p>
	 * {@code JANUS_ADMIN} and {@code JANUS_USER} may view any employee's logs.
	 * {@code JANUS_EMPLOYEE} may view only the persistent linked employee. This
	 * operation does not modify records and does not truncate the lookup instant.
	 * </p>
	 *
	 * @param  employeeNumber            exact employee identifier
	 * @param  entryTime                 nonnull exact entry instant
	 * @return                           HTTP 200 containing the matching time log
	 * @throws ResourceNotFoundException if employee or active time log is absent
	 * @throws AccessDeniedException     if the caller cannot view the employee
	 */
	@GetMapping({ "/employees/{employeeNumber}/time-logs/{entryTime}" })
	@PreAuthorize("@timeLogAuthorization.canView(authentication, #employeeNumber)")
	ResponseEntity<TimeLogResponse> findTimeLogByEmployeeAndEntryTime(
			@PathVariable("employeeNumber")
			@EmployeeNumber
			String employeeNumber,
			@PathVariable("entryTime")
			Instant entryTime);

	/**
	 * Soft-deletes an editable time log as a provisioned {@code JANUS_ADMIN}.
	 * <p>
	 * The employee and active log must exist. Deletion is allowed only while entry
	 * time is strictly after the configured lock threshold. Subsequent lookups and
	 * searches omit the deleted log.
	 * </p>
	 *
	 * @param  employeeNumber                         exact employee identifier
	 * @param  entryTime                              nonnull exact entry instant
	 * @return                                        HTTP 204 with no body
	 * @throws ResourceNotFoundException              if employee or active time log
	 *                                                is absent
	 * @throws AccessDeniedException                  if the caller is not an
	 *                                                administrator
	 * @throws TimeLogModificationNotAllowedException if deletion is locked
	 */
	@DeleteMapping({ "/employees/{employeeNumber}/time-logs/{entryTime}" })
	@PreAuthorize("@timeLogAuthorization.canDelete(authentication)")
	ResponseEntity<Void> deleteTimeLog(
			@PathVariable("employeeNumber")
			@EmployeeNumber
			String employeeNumber,
			@PathVariable("entryTime")
			Instant entryTime);

}
