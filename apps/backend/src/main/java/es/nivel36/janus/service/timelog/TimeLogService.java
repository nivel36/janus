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
 * distributed under this License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package es.nivel36.janus.service.timelog;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import es.nivel36.janus.service.ResourceNotFoundException;
import es.nivel36.janus.service.applicationsettings.ApplicationSettingsService;
import es.nivel36.janus.service.employee.Employee;
import es.nivel36.janus.service.worksite.Worksite;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Service responsible for managing {@link TimeLog} lifecycle operations.
 * <p>
 * This service provides operations to create, update, search and delete time
 * logs associated with an {@link Employee} and a {@link Worksite}. It enforces
 * business rules related to time log creation and modification, such as
 * editability windows, prevention of duplicates, and handling clock-out
 * operations without a prior clock-in.
 * </p>
 * <p>
 * All write operations are transactional to ensure data consistency. Read-only
 * operations are explicitly marked as such.
 * </p>
 */
@Validated
@Service
public class TimeLogService {

	private static final Logger logger = LoggerFactory.getLogger(TimeLogService.class);

	private final TimeLogRepository timeLogRepository;
	private final ClockOutWithoutClockInEventRepository clockOutWithoutClockInEventRepository;
	private final ApplicationSettingsService applicationSettingsService;
	private final Clock clock;
	private final int maxPageSize;

	/**
	 * Creates a new {@code TimeLogService} instance.
	 *
	 * @param  timeLogRepository                     repository used to manage
	 *                                               {@link TimeLog} persistence.
	 *                                               Can't be {@code null}.
	 * @param  clockOutWithoutClockInEventRepository repository used to store
	 *                                               {@link ClockOutWithoutClockInEvent}
	 *                                               instances. Can't be
	 *                                               {@code null}.
	 * @param  applicationSettingsService            service providing
	 *                                               administrative configuration.
	 *                                               Can't be {@code null}.
	 * @param  clock                                 clock used to retrieve the
	 *                                               current time. Can't be
	 *                                               {@code null}.
	 * @param  maxPageSize                           positive configured page-size
	 *                                               limit
	 * @throws IllegalArgumentException              if maxPageSize is not positive
	 * @throws NullPointerException                  if any argument is
	 *                                               {@code null}.
	 */
	public TimeLogService(
		final TimeLogRepository timeLogRepository,
		final ClockOutWithoutClockInEventRepository clockOutWithoutClockInEventRepository,
		final ApplicationSettingsService applicationSettingsService,
		final Clock clock,
		final @Value("${spring.data.rest.max-page-size}") int maxPageSize) {
		this.timeLogRepository = Objects.requireNonNull(timeLogRepository, "timeLogRepository can't be null");
		this.clockOutWithoutClockInEventRepository = Objects.requireNonNull(
				clockOutWithoutClockInEventRepository,
				"clockOutWithoutClockInEventRepository can't be null");
		this.applicationSettingsService = Objects
				.requireNonNull(applicationSettingsService, "applicationSettingsService can't be null");
		this.clock = Objects.requireNonNull(clock, "clock can't be null");
		if (maxPageSize < 1) {
			throw new IllegalArgumentException("maxPageSize must be positive");
		}
		this.maxPageSize = maxPageSize;
	}

