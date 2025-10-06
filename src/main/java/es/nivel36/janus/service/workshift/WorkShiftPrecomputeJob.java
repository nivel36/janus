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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import es.nivel36.janus.service.admin.AdminService;
import es.nivel36.janus.service.employee.Employee;
import es.nivel36.janus.service.employee.EmployeeService;
import es.nivel36.janus.service.timelog.TimeLog;
import es.nivel36.janus.service.timelog.TimeLogService;
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

	private final WorkShiftService workShiftService;
	private final TimeLogService timeLogService;
	private final EmployeeService employeeService;
	private final AdminService adminService;
	private final Clock clock;

	/**
	 * Constructs the scheduled job that materializes historical work-shift
	 * summaries.
	 *
	 * @param workShiftService service that builds and persists {@link WorkShift}
	 *                         aggregates; never {@code null}
	 * @param timeLogService   service that queries {@link TimeLog} data; never
	 *                         {@code null}
	 * @param employeeService  service that provides employees pending
	 *                         precomputation; never {@code null}
	 * @param adminService     service that provides admin policies (e.g., locking
	 *                         horizon); never {@code null}
	 * @param clock            clock used to derive the target anchor; never
	 *                         {@code null}
	 * @throws NullPointerException if any argument is {@code null}
	 */
	public WorkShiftPrecomputeJob(final WorkShiftService workShiftService, final TimeLogService timeLogService,
			final EmployeeService employeeService, final AdminService adminService, final Clock clock) {
		this.workShiftService = Objects.requireNonNull(workShiftService, "WorkShiftService must not be null");
		this.timeLogService = Objects.requireNonNull(timeLogService, "TimeLogService must not be null");
		this.employeeService = Objects.requireNonNull(employeeService, "EmployeeService must not be null");
		this.adminService = Objects.requireNonNull(adminService, "AdminService must not be null");
		this.clock = Objects.requireNonNull(clock, "Clock must not be null");
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
	@Transactional // writes WorkShift; must not be readOnly
	public void run() {
		final int daysUntilLocked = adminService.getDaysUntilLocked();
		final Instant target = clock.instant().minus(daysUntilLocked + 1L, ChronoUnit.DAYS);
		log.debug("WorkShift precompute started; daysUntilLocked={} targetAnchor={}", daysUntilLocked, target);

		final List<Long> employeeIds = employeeService.findEmployeesWithoutWorkshifts(target);
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

		final List<TimeLog> orphanLogs = new ArrayList<>(timeLogService.findOrphanTimeLogs(target, employee));
		if (orphanLogs.isEmpty()) {
			// Defensive: employee reported as pending but no logs found.
			log.warn("No orphan timelogs for employee {} at targetAnchor {}", employee, target);
			return;
		}
		log.trace("Orphan timelogs for employee {} count {}", employee, orphanLogs.size());

		final Deque<TimeLog> queue = new ArrayDeque<>(orphanLogs);
		while (!queue.isEmpty()) {
			this.buildAndSaveNextWorkShift(employee, queue);
			log.trace("Remaining orphan timelogs for employee {}: {}", employee, queue.size());
		}
	}

	private void buildAndSaveNextWorkShift(final Employee employee, final Deque<TimeLog> queue) {
		final TimeLog first = queue.removeFirst();
		final Worksite worksite = first.getWorksite();
		final ZoneId zone = worksite.getTimeZone();

		final Instant start = first.getEntryTime();
		final Instant endExclusive = start.plus(1, ChronoUnit.DAYS);
		final LocalDate startDay = start.atZone(zone).toLocalDate();

		log.trace("Bucket window employee={}, worksite={}, zone={}, window=[{} .. {})", employee, worksite, zone, start,
				endExclusive);

		final List<TimeLog> bucket = this.collectBucket(first, endExclusive, queue);

		final WorkShift workShift = workShiftService.buildWorkShift(employee, worksite, startDay, bucket);
		final WorkShift savedWorkshift = workShiftService.save(workShift);
		log.trace("WorkShift persisted with id {}", savedWorkshift.getId());
	}

	private List<TimeLog> collectBucket(final TimeLog first, final Instant endExclusive, final Deque<TimeLog> queue) {
		final List<TimeLog> bucket = new ArrayList<>();
		bucket.add(first);

		while (!queue.isEmpty()) {
			final TimeLog next = queue.peekFirst();
			final Instant entryTime = next.getEntryTime();
			if (!entryTime.isBefore(endExclusive)) {
				break;
			}
			bucket.add(queue.removeFirst());
		}
		log.trace("Collected {} timelogs in bucket", bucket.size());
		return bucket;
	}
}
