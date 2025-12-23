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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.nivel36.janus.service.ResourceNotFoundException;
import es.nivel36.janus.service.admin.AdminService;
import es.nivel36.janus.service.employee.Employee;
import es.nivel36.janus.service.worksite.Worksite;

/**
 * Service responsible for managing {@link TimeLog} lifecycle operations.
 *
 * <p>
 * This service provides operations to create, update, search and delete time
 * logs associated with an {@link Employee} and a {@link Worksite}. It enforces
 * business rules related to time log creation and modification, such as
 * editability windows, prevention of duplicates, and handling clock-out
 * operations without a prior clock-in.
 * </p>
 *
 * <p>
 * All write operations are transactional to ensure data consistency. Read-only
 * operations are explicitly marked as such.
 * </p>
 */
@Service
public class TimeLogService {

	private static final Logger logger = LoggerFactory.getLogger(TimeLogService.class);

	private final TimeLogRepository timeLogRepository;
	private final ClockOutWithoutClockInEventRepository clockOutWithoutClockInEventRepository;
	private final AdminService adminService;
	private final Clock clock;

	/**
	 * Creates a new {@code TimeLogService} instance.
	 *
	 * @param timeLogRepository                     repository used to manage
	 *                                              {@link TimeLog} persistence.
	 *                                              Can't be {@code null}.
	 * @param clockOutWithoutClockInEventRepository repository used to store
	 *                                              {@link ClockOutWithoutClockInEvent}
	 *                                              instances. Can't be
	 *                                              {@code null}.
	 * @param adminService                          service providing administrative
	 *                                              configuration. Can't be
	 *                                              {@code null}.
	 * @param clock                                 clock used to retrieve the
	 *                                              current time. Can't be
	 *                                              {@code null}.
	 * @throws NullPointerException if any argument is {@code null}.
	 */
	public TimeLogService(final TimeLogRepository timeLogRepository,
			final ClockOutWithoutClockInEventRepository clockOutWithoutClockInEventRepository,
			final AdminService adminService, final Clock clock) {
		this.timeLogRepository = Objects.requireNonNull(timeLogRepository, "timeLogRepository can't be null");
		this.clockOutWithoutClockInEventRepository = Objects.requireNonNull(clockOutWithoutClockInEventRepository,
				"clockOutWithoutClockInEventRepository can't be null");
		this.adminService = Objects.requireNonNull(adminService, "adminService can't be null");
		this.clock = Objects.requireNonNull(clock, "clock can't be null");
	}

	/**
	 * Creates and persists a closed {@link TimeLog} with both entry and exit times.
	 *
	 * <p>
	 * The provided times are validated to ensure they are not in the future, fall
	 * within the editable window, and do not conflict with an existing
	 * {@link TimeLog} for the same employee and entry time.
	 * </p>
	 *
	 * @param employee  employee associated with the time log. Can't be
	 *                  {@code null}.
	 * @param worksite  worksite where the employee worked. Can't be {@code null}.
	 * @param entryTime entry time of the time log. Can't be {@code null}.
	 * @param exitTime  exit time of the time log. Can't be {@code null}.
	 * @return the persisted {@link TimeLog}.
	 * @throws NullPointerException                   if any argument is
	 *                                                {@code null}.
	 * @throws TimeLogModificationNotAllowedException if the time log cannot be
	 *                                                created due to business rules.
	 */
	@Transactional
	public TimeLog createTimeLog(final Employee employee, final Worksite worksite, final Instant entryTime,
			final Instant exitTime) {
		Objects.requireNonNull(employee, "employee cannot be null.");
		Objects.requireNonNull(worksite, "worksite cannot be null.");
		Objects.requireNonNull(entryTime, "entryTime request cannot be null.");
		Objects.requireNonNull(exitTime, "exitTime request cannot be null.");

		logger.debug("Creating closed time log for employee {} at worksite {} with entry time {} and exit time {}",
				employee, worksite, entryTime, exitTime);
		final Instant now = this.clock.instant();

		final Instant lockThreshold = this.getModificationLowerBound(now);
		final Instant truncatedEntryTime = entryTime.truncatedTo(ChronoUnit.SECONDS);
		this.assertWithinEditableWindow(truncatedEntryTime, lockThreshold, now);

		final Instant truncatedExitTime = exitTime.truncatedTo(ChronoUnit.SECONDS);
		this.assertWithinEditableWindow(truncatedExitTime, lockThreshold, now);

		this.assertTimeLogDoesNotExist(employee, truncatedEntryTime);

		final TimeLog newTimeLog = new TimeLog(employee, worksite, truncatedEntryTime, truncatedExitTime);
		final TimeLog persistedTimeLog = this.timeLogRepository.save(newTimeLog);
		logger.trace("Time log {} created successfully", persistedTimeLog.getId());
		return persistedTimeLog;
	}

