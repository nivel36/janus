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
package es.nivel36.janus.api;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.nivel36.janus.service.employee.Employee;
import es.nivel36.janus.service.employee.EmployeeService;
import es.nivel36.janus.service.timelog.TimeLog;
import es.nivel36.janus.service.timelog.TimeLogService;

/**
 * REST controller responsible for exposing time log operations.
 */
@RestController
@RequestMapping("/api/v1/timelogs")
public class TimeLogController {

	private static final Logger logger = LoggerFactory.getLogger(TimeLogController.class);

	private final TimeLogService timeLogService;
	private final EmployeeService employeeService;
	private final Mapper<TimeLog, TimeLogResponse> timeLogResponseMapper;

	public TimeLogController(final TimeLogService timeLogService, final EmployeeService employeeService,
			final Mapper<TimeLog, TimeLogResponse> timeLogResponseMapper) {
		this.timeLogService = Objects.requireNonNull(timeLogService, "TimeLogService can't be null");
		this.employeeService = Objects.requireNonNull(employeeService, "EmployeeService can't be null");
		this.timeLogResponseMapper = Objects.requireNonNull(timeLogResponseMapper,
				"TimeLogResponseMapper can't be null");
	}

	/**
	 * Clocks in an employee.
	 *
	 * @param employeeEmail the email of the employee
	 * @return the created {@link TimeLog} entry
	 */
	@PostMapping("/clock-in")
	public ResponseEntity<TimeLogResponse> clockIn(final String employeeEmail) {
		logger.debug("Clock-in ACTION performed");
		Objects.requireNonNull(employeeEmail, "EmployeeEmail can't be null");
		final Employee employee = this.employeeService.findEmployeeByEmail(employeeEmail);
		if (employee == null) {
			logger.warn("No employee found with email: {}", employeeEmail);
			return ResponseEntity.notFound().build();
		}
		final TimeLog clockIn = this.timeLogService.clockIn(employee);
		final TimeLogResponse timeLog = this.timeLogResponseMapper.map(clockIn);
		return ResponseEntity.status(HttpStatus.CREATED).body(timeLog);
	}

	/**
	 * Clocks in an employee at a specified entry time.
	 *
	 * @param employeeEmail the email of the employee
	 * @param entryTime     the entry time as ISO-8601 string (e.g.,
	 *                      "2025-08-04T09:30:00")
	 * @return the created {@link TimeLogResponse} entry
	 */
	@PostMapping("/clock-in-at")
	public ResponseEntity<TimeLogResponse> clockInAt(final String employeeEmail, final String entryTime) {
		logger.debug("Clock-in-at ACTION performed");
		final Employee employee = this.employeeService.findEmployeeByEmail(employeeEmail);
		if (employee == null) {
			logger.warn("No employee found with email: {}", employeeEmail);
			return ResponseEntity.notFound().build();
		}
		final LocalDateTime entry = LocalDateTime.parse(entryTime);
		final TimeLog timeLog = this.timeLogService.clockIn(employee, entry);
		final TimeLogResponse timeLogResponse = this.timeLogResponseMapper.map(timeLog);
		return ResponseEntity.status(HttpStatus.CREATED).body(timeLogResponse);
	}

	/**
	 * Clocks out an employee.
	 *
	 * @param employeeEmail the email of the employee
	 * @return the updated {@link TimeLogResponse} entry
	 */
	@PostMapping("/clock-out")
	public ResponseEntity<TimeLogResponse> clockOut(final String employeeEmail) {
		logger.debug("Clock-out ACTION performed");
		final Employee employee = this.employeeService.findEmployeeByEmail(employeeEmail);
		if (employee == null) {
			logger.warn("No employee found with email: {}", employeeEmail);
			return ResponseEntity.notFound().build();
		}
		final TimeLog timeLog = this.timeLogService.clockOut(employee);
		final TimeLogResponse timeLogResponse = this.timeLogResponseMapper.map(timeLog);
		return ResponseEntity.ok(timeLogResponse);
	}

	/**
	 * Clocks out an employee at a specified exit time.
	 *
	 * @param employeeEmail the email of the employee
	 * @param exitTime      the exit time as ISO-8601 string (e.g.,
	 *                      "2025-08-04T17:15:00")
	 * @return the updated {@link TimeLogResponse} entry
	 */
	@PostMapping("/clock-out-at")
	public ResponseEntity<TimeLogResponse> clockOutAt(final String employeeEmail, final String exitTime) {
		logger.debug("Clock-out-at ACTION performed");
		final Employee employee = this.employeeService.findEmployeeByEmail(employeeEmail);
		if (employee == null) {
			logger.warn("No employee found with email: {}", employeeEmail);
			return ResponseEntity.notFound().build();
		}
		final LocalDateTime exit = LocalDateTime.parse(exitTime);
		final TimeLog timeLog = this.timeLogService.clockOut(employee, exit);
		final TimeLogResponse timeLogResponse = this.timeLogResponseMapper.map(timeLog);
		return ResponseEntity.ok(timeLogResponse);
	}

