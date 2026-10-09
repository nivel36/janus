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
package es.nivel36.janus.service.workshift;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import es.nivel36.janus.service.applicationsettings.ApplicationSettingsService;
import es.nivel36.janus.service.employee.Employee;
import es.nivel36.janus.service.employee.EmployeeService;
import es.nivel36.janus.service.schedule.ScheduleService;
import es.nivel36.janus.service.schedule.TimeRange;
import es.nivel36.janus.service.timelog.TimeLog;
import es.nivel36.janus.service.timelog.TimeLogService;
import es.nivel36.janus.service.timelog.TimeLogs;
import es.nivel36.janus.service.worksite.Worksite;

/**
 * Scheduled precomputation of {@link WorkShift} summaries from unassigned logs.
 * <p>
 * Eligible logs are closed, nondeleted records whose entry time is at or after
 * the computed cutoff. Selected logs are associated with persisted shifts, with
 * local dates determined by worksite zones and overnight schedules.
 */
@Component
public class WorkShiftPrecomputeJob {

	private static final Logger log = LoggerFactory.getLogger(WorkShiftPrecomputeJob.class);

	private final WorkshiftRepository workshiftRepository;
	private final TimeLogService timeLogService;
	private final ScheduleService scheduleService;
	private final EmployeeService employeeService;
	private final ApplicationSettingsService applicationSettingsService;
	private final Clock clock;
	private final ShiftPolicy policy;

	/**
	 * Constructs the scheduled job that materializes historical work-shift
	 * summaries.
	 *
	 * @param  workshiftRepository        repository that persists {@link WorkShift}
	 *                                    aggregates; never {@code null}
	 * @param  timeLogService             service that queries {@link TimeLog} data;
	 *                                    never {@code null}
	 * @param  employeeService            service that provides employees pending
	 *                                    precomputation; never {@code null}
	 * @param  scheduleService            Service used to obtain scheduled time
	 *                                    ranges. Must not be {@code null}.
	 * @param  applicationSettingsService service that provides admin policies
	 *                                    (e.g., locking horizon); never
	 *                                    {@code null}
	 * @param  clock                      clock used to derive the target anchor;
	 *                                    never {@code null}
	 * @throws NullPointerException       if any argument is {@code null}
	 */
	public WorkShiftPrecomputeJob(
		final WorkshiftRepository workshiftRepository,
		final TimeLogService timeLogService,
		final ScheduleService scheduleService,
		final EmployeeService employeeService,
		final ApplicationSettingsService applicationSettingsService,
		final Clock clock) {
		this.workshiftRepository = Objects.requireNonNull(workshiftRepository, "workshiftRepository must not be null");
		this.timeLogService = Objects.requireNonNull(timeLogService, "timeLogService must not be null");
		this.scheduleService = Objects.requireNonNull(scheduleService, "scheduleService must not be null");
		this.employeeService = Objects.requireNonNull(employeeService, "employeeService must not be null");
		this.applicationSettingsService = Objects
				.requireNonNull(applicationSettingsService, "applicationSettingsService must not be null");
		this.clock = Objects.requireNonNull(clock, "clock must not be null");
		this.policy = ShiftPolicy.defaultPolicy();
	}

	/**
	 * Persists shifts from closed, unassigned time logs at or after the cutoff.
	 * <p>
	 * The cutoff is the current instant minus the configured modification window
	 * plus one additional day. Consecutive eligible logs are grouped by worksite
	 * and local shift date. An early-morning entry within the previous day's
	 * scheduled overnight range is assigned to that previous date.
	 * <p>
	 * The job runs daily at {@code 02:15} in the scheduler's default time zone.
	 * Selected logs are associated with the persisted shift, whose work and pause
	 * totals cover the selected recorded intervals.
	 */
	@Scheduled(cron = "0 15 2 * * *")
	@Transactional
	public void run() {
		final int daysUntilLocked = this.applicationSettingsService.getDaysUntilLocked();
		final Instant target = this.clock.instant().minus(daysUntilLocked + 1L, ChronoUnit.DAYS);
		log.debug("WorkShift precompute started; daysUntilLocked={} targetAnchor={}", daysUntilLocked, target);

		final List<Long> employeeIds = this.employeeService.findEmployeesWithoutWorkshiftsSince(target);
		log.trace("Pending employees count={}", employeeIds.size());
		if (employeeIds.isEmpty()) {
			return;
		}

		for (final Long employeeId : employeeIds) {
			this.processEmployee(employeeId, target);
		}
		log.debug("WorkShift precompute finished");
	}

