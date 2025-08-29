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
package es.nivel36.janus.service.timelog;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.nivel36.janus.api.timelog.UpdateTimeLogRequest;
import es.nivel36.janus.service.admin.AdminService;
import es.nivel36.janus.service.employee.Employee;
import jakarta.persistence.EntityNotFoundException;

/**
 * Service class responsible for managing clock-in and clock-out operations for
 * employees and interacting with the {@link TimeLogRepository} to handle
 * persistence.
 */
@Service
public class TimeLogService {

	private static final Logger logger = LoggerFactory.getLogger(TimeLogService.class);

	private final TimeLogRepository timeLogRepository;
	private final AdminService adminService;
	private final Clock clock;

	public TimeLogService(final TimeLogRepository timeLogRepository, final AdminService adminService,
			final Clock clock) {
		this.timeLogRepository = Objects.requireNonNull(timeLogRepository, "TimeLogRepository can't be null");
		this.adminService = Objects.requireNonNull(adminService, "AdminService can't be null");
		this.clock = Objects.requireNonNull(clock, "Clock can't be null");
	}

	/**
	 * Clocks in an employee at the current time.
	 *
	 * @param employee the employee to clock in
	 * @return the created {@link TimeLog} entry with the current entry time
	 * @throws NullPointerException if the employee is {@code null}
	 */
	@Transactional
	public TimeLog clockIn(final Employee employee) {
		return this.clockIn(employee, LocalDateTime.now());
	}

	/**
	 * Clocks in an employee at the specified entry time.
	 *
	 * @param employee  the employee to clock in
	 * @param entryTime the specific time for clocking in
	 * @return the created {@link TimeLog} entry with the provided entry time
	 * @throws NullPointerException if the employee or entryTime is {@code null}
	 */
	@Transactional
	public TimeLog clockIn(final Employee employee, LocalDateTime entryTime) {
		Objects.requireNonNull(employee, "Employee can't be null.");
		Objects.requireNonNull(entryTime, "Entry time can't be null.");
		entryTime = entryTime.truncatedTo(ChronoUnit.SECONDS);
		logger.debug("Clocking in employee: {} at {}", employee, entryTime);
		final TimeLog timeLog = new TimeLog(employee, entryTime);
		this.timeLogRepository.save(timeLog);
		return timeLog;
	}

	/**
	 * Clocks out an employee at the current time.
	 *
	 * @param employee the employee to clock out
	 * @return the updated {@link TimeLog} with the exit time set to now
	 * @throws NullPointerException if the employee is {@code null}
	 */
	@Transactional
	public TimeLog clockOut(final Employee employee) {
		return this.clockOut(employee, LocalDateTime.now());
	}

	/**
	 * Clocks out an employee at the specified exit time.
	 *
	 * @param employee the employee to clock out
	 * @param exitTime the specific time for clocking out
	 * @return the updated {@link TimeLog} with the provided exit time
	 * @throws NullPointerException  if the employee or exitTime is {@code null}
	 * @throws IllegalStateException if there is no {@link TimeLog} for the employee
	 */
	@Transactional
	public TimeLog clockOut(final Employee employee, LocalDateTime exitTime) {
		Objects.requireNonNull(employee, "Employee can't be null.");
		Objects.requireNonNull(exitTime, "Exit time can't be null.");
		exitTime = exitTime.truncatedTo(ChronoUnit.SECONDS);
		logger.debug("Clocking out employee: {} at {}", employee, exitTime);

		final Optional<TimeLog> lastTimeLogOpt = this.timeLogRepository.findLastTimeLogByEmployee(employee);

		// Check if TimeLog is present, otherwise throw an IllegalStateException
		final TimeLog lastTimeLog = lastTimeLogOpt.orElseThrow(
				() -> new IllegalStateException(String.format("No time log found for the employee %s", employee)));

		lastTimeLog.setExitTime(exitTime);

		return lastTimeLog;
	}