	/**
	 * Retrieves the total hours worked for a specific time log.
	 *
	 * @param id the id of the time log
	 * @return a string representation of the duration (ISO-8601, e.g. PT8H30M)
	 */
	@GetMapping("/{id}/hours-worked")
	public ResponseEntity<String> getHoursWorked(final long id) {
		logger.debug("Hours-worked ACTION performed");
		final TimeLog timeLog = this.timeLogService.findTimeLogById(id);
		final Duration duration = this.timeLogService.getHoursWorked(timeLog);
		return ResponseEntity.ok(duration.toString());
	}

	/**
	 * Finds paginated time logs for an employee.
	 *
	 * @param employeeEmail the email of the employee
	 * @param pageable      the pagination information (page, size, sort)
	 * @return a page of {@link TimeLogResponse} entries
	 */
	@GetMapping("/employee")
	public ResponseEntity<Page<TimeLogResponse>> getTimeLogsByEmployee(final String employeeEmail,
			final Pageable pageable) {
		final Employee employee = employeeService.findEmployeeByEmail(employeeEmail);
		if (employee == null) {
			logger.warn("No employee found with email: {}", employeeEmail);
			return ResponseEntity.notFound().build();
		}
		final Page<TimeLogResponse> timeLogs = this.timeLogService.findTimeLogsByEmployee(employee, pageable)
				.map(this.timeLogResponseMapper::map);
		return ResponseEntity.ok(timeLogs);
	}

	/**
	 * Finds time logs for an employee on and around a specific date.
	 *
	 * @param employeeEmail the email of the employee
	 * @param date          the date as a string in ISO-8601 format (yyyy-MM-dd)
	 * @param pageable      the pagination information (page, size, sort)
	 * @return a page of {@link TimeLogResponse} entries
	 */
	@GetMapping("/employee/by-date")
	public ResponseEntity<Page<TimeLogResponse>> findTimeLogsByEmployeeAndDate(final String employeeEmail,
			final String date, final Pageable pageable) {
		final Employee employee = this.employeeService.findEmployeeByEmail(employeeEmail);
		if (employee == null) {
			logger.warn("No employee found with email: {}", employeeEmail);
			return ResponseEntity.notFound().build();
		}
		final LocalDate localDate = LocalDate.parse(date);
		final Page<TimeLogResponse> logs = this.timeLogService
				.findTimeLogsByEmployeeAndDate(employee, localDate, pageable).map(this.timeLogResponseMapper::map);
		return ResponseEntity.ok(logs);
	}

	/**
	 * Finds a time log by its id.
	 *
	 * @param id the id of the time log
	 * @return the {@link TimeLogResponse} entry
	 */
	@GetMapping("/{id}")
	public ResponseEntity<TimeLogResponse> findTimeLogById(final long id) {
		final TimeLog timeLog = this.timeLogService.findTimeLogById(id);
		final TimeLogResponse timeLogResponse = this.timeLogResponseMapper.map(timeLog);
		return ResponseEntity.ok(timeLogResponse);
	}

	/**
	 * Updates a time log entry.
	 *
	 * @param id      the id of the time log to update
	 * @param timeLog the updated time log in the request body
	 * @return the updated {@link TimeLogResponse}
	 */
	@PutMapping("/{id}")
	public ResponseEntity<TimeLogResponse> updateTimeLog(final long id, final UpdateTimeLogRequest timeLog) {
		final TimeLog updatedTimeLog = this.timeLogService.updateTimeLog(id, timeLog);
		final TimeLogResponse updatedTimeLogResponse = this.timeLogResponseMapper.map(updatedTimeLog);
		return ResponseEntity.ok(updatedTimeLogResponse);
	}

	/**
	 * Deletes a time log entry.
	 *
	 * @param id the id of the time log to delete
	 * @return a {@link ResponseEntity} with no content
	 */
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteTimeLog(final long id) {
		final TimeLog timeLog = timeLogService.findTimeLogById(id);
		this.timeLogService.deleteTimeLog(timeLog);
		return ResponseEntity.noContent().build();
	}
}