	private void processEmployee(final Long employeeId, final Instant target) {
		final Employee employee = this.employeeService.findEmployeeById(employeeId);
		log.trace("Processing employee {}", employee);

		final TimeLogs orphanLogs = this.timeLogService.findOrphanTimeLogs(employee, target);
		if (orphanLogs.isEmpty()) {
			log.warn("No orphan time logs for employee {} at targetAnchor {}", employee, target);
			return;
		}

		final Deque<TimeLog> queue = new ArrayDeque<>(orphanLogs.asList());
		while (!queue.isEmpty()) {
			this.buildAndSaveNextWorkShift(employee, queue);
		}
	}

	private void buildAndSaveNextWorkShift(final Employee employee, final Deque<TimeLog> queue) {
		final TimeLog first = queue.removeFirst();

		final Worksite worksite = first.getWorksite();
		final ZoneId zone = worksite.getTimeZone();
		final Instant firstEntry = first.getEntryTime();

		final LocalDate entryDay = firstEntry.atZone(zone).toLocalDate();
		final LocalTime entryTime = firstEntry.atZone(zone).toLocalTime();
		final LocalDate previousDay = entryDay.minusDays(1);
		final Optional<TimeRange> previousTimeRange = this.scheduleService
				.findTimeRangeForEmployeeByDate(employee, previousDay);
		final boolean belongsToPreviousOvernightShift = previousTimeRange.filter(WorkShiftPrecomputeJob::isOvernight)
				.map(range -> entryTime.isBefore(range.getEndTime())).orElse(false);

		final LocalDate day = belongsToPreviousOvernightShift ? previousDay : entryDay;
		final Optional<TimeRange> timeRange = belongsToPreviousOvernightShift ? previousTimeRange
				: this.scheduleService.findTimeRangeForEmployeeByDate(employee, day);
		final Instant dayStart = day.atStartOfDay(zone).toInstant();
		final Instant dayEndExclusive = timeRange.filter(WorkShiftPrecomputeJob::isOvernight)
				.map(range -> day.plusDays(1).atTime(range.getEndTime()).atZone(zone).toInstant())
				.orElseGet(() -> day.plusDays(1).atStartOfDay(zone).toInstant());

		log.trace(
				"Bucket employee={}, worksite={}, zone={}, day={} window=[{} .. {})",
				employee,
				worksite,
				zone,
				day,
				dayStart,
				dayEndExclusive);

		final TimeLogs bucket = this.collectBucket(first, worksite, dayStart, dayEndExclusive, queue);

		final ShiftInferenceStrategyResolver resolver = new ShiftInferenceStrategyResolver();
		final ShiftInferenceStrategy strategy = resolver.resolve(timeRange, zone, this.policy);
		final WorkShift workShift = new WorkShiftComposer(strategy).compose(employee, day, bucket);
		final WorkShift saved = this.workshiftRepository.save(workShift);
		log.trace("WorkShift persisted with id {}", saved.getId());
	}

	private static boolean isOvernight(final TimeRange timeRange) {
		return timeRange.getEndTime().isBefore(timeRange.getStartTime());
	}

	private TimeLogs collectBucket(
			final TimeLog first,
			final Worksite worksite,
			final Instant dayStart,
			final Instant dayEndExclusive,
			final Deque<TimeLog> queue) {

		final List<TimeLog> bucket = new ArrayList<>();
		bucket.add(first);

		while (!queue.isEmpty()) {
			final TimeLog next = queue.peekFirst();
			final Instant entry = next.getEntryTime();
			// Entry can't be null
			final boolean isOutOfScopeWorksite = !Objects.equals(next.getWorksite(), worksite);
			final boolean isOutsideDayWindow = entry.isBefore(dayStart) || !entry.isBefore(dayEndExclusive);
			if (isOutOfScopeWorksite || isOutsideDayWindow) {
				break;
			}
			bucket.add(queue.removeFirst());
		}

		log.trace("Collected {} time logs in bucket", bucket.size());
		return new TimeLogs(bucket);
	}
}
