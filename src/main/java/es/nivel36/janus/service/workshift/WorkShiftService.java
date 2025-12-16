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
package es.nivel36.janus.service.workshift;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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

import es.nivel36.janus.service.admin.AdminService;
import es.nivel36.janus.service.employee.Employee;
import es.nivel36.janus.service.schedule.ScheduleService;
import es.nivel36.janus.service.schedule.TimeRange;
import es.nivel36.janus.service.timelog.TimeLog;
import es.nivel36.janus.service.timelog.TimeLogService;
import es.nivel36.janus.service.worksite.Worksite;

/**
 * Service responsible for retrieving {@link WorkShift} instances.
 *
 * <p>
 * This service coordinates persistence, scheduling information and time log
 * aggregation in order to compose a {@link WorkShift} for a given employee and
 * date.
 *
 * <p>
 * Depending on business rules, a work shift may be:
 * <ul>
 * <li>retrieved directly from persistence if it is already locked, or</li>
 * <li>dynamically inferred from time logs and scheduling data.</li>
 * </ul>
 *
 * <p>
 * The service relies on multiple collaborators to ensure that shifts are built
 * consistently and according to administrative constraints.
 */
@Service
public class WorkShiftService {

	private static final Logger logger = LoggerFactory.getLogger(WorkShiftService.class);

	private final WorkshiftRepository workshiftRepository;
	private final TimeLogService timeLogService;
	private final ScheduleService scheduleService;
	private final AdminService adminService;
	private final Clock clock;
	private final ShiftPolicy policy;

	/**
	 * Creates a new {@code WorkShiftService} with all required dependencies.
	 *
	 * @param workshiftRepository Repository used to persist and retrieve work
	 *                            shifts. Can't be {@code null}.
	 * @param timeLogService      Service used to retrieve employee time logs. Can't
	 *                            be {@code null}.
	 * @param scheduleService     Service used to obtain scheduled time ranges.
	 *                            Can't be {@code null}.
	 * @param adminService        Service providing administrative configuration.
	 *                            Can't be {@code null}.
	 * @param clock               Clock used to determine the current date and time.
	 *                            Can't be {@code null}.
	 *
	 * @throws NullPointerException if any dependency is {@code null}
	 */
	public WorkShiftService(final WorkshiftRepository workshiftRepository, final TimeLogService timeLogService,
			final ScheduleService scheduleService, final AdminService adminService, final Clock clock) {
		this.workshiftRepository = Objects.requireNonNull(workshiftRepository, "workshiftRepository must not be null");
		this.timeLogService = Objects.requireNonNull(timeLogService, "timeLogService must not be null");
		this.scheduleService = Objects.requireNonNull(scheduleService, "scheduleService must not be null");
		this.adminService = Objects.requireNonNull(adminService, "adminService must not be null");
		this.clock = Objects.requireNonNull(clock, "clock must not be null");
		this.policy = ShiftPolicy.defaultPolicy();
	}

	/**
	 * Retrieves or composes a {@link WorkShift} for the given employee, worksite
	 * and date.
	 *
	 * <p>
	 * If the requested date is already locked according to administrative rules and
	 * a persisted shift exists, that shift is returned.
	 *
	 * <p>
	 * Otherwise, the shift is inferred by:
	 * <ul>
	 * <li>retrieving relevant {@link TimeLog} entries around the target date,</li>
	 * <li>obtaining the scheduled {@link TimeRange}, if any, and</li>
	 * <li>delegating composition to the {@link WorkShiftFactory}.</li>
	 * </ul>
	 *
	 * @param employee Employee for whom the work shift is requested. Can't be
	 *                 {@code null}.
	 * @param worksite Worksite defining the time zone context. Can't be
	 *                 {@code null}.
	 * @param date     Date for which the work shift is requested. Can't be
	 *                 {@code null}.
	 *
	 * @return the existing or newly composed {@link WorkShift}
	 *
	 * @throws NullPointerException if {@code employee}, {@code worksite},
	 *                              {@code worksite} time zone or {@code date} is
	 *                              {@code null}
	 */
	@Transactional(readOnly = true)
	public WorkShift findWorkShift(final Employee employee, final Worksite worksite, final LocalDate date) {
		Objects.requireNonNull(employee, "employee must not be null");
		Objects.requireNonNull(worksite, "worksite must not be null");
		Objects.requireNonNull(date, "date must not be null");
		final ZoneId tz = Objects.requireNonNull(worksite.getTimeZone(), "worksite.timeZone must not be null");

		logger.debug("Finding work shift for employee {} at worksite {} at {}", employee, worksite, date);
		final LocalDate today = this.clock.instant().atZone(tz).toLocalDate();
		final int daysUntilLocked = this.adminService.getDaysUntilLocked();
		final LocalDate lockDate = date.plusDays(daysUntilLocked);
		if (!lockDate.isAfter(today)) {
			logger.trace("Lock date has passed. Searching the workshift in the data base");
			final WorkShift workShift = this.workshiftRepository.findByEmployeeAndDate(employee, date);
			if (workShift != null) {
				return workShift;
			} else {
				logger.trace("Work shift isn't in the data base.");
			}
		}
		logger.trace("Building the work shift");
		final Page<TimeLog> logsPage = findTimeLogs(employee, date, tz);
		final List<TimeLog> orderedLogs = logsPage.getContent();
		final Optional<TimeRange> timeRange = this.scheduleService.findTimeRangeForEmployeeByDate(employee, date);

		final ShiftInferenceStrategyResolver resolver = new ShiftInferenceStrategyResolver();
		final ShiftInferenceStrategy strategy = resolver.resolve(timeRange, worksite, policy);
		return new WorkShiftComposer(strategy).compose(employee, date, orderedLogs);
	}

	private Page<TimeLog> findTimeLogs(final Employee employee, final LocalDate date, final ZoneId tz) {
		final Instant startOfDay = date.atStartOfDay().atZone(tz).toInstant();
		final Instant from = startOfDay.minus(1, ChronoUnit.DAYS);
		final Instant to = startOfDay.plus(2, ChronoUnit.DAYS); // We add two days to ensure that we cover the 24-hour
																// shifts of certain professions.
		final Pageable unpaged = Pageable.unpaged();
		return this.timeLogService.searchTimeLogsByEmployeeAndEntryTimeInRange(employee, from, to, unpaged);
	}
}