	private void assertWithinEditableWindow(final Instant time, final Instant lockThreshold, final Instant now) {
		if (time.isAfter(now)) {
			throw new TimeLogModificationNotAllowedException("Time cannot be in the future");
		}
		if (!time.isAfter(lockThreshold)) {
			throw new TimeLogModificationNotAllowedException(
					String.format("Time %s is before lock threshold %s", time, lockThreshold));
		}
	}

	private void assertTimeLogDoesNotExist(final Employee employee, final Instant entryTime) {
		final boolean timeLogExists = this.timeLogRepository.existsByEmployeeAndEntryTimeAndDeletedFalse(employee,
				entryTime);
		if (timeLogExists) {
			throw new TimeLogModificationNotAllowedException(String
					.format("A time log with entryTime %s already exists for the employee %s.", entryTime, employee));
		}
	}

	private Instant getModificationLowerBound(final Instant now) {
		final int daysUntilLocked = this.adminService.getDaysUntilLocked();
		final Duration lockDuration = Duration.ofDays(daysUntilLocked);
		return now.minus(lockDuration);
	}

	/**
	 * Creates and persists an open {@link TimeLog} by clocking in an employee.
	 *
	 * <p>
	 * The entry time is validated to ensure it is not in the future, falls within
	 * the editable window, and does not conflict with an existing {@link TimeLog}
	 * for the same employee and entry time.
	 * </p>
	 *
	 * @param employee  employee clocking in. Can't be {@code null}.
	 * @param worksite  worksite where the employee is clocking in. Can't be
	 *                  {@code null}.
	 * @param entryTime entry time of the time log. Can't be {@code null}.
	 * @return the persisted open {@link TimeLog}.
	 * @throws NullPointerException                   if any argument is
	 *                                                {@code null}.
	 * @throws TimeLogModificationNotAllowedException if the time log cannot be
	 *                                                created.
	 */
	@Transactional
	public TimeLog clockIn(final Employee employee, final Worksite worksite, final Instant entryTime) {
		Objects.requireNonNull(employee, "employee cannot be null.");
		Objects.requireNonNull(worksite, "worksite cannot be null.");
		Objects.requireNonNull(entryTime, "entryTime request cannot be null.");

		logger.debug("Creating open time log for employee {} at worksite {} with entry time {}", employee, worksite,
				entryTime);
		final Instant now = this.clock.instant();

		final Instant lockThreshold = this.getModificationLowerBound(now);
		final Instant truncatedEntryTime = entryTime.truncatedTo(ChronoUnit.SECONDS);
		this.assertWithinEditableWindow(truncatedEntryTime, lockThreshold, now);
		this.assertTimeLogDoesNotExist(employee, truncatedEntryTime);

		final TimeLog newTimeLog = new TimeLog(employee, worksite, truncatedEntryTime);
		final TimeLog persistedTimeLog = this.timeLogRepository.save(newTimeLog);
		logger.trace("Time log {} created successfully", persistedTimeLog.getId());
		return persistedTimeLog;
	}

