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
package es.nivel36.janus.service.workshift;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
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

import es.nivel36.janus.service.admin.AdminService;
import es.nivel36.janus.service.employee.Employee;
import es.nivel36.janus.service.employee.EmployeeService;
import es.nivel36.janus.service.schedule.ScheduleService;
import es.nivel36.janus.service.schedule.TimeRange;
import es.nivel36.janus.service.timelog.TimeLog;
import es.nivel36.janus.service.timelog.TimeLogService;
import es.nivel36.janus.service.timelog.TimeLogs;
import es.nivel36.janus.service.worksite.Worksite;

/**
 * Nightly precomputation of historical {@link WorkShift} summaries.
 * <p>
 * Identifies employees with orphan {@link TimeLog} entries up to a target day,
 * buckets their logs by natural day and worksite, and persists the resulting
 * {@link WorkShift} aggregates.
 */
@Component
public class WorkShiftPrecomputeJob {

	private static final Logger log = LoggerFactory.getLogger(WorkShiftPrecomputeJob.class);

	private final WorkshiftRepository workshiftRepository;
	private final TimeLogService timeLogService;
	private final ScheduleService scheduleService;
	private final EmployeeService employeeService;
	private final AdminService adminService;
	private final Clock clock;
	private final ShiftPolicy policy;

	/**
	 * Constructs the scheduled job that materializes historical work-shift
	 * summaries.
	 *
	 * @param workshiftRepository repository that persists {@link WorkShift}
	 *                            aggregates; never {@code null}
	 * @param timeLogService      service that queries {@link TimeLog} data; never
	 *                            {@code null}
	 * @param employeeService     service that provides employees pending
	 *                            precomputation; never {@code null}
	 * @param adminService        service that provides admin policies (e.g.,
	 *                            locking horizon); never {@code null}
	 * @param clock               clock used to derive the target anchor; never
	 *                            {@code null}
	 * @throws NullPointerException if any argument is {@code null}
	 */
	public WorkShiftPrecomputeJob(final WorkshiftRepository workshiftRepository, final TimeLogService timeLogService,
			final ScheduleService scheduleService, final EmployeeService employeeService,
			final AdminService adminService, final Clock clock) {
		this.workshiftRepository = Objects.requireNonNull(workshiftRepository, "workshiftRepository must not be null");
		this.timeLogService = Objects.requireNonNull(timeLogService, "timeLogService must not be null");
		this.scheduleService = Objects.requireNonNull(scheduleService, "scheduleService must not be null");
		this.employeeService = Objects.requireNonNull(employeeService, "employeeService must not be null");
		this.adminService = Objects.requireNonNull(adminService, "adminService must not be null");
		this.clock = Objects.requireNonNull(clock, "clock must not be null");
		this.policy = ShiftPolicy.defaultPolicy();
	}

	/**
	 * Executes the nightly batch that generates {@link WorkShift} records from
	 * orphan {@link TimeLog} entries older than the locking horizon defined by
	 * {@link AdminService}.
	 * <p>
	 * Policy: {@code targetAnchor = now(clock) - (daysUntilLocked + 1)}. For each
	 * employee, all orphan logs with {@code entryTime} in
	 * {@code [startOfDay, startOfDay+1d)} for that anchor day are grouped and
	 * materialized into a {@link WorkShift}.
	 * <p>
	 * Scheduling: the cron expression {@code 0 15 2 * * *} runs daily at 02:15 in
	 * the JVM default time zone unless {@code zone} is set on {@link Scheduled}.
	 */
	@Scheduled(cron = "0 15 2 * * *")
	@Transactional
	public void run() {
		final int daysUntilLocked = adminService.getDaysUntilLocked();
		final Instant target = clock.instant().minus(daysUntilLocked + 1L, ChronoUnit.DAYS);
		log.debug("WorkShift precompute started; daysUntilLocked={} targetAnchor={}", daysUntilLocked, target);

		final List<Long> employeeIds = employeeService.findEmployeesWithoutWorkshiftsSince(target);
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
		final Employee employee = employeeService.findEmployeeById(employeeId);
		log.trace("Processing employee {}", employee);

		final TimeLogs orphanLogs = timeLogService.findOrphanTimeLogs(target, employee);
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

		final LocalDate day = firstEntry.atZone(zone).toLocalDate();
		final Instant dayStart = day.atStartOfDay(zone).toInstant();
		final Instant dayEndExclusive = day.plusDays(1).atStartOfDay(zone).toInstant();

		log.trace("Bucket employee={}, worksite={}, zone={}, day={} window=[{} .. {})", employee, worksite, zone, day,
				dayStart, dayEndExclusive);

		final TimeLogs bucket = this.collectBucket(first, worksite, dayStart, dayEndExclusive, queue);
		final Optional<TimeRange> timeRange = this.scheduleService.findTimeRangeForEmployeeByDate(employee, day);

		final ShiftInferenceStrategyResolver resolver = new ShiftInferenceStrategyResolver();
		final ShiftInferenceStrategy strategy = resolver.resolve(timeRange, worksite, policy);
		final WorkShift workShift = new WorkShiftComposer(strategy).compose(employee, day, bucket);
		final WorkShift saved = this.workshiftRepository.save(workShift);
		log.trace("WorkShift persisted with id {}", saved.getId());
	}

	private TimeLogs collectBucket(final TimeLog first, final Worksite worksite, final Instant dayStart,
			final Instant dayEndExclusive, final Deque<TimeLog> queue) {

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
