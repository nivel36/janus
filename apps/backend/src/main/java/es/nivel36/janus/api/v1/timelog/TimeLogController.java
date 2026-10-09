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

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import es.nivel36.janus.api.Mapper;
import es.nivel36.janus.service.employee.Employee;
import es.nivel36.janus.service.employee.EmployeeService;
import es.nivel36.janus.service.timelog.ClockOutWithoutClockInException;
import es.nivel36.janus.service.timelog.TimeLog;
import es.nivel36.janus.service.timelog.TimeLogService;
import es.nivel36.janus.service.worksite.Worksite;
import es.nivel36.janus.service.worksite.WorksiteAccessDeniedException;
import es.nivel36.janus.service.worksite.WorksiteService;

/** Spring MVC implementation of {@link TimeLogResource}. */
@RestController
public class TimeLogController implements TimeLogResource {

	private static final Logger logger = LoggerFactory.getLogger(TimeLogController.class);

	private final TimeLogService timeLogService;
	private final EmployeeService employeeService;
	private final WorksiteService worksiteService;
	private final Clock clock;
	private final Mapper<TimeLog, TimeLogResponse> timeLogResponseMapper;

	/**
	 * Creates a controller exposing time log operations.
	 *
	 * @param  timeLogService        application service handling {@link TimeLog}
	 *                               logic; must not be {@code null}
	 * @param  employeeService       service used to resolve {@link Employee}
	 *                               entities; must not be {@code null}
	 * @param  worksiteService       service resolving {@link Worksite} entities;
	 *                               must not be {@code null}
	 * @param  timeLogResponseMapper mapper that converts {@link TimeLog} domain
	 *                               objects to {@link TimeLogResponse} DTOs; must
	 *                               not be {@code null}
	 * @throws NullPointerException  if any dependency is null
	 * @param  clock                 clock used to retrieve the current time. Can't
	 *                               be {@code null}.
	 */
	public TimeLogController(
		final TimeLogService timeLogService,
		final EmployeeService employeeService,
		final WorksiteService worksiteService,
		final @Qualifier("timeLogResponseMapper") Mapper<TimeLog, TimeLogResponse> timeLogResponseMapper,
		final Clock clock) {
		this.timeLogService = Objects.requireNonNull(timeLogService, "timeLogService can't be null");
		this.employeeService = Objects.requireNonNull(employeeService, "employeeService can't be null");
		this.worksiteService = Objects.requireNonNull(worksiteService, "worksiteService can't be null");
		this.timeLogResponseMapper = Objects
				.requireNonNull(timeLogResponseMapper, "timeLogResponseMapper can't be null");
		this.clock = Objects.requireNonNull(clock, "clock can't be null");
	}

	@Override
	public ResponseEntity<TimeLogResponse> clockIn(
			final String employeeNumber,
			final Instant entryTime,
			final String worksiteCode) {
		logger.debug("Clock-in ACTION performed");

		final Employee employee = this.requireEmployee(employeeNumber);
		final Worksite worksite = this.findWorksiteForNewRecord(employee, worksiteCode);
		final TimeLog clockIn;
		if (entryTime != null) {
			clockIn = this.timeLogService.clockIn(employee, worksite, entryTime);
		} else {
			clockIn = this.timeLogService.clockIn(employee, worksite, this.clock.instant());
		}
		final TimeLogResponse timeLog = this.timeLogResponseMapper.map(clockIn);
		return ResponseEntity.status(HttpStatus.CREATED).body(timeLog);
	}

	private Worksite findWorksiteForNewRecord(final Employee employee, final String worksiteCode) {
		final Worksite worksite = this.worksiteService.findWorksiteByCode(worksiteCode);
		this.worksiteService.assertEmployeeCanUseWorksite(employee.getEmployeeNumber(), worksiteCode);
		return worksite;
	}

	@Override
	public ResponseEntity<TimeLogResponse> clockOut(
			final String employeeNumber,
			final Instant exitTime,
			final String worksiteCode) throws ClockOutWithoutClockInException {
		logger.debug("Clock-out ACTION performed");

		final Employee employee = this.requireEmployee(employeeNumber);
		final Worksite worksite = this.findWorksiteForClockOut(employee, worksiteCode);
		final TimeLog clockOut;
		if (exitTime != null) {
			clockOut = this.timeLogService.clockOut(employee, worksite, exitTime);
		} else {
			clockOut = this.timeLogService.clockOut(employee, worksite, this.clock.instant());
		}
		final TimeLogResponse timeLogResponse = this.timeLogResponseMapper.map(clockOut);
		return ResponseEntity.ok(timeLogResponse);
	}

	private Worksite findWorksiteForClockOut(final Employee employee, final String worksiteCode) {
		final Worksite worksite = this.worksiteService.findWorksiteByCode(worksiteCode);
		try {
			this.worksiteService.assertEmployeeCanUseWorksite(employee.getEmployeeNumber(), worksiteCode);
		} catch (final WorksiteAccessDeniedException ex) {
			// The worksite may have changed between clock-in and clock-out, so we allow the
			// clock-out.
			if (!this.timeLogService.hasOpenTimeLog(employee)) {
				throw ex;
			}
		}
		return worksite;
	}

	@Override
	public ResponseEntity<TimeLogResponse> createTimeLog(
			final String employeeNumber,
			final String worksiteCode,
			final CreateTimeLogRequest timeLog) {
		logger.debug("Create time log ACTION performed");

		final Employee employee = this.requireEmployee(employeeNumber);
		final Worksite worksite = this.findWorksiteForNewRecord(employee, worksiteCode);
		final Instant entryTime = timeLog.entryTime();
		final Instant exitTime = timeLog.exitTime();
		final TimeLog createdTimeLog = this.timeLogService.createTimeLog(employee, worksite, entryTime, exitTime);
		final TimeLogResponse createdTimeLogResponse = this.timeLogResponseMapper.map(createdTimeLog);
		return ResponseEntity.status(HttpStatus.CREATED).body(createdTimeLogResponse);
	}

	@Override
	public ResponseEntity<TimeLogResponse> findTimeLogByEmployeeAndEntryTime(
			final String employeeNumber,
			final Instant entryTime) {
		logger.debug("Find time log by employee and entry time ACTION performed");

		final Employee employee = this.requireEmployee(employeeNumber);
		final TimeLog timeLog = this.timeLogService.findTimeLogByEmployeeAndEntryTime(employee, entryTime);
		final TimeLogResponse timeLogResponse = this.timeLogResponseMapper.map(timeLog);
		return ResponseEntity.ok(timeLogResponse);
	}

	private Employee requireEmployee(final String employeeNumber) {
		return this.employeeService.findEmployeeByEmployeeNumber(employeeNumber);
	}

	@Override
	public ResponseEntity<Void> deleteTimeLog(final String employeeNumber, final Instant entryTime) {
		logger.debug("Delete time log ACTION performed");

		this.timeLogService.deleteTimeLog(employeeNumber, entryTime);
		return ResponseEntity.noContent().build();
	}

}