	/**
	 * Closes the most recent open {@link TimeLog} for the given employee and
	 * worksite.
	 *
	 * <p>
	 * If no open {@link TimeLog} exists, a {@link ClockOutWithoutClockInEvent} is
	 * recorded and a {@link ClockOutWithoutClockInException} is thrown.
	 * </p>
	 *
	 * @param employee employee clocking out. Can't be {@code null}.
	 * @param worksite worksite where the employee is clocking out. Can't be
	 *                 {@code null}.
	 * @param exitTime exit time to set on the open time log. Can't be {@code null}.
	 * @return the updated {@link TimeLog}.
	 * @throws NullPointerException                   if any argument is
	 *                                                {@code null}.
	 * @throws ClockOutWithoutClockInException        if no open time log exists.
	 * @throws TimeLogModificationNotAllowedException if the exit time is not
	 *                                                editable.
	 */
	@Transactional
	public TimeLog clockOut(final Employee employee, final Worksite worksite, final Instant exitTime)
			throws ClockOutWithoutClockInException {
		Objects.requireNonNull(employee, "employee cannot be null.");
		Objects.requireNonNull(worksite, "worksite cannot be null.");
		Objects.requireNonNull(exitTime, "exitTime request cannot be null.");

		final Instant truncatedExitTime = exitTime.truncatedTo(ChronoUnit.SECONDS);
		logger.debug("Closing time log for employee {} at worksite {} and time {}", employee, worksite,
				truncatedExitTime);

		final Instant now = this.clock.instant();
		final Instant lockThreshold = this.getModificationLowerBound(now);
		this.assertWithinEditableWindow(truncatedExitTime, lockThreshold, now);

		final TimeLog lastTimeLog = this.timeLogRepository
				.findTopByEmployeeAndWorksiteAndExitTimeIsNullOrderByEntryTimeDesc(employee, worksite);

		if (lastTimeLog == null) {
			final ClockOutWithoutClockInEvent clockOutWithoutClockInEvent = new ClockOutWithoutClockInEvent(employee,
					worksite, truncatedExitTime, now);
			this.clockOutWithoutClockInEventRepository.save(clockOutWithoutClockInEvent);
			throw new ClockOutWithoutClockInException();
		}

		lastTimeLog.close(truncatedExitTime);
		logger.trace("Exit time set to {} for last time log {}", truncatedExitTime, lastTimeLog.getId());
		return lastTimeLog;
	}

	/**
	 * Deletes the specified {@link TimeLog}.
	 *
	 * <p>
	 * Deletion is only allowed while the time log is still within the editable
	 * window defined by the administrative configuration.
	 * </p>
	 *
	 * @param timeLog time log to delete. Can't be {@code null}.
	 * @throws NullPointerException                   if {@code timeLog} is
	 *                                                {@code null}.
	 * @throws TimeLogModificationNotAllowedException if deletion is locked.
	 */
	@Transactional
	public void deleteTimeLog(final TimeLog timeLog) {
		Objects.requireNonNull(timeLog, "timeLog cannot be null.");
		logger.debug("Deleting time log {}", timeLog.getId());

		final Instant now = this.clock.instant();
		final Duration lockDuration = Duration.ofDays(this.adminService.getDaysUntilLocked());

		if (!timeLog.getEntryTime().plus(lockDuration).isAfter(now)) {
			throw new TimeLogModificationNotAllowedException(
					String.format("Deletion locked for TimeLog %s with entryTime %s after %s days. Now: %s",
							timeLog.getId(), timeLog.getEntryTime(), lockDuration.toDays(), now));
		}

		this.timeLogRepository.delete(timeLog);
		logger.trace("Time log {} deleted", timeLog.getId());
	}

	/**
	 * Finds a {@link TimeLog} by employee and entry time.
	 *
	 * @param employee  employee associated with the time log. Can't be
	 *                  {@code null}.
	 * @param entryTime entry time of the time log. Can't be {@code null}.
	 * @return the matching {@link TimeLog}.
	 * @throws NullPointerException      if any argument is {@code null}.
	 * @throws ResourceNotFoundException if no matching time log is found.
	 */
	@Transactional(readOnly = true)
	public TimeLog findTimeLogByEmployeeAndEntryTime(final Employee employee, final Instant entryTime) {
		Objects.requireNonNull(employee, "employee can't be null");
		Objects.requireNonNull(entryTime, "entryTime can't be null");
		logger.debug("Finding time log by employee {} and entry time {}", employee, entryTime);

		final TimeLog timeLog = this.timeLogRepository.findByEmployeeAndEntryTime(employee, entryTime);
		if (timeLog == null) {
			throw new ResourceNotFoundException(
					String.format("TimeLog for employee %s at entry time %s was not found", employee, entryTime));
		}
		return timeLog;
	}

