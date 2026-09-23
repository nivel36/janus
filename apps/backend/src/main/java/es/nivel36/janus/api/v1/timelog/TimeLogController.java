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

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;

import es.nivel36.janus.api.Mapper;
import es.nivel36.janus.policy.timelog.TimeLogAuthorizationAdapter;
import es.nivel36.janus.service.employee.Employee;
import es.nivel36.janus.service.employee.EmployeeService;
import es.nivel36.janus.service.timelog.ClockOutWithoutClockInException;
import es.nivel36.janus.service.timelog.TimeLog;
import es.nivel36.janus.service.timelog.TimeLogService;
import es.nivel36.janus.service.worksite.Worksite;
import es.nivel36.janus.service.worksite.WorksiteAccessDeniedException;
import es.nivel36.janus.service.worksite.WorksiteService;
import es.nivel36.janus.util.EmailAddresses;

/**
 * REST controller responsible for exposing operations related to employee time
 * logs.
 * <p>
 * Provides endpoints for creating, retrieving, and deleting {@link TimeLog}
 * entries, as well as clock-in/clock-out operations and duration calculations.
 */
@RestController
public class TimeLogController implements TimeLogResource {

	private static final Logger logger = LoggerFactory.getLogger(TimeLogController.class);

	private final TimeLogService timeLogService;
	private final TimeLogAuthorizationAdapter authorization;
	private final EmployeeService employeeService;
	private final WorksiteService worksiteService;
	private final Clock clock;
	private final Mapper<TimeLog, TimeLogResponse> timeLogResponseMapper;

	/**
	 * Creates a controller exposing time log operations.
	 *
	 * @param timeLogService        application service handling {@link TimeLog}
	 *                              logic; must not be {@code null}
	 * @param authorization         authorization adapter used to scope employee-only
	 *                              operations; must not be {@code null}
	 * @param employeeService       service used to resolve {@link Employee}
	 *                              entities; must not be {@code null}
	 * @param worksiteService       service resolving {@link Worksite} entities;
	 *                              must not be {@code null}
	 * @param timeLogResponseMapper mapper that converts {@link TimeLog} domain
	 *                              objects to {@link TimeLogResponse} DTOs; must
	 *                              not be {@code null}
	 * @param clock                 clock used to retrieve the current time. Can't
	 *                              be {@code null}.
	 */
	public TimeLogController( //
			final TimeLogService timeLogService, //
			final TimeLogAuthorizationAdapter authorization, //
			final EmployeeService employeeService, //
			final WorksiteService worksiteService, //
			final @Qualifier("timeLogResponseMapper") Mapper<TimeLog, TimeLogResponse> timeLogResponseMapper, //
			final Clock clock //
	) {
		this.timeLogService = Objects.requireNonNull( //
				timeLogService, "timeLogService can't be null");
		this.authorization = Objects.requireNonNull(authorization, "authorization can't be null");
		this.employeeService = Objects.requireNonNull( //
				employeeService, "employeeService can't be null");
		this.worksiteService = Objects.requireNonNull( //
				worksiteService, "worksiteService can't be null");
		this.timeLogResponseMapper = Objects.requireNonNull( //
				timeLogResponseMapper, "timeLogResponseMapper can't be null");
		this.clock = Objects.requireNonNull( //
				clock, "clock can't be null");
	}

	/**
	 * Clocks in an employee at a specified entry time or at the current time if
	 * none is provided.
	 *
	 * @param employeeEmail the email of the employee; must not be {@code null}
	 * @param entryTime     the entry time as ISO-8601 string (e.g.,
	 *                      "2025-08-04T09:30:00Z"); if {@code null}, the current
	 *                      system time will be used
	 * @param worksiteCode  the code of the worksite where the time log is created;
	 *                      must not be {@code null}
	 * @param authentication current authentication; must not be {@code null}
	 * @return the created {@link TimeLogResponse}
	 */
	@Override
	public ResponseEntity<TimeLogResponse> clockIn( //
			final String employeeEmail, //
			final Instant entryTime, //
			final String worksiteCode, //
			final Authentication authentication) {
		logger.debug("Clock-in ACTION performed");

		final String email = this.effectiveEmployeeEmail(authentication, employeeEmail);
		final Employee employee = this.employeeService.findEmployeeByEmail(email);
		final Worksite worksite = this.findWorksiteForNewRecord(email, worksiteCode.trim());
		final TimeLog clockIn;
		if (entryTime != null) {
			clockIn = this.timeLogService.clockIn(employee, worksite, entryTime);
		} else {
			clockIn = this.timeLogService.clockIn(employee, worksite, this.clock.instant());
		}
		final TimeLogResponse timeLog = this.timeLogResponseMapper.map(clockIn);
		return ResponseEntity.status(HttpStatus.CREATED).body(timeLog);
	}

	private Worksite findWorksiteForNewRecord(final String employeeEmail, final String worksiteCode) {
		final Worksite worksite = this.worksiteService.findWorksiteByCode(worksiteCode);
		this.worksiteService.assertEmployeeCanUseWorksite(employeeEmail, worksite);
		return worksite;
	}

