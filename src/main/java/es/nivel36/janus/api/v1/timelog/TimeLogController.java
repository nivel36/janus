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

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import es.nivel36.janus.api.Mapper;
import es.nivel36.janus.service.ResourceNotFoundException;
import es.nivel36.janus.service.employee.Employee;
import es.nivel36.janus.service.employee.EmployeeService;
import es.nivel36.janus.service.timelog.TimeLog;
import es.nivel36.janus.service.timelog.TimeLogService;
import es.nivel36.janus.service.worksite.Worksite;
import es.nivel36.janus.service.worksite.WorksiteService;

/**
 * REST controller responsible for exposing operations related to employee time
 * logs.
 * <p>
 * Provides endpoints for creating, retrieving, and deleting {@link TimeLog}
 * entries, as well as clock-in/clock-out operations and duration calculations.
 */
@RestController
@RequestMapping("/api/v1/employee/{employeeEmail}/timelogs")
public class TimeLogController {

	private static final Logger logger = LoggerFactory.getLogger(TimeLogController.class);

	private final TimeLogService timeLogService;
	private final EmployeeService employeeService;
	private final WorksiteService worksiteService;
	private final Mapper<TimeLog, TimeLogResponse> timeLogResponseMapper;

	public TimeLogController(final TimeLogService timeLogService, final EmployeeService employeeService,
			final WorksiteService worksiteService, final Mapper<TimeLog, TimeLogResponse> timeLogResponseMapper) {
		this.timeLogService = Objects.requireNonNull(timeLogService, "TimeLogService can't be null");
		this.employeeService = Objects.requireNonNull(employeeService, "EmployeeService can't be null");
		this.worksiteService = Objects.requireNonNull(worksiteService, "WorksiteService can't be null");
		this.timeLogResponseMapper = Objects.requireNonNull(timeLogResponseMapper,
				"TimeLogResponseMapper can't be null");
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
	 * @return the created {@link TimeLogResponse}
	 */
	@PostMapping("/clock-in")
	public ResponseEntity<TimeLogResponse> clockIn(final @PathVariable("employeeEmail") String employeeEmail,
			final @RequestParam(value = "entryTime", required = false) Instant entryTime,
			final @RequestParam("worksiteCode") String worksiteCode) {
		Objects.requireNonNull(employeeEmail, "EmployeeEmail can't be null");
		Objects.requireNonNull(worksiteCode, "WorksiteCode can't be null");
		logger.debug("Clock-in ACTION performed");

		final Employee employee = findEmployee(employeeEmail);
		final Worksite worksite = findWorksite(worksiteCode);

		final TimeLog clockIn;
		if (entryTime != null) {
			clockIn = this.timeLogService.clockIn(employee, worksite, entryTime);
		} else {
			clockIn = this.timeLogService.clockIn(employee, worksite);
		}

		final TimeLogResponse timeLog = this.timeLogResponseMapper.map(clockIn);
		return ResponseEntity.status(HttpStatus.CREATED).body(timeLog);
	}

	private Worksite findWorksite(final String worksiteCode) {
		final Worksite worksite = this.worksiteService.findWorksiteByCode(worksiteCode);
		if (worksite == null) {
			logger.warn("No worksite found with code: {}", worksiteCode);
			throw new ResourceNotFoundException("No worksite found with code " + worksiteCode);
		}
		return worksite;
	}

	private Employee findEmployee(final String employeeEmail) {
		final Employee employee = this.employeeService.findEmployeeByEmail(employeeEmail);
		if (employee == null) {
			logger.warn("No employee found with email: {}", employeeEmail);
			throw new ResourceNotFoundException("No employee found with email " + employeeEmail);
		}
		return employee;
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
	 * @return the updated {@link TimeLogResponse}
	 */
	@PostMapping("/clock-out")
	public ResponseEntity<TimeLogResponse> clockOut(final @PathVariable("employeeEmail") String employeeEmail,
			final @RequestParam(value = "exitTime", required = false) Instant exitTime,
			final @RequestParam("worksiteCode") String worksiteCode) {
		Objects.requireNonNull(employeeEmail, "EmployeeEmail can't be null");
		Objects.requireNonNull(worksiteCode, "WorksiteCode can't be null");
		logger.debug("Clock-out ACTION performed");

		final Employee employee = findEmployee(employeeEmail);
		final Worksite worksite = findWorksite(worksiteCode);

		final TimeLog clockOut;
		if (exitTime != null) {
			clockOut = this.timeLogService.clockOut(employee, worksite, exitTime);
		} else {
			clockOut = this.timeLogService.clockOut(employee, worksite);
		}
		final TimeLogResponse timeLogResponse = this.timeLogResponseMapper.map(clockOut);
		return ResponseEntity.ok(timeLogResponse);
	}

	/**
	 * Creates a new time log entry for a specific employee and worksite.
	 *
	 * @param employeeEmail the email of the employee; must not be {@code null}
	 * @param worksiteCode  the code of the worksite where the time log is created;
	 *                      must not be {@code null}
	 * @param timeLog       the {@link CreateTimeLogRequest} payload containing the
	 *                      entry and exit times; must not be {@code null}
	 * @return the created {@link TimeLogResponse}
	 */
	@PostMapping("/timelogs")
	public ResponseEntity<TimeLogResponse> createTimeLog(final @PathVariable("employeeEmail") String employeeEmail,
			final @RequestParam("worksiteCode") String worksiteCode, final @RequestBody CreateTimeLogRequest timeLog) {
		Objects.requireNonNull(employeeEmail, "EmployeeEmail can't be null");
		Objects.requireNonNull(worksiteCode, "WorksiteCode can't be null");
		Objects.requireNonNull(timeLog, "TimeLog can't be null");
		logger.debug("Create timelog ACTION performed");

		final Employee employee = findEmployee(employeeEmail);
		final Worksite worksite = findWorksite(worksiteCode);

		final TimeLog createdTimeLog = this.timeLogService.createTimeLog(employee, worksite, timeLog);
		final TimeLogResponse updatedTimeLogResponse = this.timeLogResponseMapper.map(createdTimeLog);
		return ResponseEntity.ok(updatedTimeLogResponse);
	}

	/**
	 * Searches time logs for a given employee, optionally restricted to a date-time
	 * range.
	 * <p>
	 * If {@code fromInstant} and {@code toInstant} are omitted, all time logs are
	 * returned. Both must be provided together when filtering by range.
	 *
	 * @param employeeEmail the email of the employee; must not be {@code null}
	 * @param fromInstant   the start of the date-time range (inclusive); may be
	 *                      {@code null}
	 * @param toInstant     the end of the date-time range (inclusive); may be
	 *                      {@code null}
	 * @param pageable      pagination information; must not be {@code null}
	 * @return a page of {@link TimeLogResponse} entries
	 * @throws IllegalArgumentException if only one of {@code fromInstant} or
	 *                                  {@code toInstant} is provided
	 */
	@GetMapping("/")
	public ResponseEntity<Page<TimeLogResponse>> searchByEmployee(
			final @PathVariable("employeeEmail") String employeeEmail,
			final @RequestParam(value = "fromInstant", required = false) Instant fromInstant,
			final @RequestParam(value = "toInstant", required = false) Instant toInstant, final Pageable pageable) {
		Objects.requireNonNull(employeeEmail, "EmployeeEmail can't be null");
		if (Objects.isNull(fromInstant) ^ Objects.isNull(toInstant)) {
			throw new IllegalArgumentException("Both fromInstant and toInstant must be provided together or omitted.");
		}
		logger.debug("Search time logs by employee ACTION performed");

		final Employee employee = findEmployee(employeeEmail);

		final Page<TimeLog> timeLogs;
		if (fromInstant == null) {
			timeLogs = this.timeLogService.searchTimeLogsByEmployee(employee, pageable);
		} else {
			timeLogs = this.timeLogService.searchByEmployeeAndEntryTimeInRange(employee, fromInstant, toInstant,
					pageable);
		}
		final Page<TimeLogResponse> timeLogResponse = timeLogs.map(this.timeLogResponseMapper::map);
		return ResponseEntity.ok(timeLogResponse);
	}

	/**
	 * Finds a specific time log for an employee by its entry time.
	 *
	 * @param employeeEmail the email of the employee; must not be {@code null}
	 * @param entryTime     the entry time of the time log; must not be {@code null}
	 * @return the {@link TimeLogResponse} entry
	 */
	@GetMapping("/{entryTime}")
	public ResponseEntity<TimeLogResponse> findTimeLogByEmployeeAndEntryTime(
			final @PathVariable("employeeEmail") String employeeEmail,
			final @PathVariable("entryTime") Instant entryTime) {
		Objects.requireNonNull(employeeEmail, "EmployeeEmail can't be null");
		Objects.requireNonNull(entryTime, "EntryTime can't be null");
		logger.debug("Find time log by employee and entry time ACTION performed");

		final Employee employee = findEmployee(employeeEmail);

		final TimeLog timeLog = this.timeLogService.findTimeLogByEmployeeAndEntryTime(employee, entryTime);
		final TimeLogResponse timeLogResponse = this.timeLogResponseMapper.map(timeLog);
		return ResponseEntity.ok(timeLogResponse);
	}

	/**
	 * Retrieves the total duration worked for a given time log.
	 *
	 * @param employeeEmail the email of the employee; must not be {@code null}
	 * @param entryTime     the entry time of the time log; must not be {@code null}
	 * @return a {@link DurationResponse} containing hours, minutes, seconds, and
	 *         ISO-8601 representation
	 */
	@GetMapping("/{entryTime}/time-worked")
	public ResponseEntity<DurationResponse> getHoursWorked(final @PathVariable("employeeEmail") String employeeEmail,
			final @PathVariable("entryTime") Instant entryTime) {
		Objects.requireNonNull(employeeEmail, "EmployeeEmail can't be null");
		Objects.requireNonNull(entryTime, "EntryTime can't be null");
		logger.debug("Hours-worked ACTION performed");

		final Employee employee = findEmployee(employeeEmail);

		final TimeLog timeLog = this.timeLogService.findTimeLogByEmployeeAndEntryTime(employee, entryTime);
		timeLog.setEmployee(employee);
		final Duration duration = this.timeLogService.getTimeWorked(timeLog);
		DurationResponse response = new DurationResponse(duration.toHours(), duration.toMinutesPart(),
				duration.toSecondsPart(), duration.toString());
		return ResponseEntity.ok(response);
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
	@DeleteMapping("/{entryTime}")
	public ResponseEntity<Void> deleteTimeLog(final @PathVariable("employeeEmail") String employeeEmail,
			final @PathVariable("entryTime") Instant entryTime) {
		Objects.requireNonNull(employeeEmail, "EmployeeEmail can't be null");
		Objects.requireNonNull(entryTime, "EntryTime can't be null");
		logger.debug("Delete timelog ACTION performed");

		final Employee employee = findEmployee(employeeEmail);

		final TimeLog timeLog = timeLogService.findTimeLogByEmployeeAndEntryTime(employee, entryTime);
		this.timeLogService.deleteTimeLog(timeLog);
		return ResponseEntity.noContent().build();
	}
}