	/**
	 * Creates and persists a closed {@link TimeLog} with both entry and exit times.
	 * <p>
	 * The provided times are validated to ensure they are not in the future, fall
	 * within the editable window, and do not conflict with an existing
	 * {@link TimeLog} for the same employee and entry time.
	 * </p>
	 *
	 * @param  employee                               employee associated with the
	 *                                                time log. Can't be
	 *                                                {@code null}.
	 * @param  worksite                               worksite where the employee
	 *                                                worked. Can't be {@code null}.
	 * @param  entryTime                              entry time of the time log.
	 *                                                Can't be {@code null}.
	 * @param  exitTime                               exit time of the time log.
	 *                                                Can't be {@code null}.
	 * @return                                        the persisted {@link TimeLog}.
	 * @throws ConstraintViolationException           if a required argument is null
	 *                                                through the Spring proxy
	 * @throws NullPointerException                   if any argument is null on a
	 *                                                direct call
	 * @throws TimeLogModificationNotAllowedException if the time log cannot be
	 *                                                created due to business rules.
	 * @throws TimeLogFutureTimeException             if a truncated time is in the
	 *                                                future
	 * @throws TimeLogChronologyException             if truncated exit is not
	 *                                                strictly after entry
	 */
	@Transactional
	public TimeLog createTimeLog(
			final @NotNull Employee employee,
			final @NotNull Worksite worksite,
			final @NotNull Instant entryTime,
			final @NotNull Instant exitTime) {
		Objects.requireNonNull(employee, "employee cannot be null.");
		Objects.requireNonNull(worksite, "worksite cannot be null.");
		Objects.requireNonNull(entryTime, "entryTime request cannot be null.");
		Objects.requireNonNull(exitTime, "exitTime request cannot be null.");

		logger.debug(
				"Creating closed time log for employee {} at worksite {} with entry time {} and exit time {}",
				employee.getId(),
				worksite.getCode(),
				entryTime,
				exitTime);
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
			throw new TimeLogFutureTimeException();
		}
		if (!time.isAfter(lockThreshold)) {
			throw new TimeLogModificationNotAllowedException(
					String.format("Time %s is before lock threshold %s", time, lockThreshold));
		}
	}

	private void assertTimeLogDoesNotExist(final Employee employee, final Instant entryTime) {
		final boolean timeLogExists = this.timeLogRepository
				.existsByEmployeeIdAndEntryTimeAndDeletedFalse(employee.getId(), entryTime);
		if (timeLogExists) {
			throw new TimeLogModificationNotAllowedException(
					String.format(
							"A time log with entryTime %s already exists for the employee %s.",
							entryTime,
							employee));
		}
	}

	private Instant getModificationLowerBound(final Instant now) {
		final int daysUntilLocked = this.applicationSettingsService.getDaysUntilLocked();
		final Duration lockDuration = Duration.ofDays(daysUntilLocked);
		return now.minus(lockDuration);
	}

	/**
	 * Creates and persists an open {@link TimeLog} by clocking in an employee.
	 * <p>
	 * The entry time is validated to ensure it is not in the future, falls within
	 * the editable window, and does not conflict with an existing {@link TimeLog}
	 * for the same employee and entry time.
	 * </p>
	 *
	 * @param  employee                               employee clocking in. Can't be
	 *                                                {@code null}.
	 * @param  worksite                               worksite where the employee is
	 *                                                clocking in. Can't be
	 *                                                {@code null}.
	 * @param  entryTime                              entry time of the time log.
	 *                                                Can't be {@code null}.
	 * @return                                        the persisted open
	 *                                                {@link TimeLog}.
	 * @throws ConstraintViolationException           if a required argument is null
	 *                                                through the Spring proxy
	 * @throws NullPointerException                   if any argument is
	 *                                                {@code null}.
	 * @throws TimeLogModificationNotAllowedException if the time log cannot be
	 *                                                created.
	 * @throws TimeLogFutureTimeException             if truncated entry time is in
	 *                                                the future
	 */
	@Transactional
	public TimeLog clockIn(
			final @NotNull Employee employee,
			final @NotNull Worksite worksite,
			final @NotNull Instant entryTime) {
		Objects.requireNonNull(employee, "employee cannot be null.");
		Objects.requireNonNull(worksite, "worksite cannot be null.");
		Objects.requireNonNull(entryTime, "entryTime request cannot be null.");

		logger.debug(
				"Creating open time log for employee {} at worksite {} with entry time {}",
				employee.getId(),
				worksite.getCode(),
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
	 * Indicates whether the employee currently has an active open {@link TimeLog}.
	 *
	 * @param  employee the employee to inspect; must not be {@code null}.
	 * @return          {@code true} when an open time log exists for the employee ;
	 *                  {@code false} otherwise.
	 */
	@Transactional(readOnly = true)
	public boolean hasOpenTimeLog(final @NotNull Employee employee) {
		Objects.requireNonNull(employee, "employee cannot be null.");

		return this.timeLogRepository
				.findTopByEmployeeIdAndExitTimeIsNullOrderByEntryTimeDesc(employee.getId()) != null;
	}

	/**
	 * Closes the most recent open {@link TimeLog} for the given employee. The
	 * worksite argument is checked against the open log when worksite changes are
	 * disabled.
	 * <p>
	 * If no open {@link TimeLog} exists, a {@link ClockOutWithoutClockInEvent} is
	 * recorded and a {@link ClockOutWithoutClockInException} is thrown.
	 * </p>
	 *
	 * @param  employee                               employee clocking out. Can't
	 *                                                be {@code null}.
	 * @param  worksite                               worksite where the employee is
	 *                                                clocking out. Can't be
	 *                                                {@code null}.
	 * @param  exitTime                               exit time to set on the open
	 *                                                time log. Can't be
	 *                                                {@code null}.
	 * @return                                        the updated {@link TimeLog}.
	 * @throws ConstraintViolationException           if a required argument is null
	 *                                                through the Spring proxy
	 * @throws NullPointerException                   if any argument is
	 *                                                {@code null}.
	 * @throws ClockOutWithoutClockInException        if no open time log exists.
	 * @throws TimeLogModificationNotAllowedException if the exit time is not
	 *                                                editable.
	 * @throws TimeLogFutureTimeException             if truncated exit time is in
	 *                                                the future
	 * @throws TimeLogChronologyException             if truncated exit is not
	 *                                                strictly after entry
	 * @throws WorksiteMismatchOnClockOutException    if worksite differs and
	 *                                                changes are disabled
	 */
	@Transactional(noRollbackFor = ClockOutWithoutClockInException.class)
	public TimeLog clockOut(
			final @NotNull Employee employee,
			final @NotNull Worksite worksite,
			final @NotNull Instant exitTime) throws ClockOutWithoutClockInException {
		Objects.requireNonNull(employee, "employee cannot be null.");
		Objects.requireNonNull(worksite, "worksite cannot be null.");
		Objects.requireNonNull(exitTime, "exitTime request cannot be null.");

		final Instant truncatedExitTime = exitTime.truncatedTo(ChronoUnit.SECONDS);
		logger.debug(
				"Closing time log for employee {} at worksite {} and time {}",
				employee.getId(),
				worksite.getCode(),
				truncatedExitTime);

		final Instant now = this.clock.instant();
		final Instant lockThreshold = this.getModificationLowerBound(now);
		this.assertWithinEditableWindow(truncatedExitTime, lockThreshold, now);

		final TimeLog lastTimeLog = this.timeLogRepository
				.findTopByEmployeeIdAndExitTimeIsNullOrderByEntryTimeDesc(employee.getId());

		if (lastTimeLog == null) {
			final ClockOutWithoutClockInEvent clockOutWithoutClockInEvent = new ClockOutWithoutClockInEvent(
					employee,
					worksite,
					truncatedExitTime,
					now);
			this.clockOutWithoutClockInEventRepository.save(clockOutWithoutClockInEvent);
			throw new ClockOutWithoutClockInException();
		}

		if (!this.applicationSettingsService.isWorksiteChangeDuringShiftAllowed()
				&& !lastTimeLog.getWorksite().equals(worksite)) {
			throw new WorksiteMismatchOnClockOutException(lastTimeLog.getWorksite(), worksite);
		}

		lastTimeLog.close(truncatedExitTime);
		logger.trace("Exit time set to {} for last time log {}", truncatedExitTime, lastTimeLog.getId());
		return lastTimeLog;
	}

	/**
	 * Deletes the specified {@link TimeLog}.
	 * <p>
	 * Deletion is only allowed while the time log is still within the editable
	 * window defined by the administrative configuration.
	 * </p>
	 *
	 * @param  timeLog                                time log to delete. Can't be
	 *                                                {@code null}.
	 * @throws ConstraintViolationException           if a required argument is null
	 *                                                through the Spring proxy
	 * @throws NullPointerException                   if {@code timeLog} is
	 *                                                {@code null}.
	 * @throws TimeLogModificationNotAllowedException if deletion is locked.
	 */
	@Transactional
	public void deleteTimeLog(final @NotNull TimeLog timeLog) {
		Objects.requireNonNull(timeLog, "timeLog cannot be null.");
		logger.debug("Deleting time log {}", timeLog.getId());

		final Instant now = this.clock.instant();
		final Duration lockDuration = Duration.ofDays(this.applicationSettingsService.getDaysUntilLocked());

		if (!timeLog.getEntryTime().plus(lockDuration).isAfter(now)) {
			throw new TimeLogModificationNotAllowedException(
					String.format(
							"Deletion locked for TimeLog %s with entryTime %s after %s days. Now: %s",
							timeLog.getId(),
							timeLog.getEntryTime(),
							lockDuration.toDays(),
							now));
		}

		this.timeLogRepository.delete(timeLog);
		logger.trace("Time log {} deleted", timeLog.getId());
	}

	/**
	 * Finds a {@link TimeLog} by employee and entry time.
	 *
	 * @param  employee                     employee associated with the time log.
	 *                                      Can't be {@code null}.
	 * @param  entryTime                    entry time of the time log. Can't be
	 *                                      {@code null}.
	 * @return                              the matching {@link TimeLog}.
	 * @throws ConstraintViolationException if a required argument is null through
	 *                                      the Spring proxy
	 * @throws NullPointerException         if any argument is {@code null}.
	 * @throws ResourceNotFoundException    if no matching time log is found.
	 */
	@Transactional(readOnly = true)
	public TimeLog findTimeLogByEmployeeAndEntryTime(
			final @NotNull Employee employee,
			final @NotNull Instant entryTime) {
		Objects.requireNonNull(employee, "employee can't be null");
		Objects.requireNonNull(entryTime, "entryTime can't be null");
		logger.debug("Finding time log by employee {} and entry time {}", employee.getId(), entryTime);

		final TimeLog timeLog = this.timeLogRepository.findByEmployeeIdAndEntryTime(employee.getId(), entryTime);
		if (timeLog == null) {
			throw new ResourceNotFoundException(
					String.format("TimeLog for employee %s at entry time %s was not found", employee, entryTime));
		}
		return timeLog;
	}

	/**
	 * Finds orphan {@link TimeLog} instances for an employee since a given instant.
	 * <p>
	 * An orphan time log is a log that is not properly paired or finalized
	 * according to business rules.
	 * </p>
	 *
	 * @param  employee                     employee for whom orphan time logs are
	 *                                      searched. Can't be {@code null}.
	 * @param  from                         lower bound instant for the search.
	 *                                      Can't be {@code null}.
	 * @return                              a list of orphan {@link TimeLog}
	 *                                      instances. Never {@code null}.
	 * @throws ConstraintViolationException if a required argument is null through
	 *                                      the Spring proxy
	 * @throws NullPointerException         if any argument is {@code null}.
	 */
	@Transactional(readOnly = true)
	public TimeLogs findOrphanTimeLogs(final @NotNull Employee employee, final @NotNull Instant from) {
		Objects.requireNonNull(from, "from must not be null");
		Objects.requireNonNull(employee, "employee must not be null");
		logger.debug("Finding orphan timeLog from {} and employee {}", from, employee.getId());

		final Long employeeId = employee.getId();
		final List<TimeLog> orphanTimeLogs = this.timeLogRepository.findOrphanTimeLogsSince(employeeId, from);
		logger.trace("Found {} orphan time logs", orphanTimeLogs.size());
		return new TimeLogs(orphanTimeLogs);
	}

	/**
	 * Searches active time logs within a mandatory authorized scope.
	 * <p>
	 * The caller must authorize the search and supply nonnull criteria, scope and a
	 * paged request. Employee numbers must satisfy their declared constraint. Start
	 * and end must both be omitted or supplied with start strictly before end.
	 * Filters combine with AND, including scope, before pagination and counting.
	 * Employee number matching is exact without trimming; the entry-time range
	 * includes start and excludes end. Deleted logs are omitted.
	 * </p>
	 * <p>
	 * Page size is capped at configured spring.data.rest.max-page-size. Results
	 * default to descending entry time, with ascending id as a tie-breaker unless
	 * explicitly sorted. Public sort fields are id, entryTime, exitTime,
	 * employeeNumber and worksiteCode. This operation changes no records.
	 * </p>
	 *
	 * @param  criteria                     optional client filters, independent of
	 *                                      authorization
	 * @param  scope                        mandatory authorized records
	 * @param  pageable                     requested page and public ordering
	 * @return                              page of matching, authorized time logs,
	 *                                      possibly empty
	 * @throws ConstraintViolationException if a required argument is null or the
	 *                                      employee number is invalid through the
	 *                                      Spring proxy
	 * @throws NullPointerException         if criteria, scope or pageable is null
	 *                                      on a direct call
	 * @throws IllegalArgumentException     if the range is incomplete or
	 *                                      nonincreasing, paging is unpaged or a
	 *                                      sort field is unsupported
	 */
	@Transactional(readOnly = true)
	public Page<TimeLog> searchTimeLogs(
			final @NotNull @Valid TimeLogSearchCriteria criteria,
			final @NotNull TimeLogSearchScope scope,
			final @NotNull Pageable pageable) {
		Objects.requireNonNull(criteria, "criteria can't be null");
		Objects.requireNonNull(scope, "scope can't be null");
		Objects.requireNonNull(pageable, "pageable can't be null");
		if ((criteria.start() == null) != (criteria.end() == null)
				|| (criteria.start() != null && !criteria.start().isBefore(criteria.end()))) {
			throw new IllegalArgumentException("start and end must be provided together and end must be after start");
		}
		final Pageable normalizedPageable = this.normalizePageable(pageable);
		logger.atDebug().addKeyValue("employeeNumber", criteria.employeeNumber()).addKeyValue("start", criteria.start())
				.addKeyValue("end", criteria.end()).addKeyValue("scope", scope)
				.addKeyValue("page", normalizedPageable.getPageNumber())
				.addKeyValue("pageSize", normalizedPageable.getPageSize())
				.addKeyValue("sort", normalizedPageable.getSort().toString()).log("Searching time logs");

		final Specification<TimeLog> searchCriteria = TimeLogSearchSpecifications.matching(criteria);
		final Specification<TimeLog> searchCriteriaWithScope = TimeLogSearchSpecifications.within(scope)
				.and(searchCriteria);
		return this.timeLogRepository.findAll(searchCriteriaWithScope, normalizedPageable);
	}

	private Pageable normalizePageable(final Pageable pageable) {
		if (pageable.isUnpaged()) {
			throw new IllegalArgumentException("Must be paged");
		}
		final List<Order> orders = new ArrayList<>();
		for (final Order order : pageable.getSort()) {
			final String property = switch (order.getProperty()) {
			case "id", "entryTime", "exitTime" -> order.getProperty();
			case "employeeNumber" -> "employee.employeeNumber";
			case "worksiteCode" -> "worksite.code";
			default -> throw new IllegalArgumentException("Unsupported TimeLog sort field: " + order.getProperty());
			};
			orders.add(order.withProperty(property));
		}
		if (orders.isEmpty()) {
			orders.add(Order.desc("entryTime"));
		}
		if (orders.stream().noneMatch(order -> "id".equals(order.getProperty()))) {
			orders.add(Order.asc("id"));
		}
		return PageRequest
				.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), this.maxPageSize), Sort.by(orders));
	}
}
