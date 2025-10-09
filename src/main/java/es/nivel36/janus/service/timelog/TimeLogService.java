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
 * distributed under this License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package es.nivel36.janus.service.timelog;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
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

import es.nivel36.janus.api.v1.timelog.CreateTimeLogRequest;
import es.nivel36.janus.service.ResourceNotFoundException;
import es.nivel36.janus.service.admin.AdminService;
import es.nivel36.janus.service.employee.Employee;
import es.nivel36.janus.service.worksite.Worksite;

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
	 * Clocks in an {@link Employee} at the current time for the given
	 * {@link Worksite}.
	 *
	 * @param employee the employee to clock in; must not be {@code null}
	 * @param worksite the worksite where the employee is clocking in; must not be
	 *                 {@code null}
	 * @return the created {@link TimeLog} entry with the current entry time
	 * @throws NullPointerException if {@code employee} or {@code worksite} is
	 *                              {@code null}
	 */
	@Transactional
	public TimeLog clockIn(final Employee employee, final Worksite worksite) {
		return this.clockIn(employee, worksite, clock.instant());
	}

	/**
	 * Clocks in an {@link Employee} at the specified entry time for the given
	 * {@link Worksite}.
	 *
	 * <p>
	 * The provided {@code entryTime} is truncated to seconds.
	 * 
	 * @param employee  the employee to clock in; must not be {@code null}
	 * @param worksite  the worksite where the employee is clocking in; must not be
	 *                  {@code null}
	 * @param entryTime the specific time for clocking in; must not be {@code null}
	 * @return the created {@link TimeLog} with the provided entry time
	 * @throws NullPointerException if {@code employee}, {@code worksite} or
	 *                              {@code entryTime} is {@code null}
	 */
	@Transactional
	public TimeLog clockIn(final Employee employee, final Worksite worksite, final Instant entryTime) {
		Objects.requireNonNull(employee, "Employee can't be null.");
		Objects.requireNonNull(worksite, "Worksite can't be null.");
		Objects.requireNonNull(entryTime, "Entry time can't be null.");
		final Instant truncatedEntryTime = entryTime.truncatedTo(ChronoUnit.SECONDS);

		logger.debug("Clocking in employee {} at worksite {} and time {}", employee, worksite, truncatedEntryTime);

		final TimeLog timeLog = new TimeLog(employee, worksite, truncatedEntryTime);
		this.timeLogRepository.save(timeLog);
		return timeLog;
	}

	/**
	 * Clocks out an {@link Employee} at the current time for the given
	 * {@link Worksite}.
	 *
	 * <p>
	 * If a previous {@link TimeLog} for the {@code employee}/{@code worksite}
	 * cannot be found, the method constructs a synthetic one-second {@link TimeLog}
	 * with {@code entryTime = exitTime - 1s} and {@code exitTime = exitTime}.
	 *
	 * @param employee the employee to clock out; must not be {@code null}
	 * @param worksite the worksite where the employee is clocking out; must not be
	 *                 {@code null}
	 * @return the last {@link TimeLog} for the employee/worksite with
	 *         {@code exitTime} set. If none existed, returns the synthetic
	 *         one-second {@link TimeLog}.
	 *
	 * @throws NullPointerException if {@code employee} or {@code worksite} is
	 *                              {@code null}
	 */
	@Transactional
	public TimeLog clockOut(final Employee employee, final Worksite worksite) {
		return this.clockOut(employee, worksite, clock.instant());
	}

	/**
	 * Clocks out an {@link Employee} at the specified exit time for the given
	 * {@link Worksite}.
	 *
	 * <p>
	 * The provided {@code exitTime} is truncated to seconds.<br>
	 * If a previous {@link TimeLog} for the {@code employee}/{@code worksite}
	 * cannot be found, the method constructs a synthetic one-second {@link TimeLog}
	 * with {@code entryTime = exitTime - 1s} and {@code exitTime = exitTime}.
	 *
	 * @param employee the employee to clock out; must not be {@code null}
	 * @param worksite the worksite where the employee is clocking out; must not be
	 *                 {@code null}
	 * @param exitTime the specific time for clocking out; must not be {@code null}
	 * @return the last {@link TimeLog} for the employee/worksite with
	 *         {@code exitTime} set. If none existed, returns the synthetic
	 *         one-second {@link TimeLog}.
	 *
	 * @throws NullPointerException if {@code employee}, {@code worksite} or
	 *                              {@code exitTime} is {@code null}
	 *
	 */
	@Transactional
	public TimeLog clockOut(final Employee employee, final Worksite worksite, Instant exitTime) {
		Objects.requireNonNull(employee, "Employee can't be null.");
		Objects.requireNonNull(worksite, "Worksite can't be null.");
		Objects.requireNonNull(exitTime, "Exit time can't be null.");
		exitTime = exitTime.truncatedTo(ChronoUnit.SECONDS);

		logger.debug("Clocking out employee {} at worksite {} and time {}", employee, worksite, exitTime);

		TimeLog lastTimeLog = this.timeLogRepository.findTopByEmployeeAndWorksiteOrderByEntryTimeDesc(employee,
				worksite);
		if (lastTimeLog == null) {
			logger.warn("The employee did not clock in. A one-second timelog is created.");
			lastTimeLog = new TimeLog(employee, worksite, exitTime.minusSeconds(1), exitTime);
		}

		lastTimeLog.setExitTime(exitTime);
		this.timeLogRepository.save(lastTimeLog);
		logger.trace("Exit time set to {} for last TimeLog {}", exitTime, lastTimeLog.getId());
		return lastTimeLog;
	}

	/**
	 * Calculates the total time worked for a given {@link TimeLog}. If the employee
	 * hasn't clocked out yet, the duration is from entry time to {@code now}.
	 *
	 * @param timeLog the time log entry; must not be {@code null}
	 * @return a {@link Duration} representing the total time worked
	 * @throws NullPointerException if {@code timeLog} is {@code null}
	 */
	public Duration getTimeWorked(final TimeLog timeLog) {
		Objects.requireNonNull(timeLog, "TimeLog can't be null.");
		logger.debug("Calculating worked duration for TimeLog {}", timeLog.getId());

		if (timeLog.getExitTime() == null) {
			final Duration duration = Duration.between(timeLog.getEntryTime(), this.clock.instant());
			logger.trace("TimeLog {} has no exit time. Duration until now: {}", timeLog.getId(), duration);
			return duration;
		} else {
			final Duration duration = Duration.between(timeLog.getEntryTime(), timeLog.getExitTime());
			logger.trace("TimeLog {} has exit time {}. Duration: {}", timeLog.getId(), timeLog.getExitTime(), duration);
			return duration;
		}
	}

	/**
	 * Finds a {@link TimeLog} by employee and exact entry time.
	 *
	 * @param employee  the employee; must not be {@code null}
	 * @param entryTime the exact entry time; must not be {@code null}
	 * @return the {@link TimeLog} found
	 * @throws NullPointerException      if {@code employee} or {@code entryTime} is
	 *                                   {@code null}
	 * @throws ResourceNotFoundException if no time log exists for the given pair
	 */
	@Transactional(readOnly = true)
	public TimeLog findTimeLogByEmployeeAndEntryTime(final Employee employee, final Instant entryTime) {
		Objects.requireNonNull(employee, "Employee can't be null");
		Objects.requireNonNull(entryTime, "Entry time can't be null");

		logger.debug("Finding TimeLog by employee {} and entryTime {}", employee, entryTime);

		final TimeLog timeLog = this.timeLogRepository.findByEmployeeAndEntryTime(employee, entryTime);
		if (timeLog == null) {
			throw new ResourceNotFoundException(
					String.format("TimeLog for employee %s at entry time %s was not found", employee, entryTime));
		}
		return timeLog;
	}

	/**
	 * Finds the last time log for the specified employee at a given worksite.
	 *
	 * @param employee the employee; must not be {@code null}
	 * @param worksite the worksite; must not be {@code null}
	 * @return an {@link Optional} with the last {@link TimeLog} for the
	 *         employee/worksite
	 * @throws NullPointerException if {@code employee} or {@code worksite} is
	 *                              {@code null}
	 */
	@Transactional(readOnly = true)
	public TimeLog findLastTimeLogByEmployee(final Employee employee, final Worksite worksite) {
		Objects.requireNonNull(employee, "Employee can't be null.");
		Objects.requireNonNull(worksite, "Worksite can't be null.");
		logger.debug("Finding last TimeLog for employee {} and worksite {}", employee, worksite);

		return this.timeLogRepository.findTopByEmployeeAndWorksiteOrderByEntryTimeDesc(employee, worksite);
	}

	/**
	 * Finds the list of {@link TimeLog} entries for the given employee that are
	 * considered "orphans" since the specified instant; i.e., time logs that are
	 * not associated with any {@link WorkShift}.
	 * <p>
	 * The results are ordered by {@code entry_time} in descending order (most
	 * recent first).
	 * 
	 * @param from     the lower bound (inclusive) instant; only time logs with
	 *                 {@code entryTime >= from} are considered
	 * @param employee the employee whose orphan time logs will be retrieved
	 * @return a list of orphan {@link TimeLog} entities for the specified employee
	 *         since the given instant
	 * @throws NullPointerException if {@code from} or {@code employeeId} is
	 *                              {@code null}
	 */
	@Transactional(readOnly = true)
	public List<TimeLog> findOrphanTimeLogs(final Instant from, final Employee employee) {
		Objects.requireNonNull(from, "From must not be null");
		Objects.requireNonNull(employee, "Employee must not be null");
		logger.debug("Finding orphan TimeLog from date {} and employee {}", from, employee);

		return timeLogRepository.findOrphanTimeLogsSince(from, employee);
	}

	/**
	 * Finds a paginated list of time logs for the specified employee.
	 *
	 * @param employee the employee; must not be {@code null}
	 * @param page     the {@link Pageable} to control pagination/sorting; must not
	 *                 be {@code null}
	 * @return a {@link Page} of {@link TimeLog} entries for the employee
	 * @throws NullPointerException if {@code employee} or {@code page} is
	 *                              {@code null}
	 */
	@Transactional(readOnly = true)
	public Page<TimeLog> searchTimeLogsByEmployee(final Employee employee, final Pageable page) {
		Objects.requireNonNull(employee, "Employee can't be null.");
		Objects.requireNonNull(page, "Page can't be null.");

		logger.debug("Finding TimeLogs for employee {} with offset {} and pageSize {}", employee, page.getOffset(),
				page.getPageSize());
		return this.timeLogRepository.searchTimeLogsByEmployee(employee, page);
	}

	/**
	 * Retrieves time logs for an {@link Employee} {@link TimeLog} records for the
	 * specified employee whose {@code entryTime} falls within the given time range.
	 * <p>
	 * The {@code start} parameter is inclusive; records with
	 * {@code entryTime &gt;= start} are included.<br>
	 * The {@code end} parameter is exclusive; records with
	 * {@code entryTime &lt; end} are included.
	 *
	 * @param employee    the employee whose time logs are to be retrieved; must not
	 *                    be {@code null}
	 * @param fromInstant the inclusive lower bound of the time range; must not be
	 *                    {@code null}
	 * @param toInstant   the exclusive upper bound of the time range; must not be
	 *                    {@code null}
	 * @param page        pagination parameters including offset, size, and sort
	 *                    order; must not be {@code null}
	 * @return a {@link Page} of {@link TimeLog} entries in the range
	 * @throws NullPointerException if any parameter is {@code null}
	 */
	@Transactional(readOnly = true)
	public Page<TimeLog> searchByEmployeeAndEntryTimeInRange(final Employee employee, final Instant fromInstant,
			final Instant toInstant, final Pageable page) {
		Objects.requireNonNull(employee, "Employee cannot be null.");
		Objects.requireNonNull(fromInstant, "fromInstant cannot be null.");
		Objects.requireNonNull(toInstant, "toInstant cannot be null.");
		Objects.requireNonNull(page, "Page cannot be null.");

		logger.debug("Finding TimeLogs for employee {} in range [{}, {})", employee, fromInstant, toInstant);

		return this.timeLogRepository.searchByEmployeeAndEntryTimeInRange(employee, fromInstant, toInstant, page);
	}

	/**
	 * Creates a {@link TimeLog} with the values provided in the request.
	 * <p>
	 * Business rules:
	 * <ul>
	 * <li>Both {@code entryTime} and {@code exitTime} must be provided.</li>
	 * <li>Editing window: each timestamp must be within {@code daysUntilLocked}
	 * days from {@code now} (i.e., {@code newValue + daysUntilLocked > now}).</li>
	 * <li>Chronology: {@code entryTime} must be strictly before
	 * {@code exitTime}.</li>
	 * <li>Uniqueness: there must not already exist a log for the same employee and
	 * {@code entryTime}.</li>
	 * </ul>
	 *
	 * @param employee the employee owner of the new log; must not be {@code null}
	 * @param worksite the worksite related to the new log; must not be {@code null}
	 * @param request  the creation payload; must not be {@code null}
	 * @return the persisted {@link TimeLog}
	 * @throws NullPointerException                   if {@code employee},
	 *                                                {@code worksite} or
	 *                                                {@code request} is
	 *                                                {@code null}, or if
	 *                                                {@code entryTime} or
	 *                                                {@code exitTime} are
	 *                                                {@code null}
	 * @throws TimeLogModificationNotAllowedException if the editing window is
	 *                                                violated
	 * @throws TimeLogChronologyException             if
	 *                                                {@code entryTime >= exitTime}
	 */
	@Transactional
	public TimeLog createTimeLog(final Employee employee, final Worksite worksite, final CreateTimeLogRequest request) {
		Objects.requireNonNull(employee, "Employee cannot be null.");
		Objects.requireNonNull(worksite, "Worksite cannot be null.");
		Objects.requireNonNull(request, "TimeLog request cannot be null.");

		logger.debug("Creating TimeLog for employee {} at worksite {} with request {}", employee, worksite, request);

		final Instant now = clock.instant();
		final Duration lockDuration = Duration.ofDays(adminService.getDaysUntilLocked());

		final Instant newEntry = request.entryTime();
		final Instant newExit = request.exitTime();

		if (newEntry == null || newExit == null) {
			throw new NullPointerException("Both entryTime and exitTime must be provided.");
		}

		if (!newEntry.plus(lockDuration).isAfter(now)) {
			throw new TimeLogModificationNotAllowedException(String.format(
					"Creation locked for entryTime %s after %s days. Now: %s", newEntry, lockDuration.toDays(), now));
		}

		if (!newExit.plus(lockDuration).isAfter(now)) {
			throw new TimeLogModificationNotAllowedException(String.format(
					"Creation locked for exitTime %s after %s days. Now: %s", newExit, lockDuration.toDays(), now));
		}

		if (!newEntry.isBefore(newExit)) {
			throw new TimeLogChronologyException(
					String.format("entryTime %s must be strictly before exitTime %s.", newEntry, newExit));
		}

		final boolean timeLogExists = this.timeLogRepository.existsByEmployeeAndEntryTime(employee, newEntry);
		if (timeLogExists) {
			throw new TimeLogModificationNotAllowedException(
					String.format("A time log with entryTime %s already exists for the employee.", newEntry));
		}

		final TimeLog newTimeLog = new TimeLog(employee, worksite, newEntry, newExit);

		return timeLogRepository.save(newTimeLog);
	}

	/**
	 * Deletes a {@link TimeLog}.
	 * <p>
	 * Business rule:
	 * <ul>
	 * <li>Deletion allowed only if {@code entryTime + daysUntilLocked > now}.</li>
	 * </ul>
	 *
	 * @param timeLog the time log to delete; must not be {@code null}
	 * @throws NullPointerException                   if {@code timeLog} is
	 *                                                {@code null}
	 * @throws TimeLogModificationNotAllowedException if the editing window is
	 *                                                violated
	 */
	@Transactional
	public void deleteTimeLog(final TimeLog timeLog) {
		Objects.requireNonNull(timeLog, "TimeLog cannot be null.");
		logger.debug("Deleting TimeLog {}", timeLog.getId());

		final Instant now = clock.instant();
		final Duration lockDuration = Duration.ofDays(adminService.getDaysUntilLocked());

		if (!timeLog.getEntryTime().plus(lockDuration).isAfter(now)) {
			throw new TimeLogModificationNotAllowedException(
					String.format("Deletion locked for TimeLog %s with entryTime %s after %s days. Now: %s",
							timeLog.getId(), timeLog.getEntryTime(), lockDuration.toDays(), now));
		}

		this.timeLogRepository.delete(timeLog);
		logger.trace("TimeLog {} deleted", timeLog.getId());
	}
}