	/**
	 * Calculates the total hours worked for a given time log. If the employee
	 * hasn't clocked out yet, the duration will be calculated from the entry time
	 * to the current time.
	 *
	 * @param timeLog the {@link TimeLog} entry containing the clock-in and
	 *                clock-out times
	 * @return a {@link Duration} representing the total hours worked
	 * @throws NullPointerException if the timeLog is {@code null}
	 */
	public Duration getHoursWorked(final TimeLog timeLog) {
		Objects.requireNonNull(timeLog, "TimeLog can't be null.");
		logger.debug("Calculating hours worked for TimeLog: {}", timeLog);

		if (timeLog.getExitTime() == null) {
			final Duration duration = Duration.between(timeLog.getEntryTime(), LocalDateTime.now());
			logger.trace("TimeLog has no exit time. Calculating duration until now: {}", duration);
			return duration;
		} else {
			final Duration duration = Duration.between(timeLog.getEntryTime(), timeLog.getExitTime());
			logger.trace("TimeLog has exit time. Calculating duration between entry and exit: {}", duration);
			return duration;
		}
	}

	/**
	 * Finds a time log by its primary key (id).
	 *
	 * @param id the ID of the time log to be found
	 * @return the {@link TimeLog} with the specified id, or {@code null} if not
	 *         found
	 * @throws NullPointerException    if the id is null
	 * @throws EntityNotFoundException if Time log does not exist.
	 */
	@Transactional(readOnly = true)
	public TimeLog findTimeLogById(final Long id) {
		Objects.requireNonNull(id, "Id can't be null");
		logger.debug("Finding TimeLog by id: {}", id);
		return this.timeLogRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException(String.format("There is no TimeLog with id %s", id)));
	}

	/**
	 * Finds the last time log for the specified employee.
	 *
	 * @param employee the employee whose last time log is to be found
	 * @return the last {@link TimeLog} for the employee
	 * @throws NullPointerException if the employee is {@code null}
	 */
	@Transactional(readOnly = true)
	public Optional<TimeLog> findLastTimeLogByEmployee(final Employee employee) {
		Objects.requireNonNull(employee, "Employee can't be null.");
		logger.debug("Finding last TimeLog for employee: {}", employee);
		return this.timeLogRepository.findLastTimeLogByEmployee(employee);
	}

	/**
	 * Finds a list of time logs for the specified employee with pagination.
	 *
	 * @param employee      the employee whose time logs are to be found
	 * @param startPosition the initial position of the search from which to start
	 *                      returning values.
	 * @param pageSize      the size of each page
	 * @return a list of {@link TimeLog} entries for the employee
	 * @throws NullPointerException     if the employee is {@code null}
	 * @throws IllegalArgumentException if the page is negative or the pageSize is
	 *                                  less than 1
	 */
	@Transactional(readOnly = true)
	public Page<TimeLog> findTimeLogsByEmployee(final Employee employee, final Pageable page) {
		Objects.requireNonNull(employee, "Employee can't be null.");
		Objects.requireNonNull(page, "Page can't be null.");

		logger.debug("Finding TimeLogs for employee: {} with startPosition: {}, pageSize: {}", employee,
				page.getOffset(), page.getPageSize());
		return this.timeLogRepository.findTimeLogsByEmployee(employee, page);
	}

	/**
	 * Retrieves all {@link TimeLog} entries for a given {@link Employee} within a
	 * date range centered around the specified date.
	 *
	 * <p>
	 * This method searches for all time logs for the specified employee within a
	 * 3-day range: the day before the given date, the day of the date, and the day
	 * after the date. This can be useful when the exact time entry may span across
	 * multiple days or if there are related time logs around the specified date.
	 * </p>
	 *
	 * @param employee the employee whose time logs are to be retrieved. Cannot be
	 *                 {@code null}.
	 * @param date     the date for which the time logs are to be searched, along
	 *                 with the adjacent days. Cannot be {@code null}.
	 * @return a list of {@link TimeLog} entries for the specified employee and date
	 *         range.
	 * @throws NullPointerException if the {@code employee} or {@code date} is
	 *                              {@code null}.
	 */
	@Transactional(readOnly = true)
	public List<TimeLog> findTimeLogsByEmployeeAndDate(final Employee employee, final LocalDate date) {
		Objects.requireNonNull(employee, "Employee cannot be null.");
		Objects.requireNonNull(date, "Date cannot be null.");

		logger.debug("Finding TimeLogs for employee: {} with date: {}", employee, date);
		return this.timeLogRepository.findTimeLogsByEmployeeAndDateRange(employee, date.minusDays(1).atStartOfDay(),
				date.plusDays(1).atStartOfDay());
	}

	/**
	 * Updates a {@link TimeLog} with the values provided in the request.
	 * <p>
	 * Applied business rules:
	 * <ul>
	 * <li>A timestamp can only be modified if it is within the editing window
	 * (newValue + daysUntilLocked &gt; now).</li>
	 * <li>If both values are provided, temporal consistency is validated (entryTime
	 * ≤ exitTime).</li>
	 * </ul>
	 *
	 * @param id      identifier of the record to be updated
	 * @param request values to update (optional fields)
	 * @return the persisted entity after the update
	 * @throws NullPointerException                   if request is null or
	 *                                                entryTime and exitTime are
	 *                                                null
	 * @throws EntityNotFoundException                if Time log does not exist.
	 * @throws TimeLogModificationNotAllowedException if the editing window is
	 *                                                violated
	 * @throws TimeLogChronologyException             if entryTime &gt; exitTime
	 */
	@Transactional
	public TimeLog updateTimeLog(final Long id, final UpdateTimeLogRequest request) {
		Objects.requireNonNull(request, "TimeLog request cannot be null.");
		logger.debug("Updating TimeLog with id: {}", id);
		final TimeLog entity = findTimeLogById(id);
		final LocalDateTime now = LocalDateTime.now(clock);
		final Period lockPeriod = Period.ofDays(adminService.getDaysUntilLocked());

		final LocalDateTime newEntry = request.entryTime().orElse(null);
		final LocalDateTime newExit = request.exitTime().orElse(null);

		if (newEntry == null && newExit == null) {
			throw new NullPointerException("TimeLog request cannot be null.");
		}

		if (newEntry != null && !newEntry.plus(lockPeriod).isAfter(now)) {
			throw new TimeLogModificationNotAllowedException(String.format(
					"Modification of entry time %s is locked after %s. Current time: %s", newEntry, lockPeriod, now));
		}

		if (newExit != null && !newExit.plus(lockPeriod).isAfter(now)) {
			throw new TimeLogModificationNotAllowedException(String.format(
					"Modification of exit time %s is locked after %s. Current time: %s", newExit, lockPeriod, now));
		}

		if (newEntry != null && newExit != null) {
			assertChronology(newEntry, newExit);
		}
		if (newEntry != null && entity.getExitTime() != null) {
			assertChronology(newEntry, entity.getExitTime());
		}
		if (newExit != null && entity.getEntryTime() != null) {
			assertChronology(entity.getEntryTime(), newExit);
		}

		if (newEntry != null) {
			entity.setEntryTime(newEntry);
		}
		if (newExit != null) {
			entity.setExitTime(newExit);
		}

		return timeLogRepository.save(entity);
	}

	private static void assertChronology(final LocalDateTime entry, final LocalDateTime exit) {
		if (entry.isAfter(exit)) {
			throw new TimeLogChronologyException(
					String.format("Invalid chronology: entryTime %s is after exitTime %s.", entry, exit));
		}
	}

	/**
	 * Deletes a {@link TimeLog} entry.
	 * <p>
	 * Applied business rules:
	 * <ul>
	 * <li>A time log can only be deleted if it is still within the editing window
	 * (entryTime + daysUntilLocked &gt; now).</li>
	 * <li>If the editing window has expired, a
	 * {@link TimeLogModificationNotAllowedException} is thrown.</li>
	 * </ul>
	 *
	 * @param timeLog the {@link TimeLog} instance to be deleted, must not be
	 *                {@code null}
	 * @throws NullPointerException                   if the time log is
	 *                                                {@code null}
	 * @throws TimeLogModificationNotAllowedException if the editing window is
	 *                                                violated
	 */
	@Transactional
	public void deleteTimeLog(final TimeLog timeLog) {
		Objects.requireNonNull(timeLog, "TimeLog cannot be null.");
		logger.debug("Deleting TimeLog: {}", timeLog);

		final LocalDateTime now = LocalDateTime.now(clock);
		final Period lockPeriod = Period.ofDays(adminService.getDaysUntilLocked());

		if (!timeLog.getEntryTime().plus(lockPeriod).isAfter(now)) {
			throw new TimeLogModificationNotAllowedException(
					String.format("Deletion of TimeLog with entryTime %s is locked after %s. Current time: %s",
							timeLog.getEntryTime(), timeLog.getEntryTime().plus(lockPeriod), now));
		}

		this.timeLogRepository.delete(timeLog);
	}
}