	/**
	 * Clocks out an employee at a specified exit time or at the current time if
	 * none is provided.
	 *
	 * @param employeeEmail the email of the employee; must not be {@code null}
	 * @param exitTime      the exit time as ISO-8601 string (e.g.,
	 *                      "2025-08-04T18:00:00Z"); if {@code null}, the current
	 *                      system time will be used
	 * @param worksiteCode  the code of the worksite where the time log is updated;
	 *                      must not be {@code null}
	 * @param authentication current authentication; must not be {@code null}
	 * @return the updated {@link TimeLogResponse}
	 * @throws ClockOutWithoutClockInException if the TimeLog record cannot be
	 *                                         closed because it does not have an
	 *                                         entry time
	 */
	@Override
	public ResponseEntity<TimeLogResponse> clockOut( //
			final String employeeEmail, //
			final Instant exitTime, //
			final String worksiteCode, //
			final Authentication authentication) throws ClockOutWithoutClockInException {
		logger.debug("Clock-out ACTION performed");

		final String email = this.effectiveEmployeeEmail(authentication, employeeEmail);
		final Employee employee = this.employeeService.findEmployeeByEmail(email);
		final Worksite worksite = this.findWorksiteForClockOut(email,
				worksiteCode);
		final TimeLog clockOut;
		if (exitTime != null) {
			clockOut = this.timeLogService.clockOut(employee, worksite, exitTime);
		} else {
			clockOut = this.timeLogService.clockOut(employee, worksite, this.clock.instant());
		}
		final TimeLogResponse timeLogResponse = this.timeLogResponseMapper.map(clockOut);
		return ResponseEntity.ok(timeLogResponse);
	}

	private Worksite findWorksiteForClockOut(final String employeeEmail, final String worksiteCode) {
		final Worksite worksite = this.worksiteService.findWorksiteByCode(worksiteCode);
		try {
			this.worksiteService.assertEmployeeCanUseWorksite(employeeEmail, worksite);
		} catch (final WorksiteAccessDeniedException ex) {
			// The worksite may have changed between clock-in and clock-out, so we allow the
			// clock-out.
			if (!this.timeLogService.hasOpenTimeLog(employeeEmail)) {
				throw ex;
			}
		}
		return worksite;
	}

	/**
	 * Creates a new time log entry for a specific employee and worksite.
	 *
	 * @param employeeEmail the email of the employee; must not be {@code null}
	 * @param worksiteCode  the code of the worksite where the time log is created;
	 *                      must not be {@code null}
	 * @param timeLog       the {@link CreateTimeLogRequest} payload containing the
	 *                      entry and exit times; must not be {@code null}
	 * @param authentication the current authentication; must not be {@code null}
	 * @return the created {@link TimeLogResponse}
	 */
	@Override
	public ResponseEntity<TimeLogResponse> createTimeLog( //
			final String employeeEmail, //
			final String worksiteCode, //
			final CreateTimeLogRequest timeLog, //
			final Authentication authentication) {
		logger.debug("Create time log ACTION performed");

		final String email = this.effectiveEmployeeEmail(authentication, employeeEmail);
		final Employee employee = this.employeeService.findEmployeeByEmail(email);
		final Worksite worksite = this.findWorksiteForNewRecord(email, worksiteCode.trim());
		final Instant entryTime = timeLog.entryTime();
		final Instant exitTime = timeLog.exitTime();
		final TimeLog createdTimeLog = this.timeLogService.createTimeLog(employee, worksite, entryTime, exitTime);
		final TimeLogResponse createdTimeLogResponse = this.timeLogResponseMapper.map(createdTimeLog);
		return ResponseEntity.ok(createdTimeLogResponse);
	}

	/**
	 * Finds a specific time log for an employee by its entry time.
	 *
	 * @param employeeEmail the email of the employee; must not be {@code null}
	 * @param entryTime     the entry time of the time log; must not be {@code null}
	 * @param authentication the current authentication; must not be {@code null}
	 * @return the {@link TimeLogResponse} entry
	 */
	@Override
	public ResponseEntity<TimeLogResponse> findTimeLogByEmployeeAndEntryTime(//
			final String employeeEmail, //
			final Instant entryTime, //
			final Authentication authentication) {
		logger.debug("Find time log by employee and entry time ACTION performed");

		final String email = this.effectiveEmployeeEmail(authentication, employeeEmail);
		final TimeLog timeLog = this.timeLogService.findTimeLogByEmployeeAndEntryTime(email, entryTime);
		final TimeLogResponse timeLogResponse = this.timeLogResponseMapper.map(timeLog);
		return ResponseEntity.ok(timeLogResponse);
	}

	private String effectiveEmployeeEmail(final Authentication authentication, final String requestedEmail) {
		return EmailAddresses.canonicalize(this.authorization.effectiveEmployeeEmail(authentication, requestedEmail));
	}

	/**
	 * Deletes a time log entry for an employee by its entry time.
	 *
	 * @param employeeEmail the email of the employee; must not be {@code null}
	 * @param entryTime     the entry time of the time log to delete; must not be
	 *                      {@code null}
	 * @return a {@link ResponseEntity} with no content (HTTP 204) if the deletion
	 *         succeeds
	 */
	@Override
	public ResponseEntity<Void> deleteTimeLog(//
			final String employeeEmail, //
			final Instant entryTime) {
		logger.debug("Delete time log ACTION performed");

		final String email = EmailAddresses.canonicalize(employeeEmail);
		final TimeLog timeLog = this.timeLogService.findTimeLogByEmployeeAndEntryTime(email, entryTime);
		this.timeLogService.deleteTimeLog(timeLog);
		return ResponseEntity.noContent().build();
	}
}