	/**
	 * Finds orphan {@link TimeLog} instances for an employee since a given instant.
	 *
	 * <p>
	 * An orphan time log is a log that is not properly paired or finalized
	 * according to business rules.
	 * </p>
	 *
	 * @param from     lower bound instant for the search. Can't be {@code null}.
	 * @param employee employee for whom orphan time logs are searched. Can't be
	 *                 {@code null}.
	 * @return a list of orphan {@link TimeLog} instances. Never {@code null}.
	 * @throws NullPointerException if any argument is {@code null}.
	 */
	@Transactional(readOnly = true)
	public TimeLogs findOrphanTimeLogs(final Instant from, final Employee employee) {
		Objects.requireNonNull(from, "from must not be null");
		Objects.requireNonNull(employee, "employee must not be null");
		logger.debug("Finding orphan timeLog from {} and employee {}", from, employee);

		final List<TimeLog> orphanTimeLogs = this.timeLogRepository.findOrphanTimeLogsSince(from, employee);
		logger.trace("Found {} orphan time logs", orphanTimeLogs.size());
		return new TimeLogs(orphanTimeLogs);
	}

	/**
	 * Searches {@link TimeLog} instances for a given employee using pagination.
	 *
	 * @param employee employee whose time logs are searched. Can't be {@code null}.
	 * @param page     pagination information. Can't be {@code null}.
	 * @return a {@link Page} of {@link TimeLog} instances.
	 * @throws NullPointerException if any argument is {@code null}.
	 */
	@Transactional(readOnly = true)
	public Page<TimeLog> searchTimeLogsByEmployee(final Employee employee, final Pageable page) {
		Objects.requireNonNull(employee, "employee can't be null.");
		Objects.requireNonNull(page, "page can't be null.");
		logger.debug("Finding time logs for employee {} with offset {} and page size {}", employee, page.getOffset(),
				page.getPageSize());

		final Page<TimeLog> timeLogs = this.timeLogRepository.searchTimeLogsByEmployee(employee, page);
		logger.trace("Found {} time logs", timeLogs.getTotalElements());
		return timeLogs;
	}

	/**
	 * Searches {@link TimeLog} instances for a given employee whose entry time
	 * falls within the specified range.
	 *
	 * @param employee    employee whose time logs are searched. Can't be
	 *                    {@code null}.
	 * @param fromInstant inclusive lower bound of the entry time range. Can't be
	 *                    {@code null}.
	 * @param toInstant   exclusive upper bound of the entry time range. Can't be
	 *                    {@code null}.
	 * @param page        pagination information. Can't be {@code null}.
	 * @return a {@link Page} of {@link TimeLog} instances within the given range.
	 * @throws NullPointerException if any argument is {@code null}.
	 */
	@Transactional(readOnly = true)
	public Page<TimeLog> searchTimeLogsByEmployeeAndEntryTimeInRange(final Employee employee, final Instant fromInstant,
			final Instant toInstant, final Pageable page) {
		Objects.requireNonNull(employee, "employee cannot be null.");
		Objects.requireNonNull(fromInstant, "fromInstant cannot be null.");
		Objects.requireNonNull(toInstant, "toInstant cannot be null.");
		Objects.requireNonNull(page, "page cannot be null.");
		logger.debug("Finding time logs for employee {} in range [{}, {})", employee, fromInstant, toInstant);

		final Page<TimeLog> timeLogs = this.timeLogRepository.searchByEmployeeAndEntryTimeInRange(employee, fromInstant,
				toInstant, page);
		logger.trace("Found {} time logs", timeLogs.getTotalElements());
		return timeLogs;
	}
}
