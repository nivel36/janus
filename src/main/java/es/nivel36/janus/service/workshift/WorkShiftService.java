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
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import es.nivel36.janus.service.admin.AdminService;
import es.nivel36.janus.service.employee.Employee;
import es.nivel36.janus.service.schedule.ScheduleService;
import es.nivel36.janus.service.schedule.TimeRange;
import es.nivel36.janus.service.timelog.TimeLog;
import es.nivel36.janus.service.timelog.TimeLogService;
import es.nivel36.janus.service.worksite.Worksite;

/**
 * Service responsible for composing {@link WorkShift} instances from time logs
 * and schedules. Relies on {@link TimeLogService}, {@link ScheduleService} and
 * a repository to fetch or materialize daily shifts.
 */
@Service
public class WorkShiftService {

	private static final Logger logger = LoggerFactory.getLogger(WorkShiftService.class);
	private static final Duration FOUR_HOURS = Duration.ofHours(4);

	private final WorkshiftRepository workshiftRepository;
	private final TimeLogService timeLogservice;
	private final ScheduleService scheduleService;
	private final AdminService adminService;
	private final Clock clock;

	/**
	 * Creates a new {@code WorkShiftService}.
	 *
	 * @param workshiftRepository repository for {@link WorkShift} persistence. Not
	 *                            null.
	 * @param timeLogService      service to query {@link TimeLog}. Not null.
	 * @param scheduleService     service to resolve planned schedules. Not null.
	 * @param adminService        service providing admin parameters. Not null.
	 * @param clock               clock used for time-based decisions. Not null.
	 * @throws NullPointerException if any argument is {@code null}.
	 */
	public WorkShiftService(final WorkshiftRepository workshiftRepository, final TimeLogService timeLogService,
			final ScheduleService scheduleService, final AdminService adminService, final Clock clock) {
		this.workshiftRepository = Objects.requireNonNull(workshiftRepository, "WorkShift Repository must not be null");
		this.timeLogservice = Objects.requireNonNull(timeLogService, "TimeLog Service must not be null");
		this.scheduleService = Objects.requireNonNull(scheduleService, "Schedule Service must not be null");
		this.adminService = Objects.requireNonNull(adminService, "Admin Service must not be null");
		this.clock = Objects.requireNonNull(clock, "Clock must not be null");
	}

	/**
	 * Persists the given {@link WorkShift}.
	 *
	 * @param workShift the {@link WorkShift} to save. Must not be {@code null}.
	 * @return the saved instance of the {@link WorkShift}
	 * @throws NullPointerException if {@code workShift} is {@code null}.
	 */
	public WorkShift save(final WorkShift workShift) {
		Objects.requireNonNull(workShift, "workShift must not be null");
		logger.debug("Saving workShift {}", workShift);
		return this.workshiftRepository.save(workShift);
	}

	/**
	 * Retrieves or builds the work shift for a specified employee at a given
	 * worksite and date.
	 *
	 * @param employee the employee whose work shift is requested. Not null.
	 * @param worksite the worksite providing the time zone context. Not null.
	 * @param date     the local date of interest in the worksite time zone. Not
	 *                 null.
	 * @return a {@link WorkShift}. It may contain no time logs if none were found.
	 * @throws NullPointerException if any parameter is {@code null}.
	 */
	public WorkShift findWorkShift(final Employee employee, final Worksite worksite, final LocalDate date) {
		Objects.requireNonNull(employee, "Employee must not be null");
		Objects.requireNonNull(worksite, "Worksite must not be null");
		Objects.requireNonNull(date, "Date must not be null");
		logger.debug("Finding workshift for employee {} at worksite {} on {}", employee, worksite, date);

		final ZoneId z = worksite.getTimeZone();

		// If the lock date has already passed, the batch job may have materialized
		// the working day; in that case, it makes sense to query the database first.
		final LocalDate today = clock.instant().atZone(z).toLocalDate();
		if (!date.plus(adminService.getDaysUntilLocked(), ChronoUnit.DAYS).isAfter(today)) {
			logger.trace("Lock window passed for {}. Checking existing materialized shifts in DB.", date);
			final boolean existsWorkShift = this.workshiftRepository.existsByEmployeeAndDate(employee, date);
			if (existsWorkShift) {
				return this.workshiftRepository.findByEmployeeAndDate(employee, date);
			} else {
				// It's not a warning because he/she may not have worked that day.
				logger.trace("No workshifts found in DB");
			}
		}

		// No existing (or not yet materialized) work shift found. Build a new one.
		final Instant fromInstant = date.atStartOfDay().atZone(z).toInstant().minus(1, ChronoUnit.DAYS);
		final Instant toInstant = date.atStartOfDay().atZone(z).toInstant().plus(2, ChronoUnit.DAYS);
		logger.trace("Querying time logs with range [{} - {}]", fromInstant, toInstant);
		final Page<TimeLog> timeLogs = this.timeLogservice.searchByEmployeeAndEntryTimeInRange(employee, fromInstant,
				toInstant, Pageable.unpaged());
		return this.buildWorkShift(employee, worksite, date, timeLogs.getContent());
	}

	/**
	 * Builds a {@link WorkShift} from raw time logs, using schedule information if
	 * available.
	 *
	 * @param employee     employee. Must not be {@code null}.
	 * @param worksite     worksite for timezone. Must not be {@code null}.
	 * @param date         local date at worksite. Must not be {@code null}.
	 * @param timeLogsList ordered list of {@link TimeLog} entries to evaluate; must
	 *                     not be {@code null}
	 * @return a populated {@link WorkShift}; never {@code null}
	 */
	public WorkShift buildWorkShift(final Employee employee, final Worksite worksite, final LocalDate date,
			final List<TimeLog> timeLogsList) {
		Objects.requireNonNull(employee);
		Objects.requireNonNull(worksite);
		Objects.requireNonNull(date);
		Objects.requireNonNull(timeLogsList);
		logger.debug("Building work shift for employee {} at worksite {} on date {}", employee, worksite, date);
		if (timeLogsList.isEmpty()) {
			logger.trace("No time logs found.");
			final WorkShift workShift = new WorkShift();
			workShift.setEmployee(employee);
			workShift.setDate(date);
			return workShift;
		}
		final Optional<TimeRange> timeRange = this.scheduleService.findTimeRangeForEmployeeByDate(employee, date);

		return timeRange.map(tr -> this.buildWorkShift(employee, worksite, date, tr, timeLogsList))
				.orElseGet(() -> this.buildWorkShiftForNonWorkingDay(employee, date, timeLogsList));
	}

	/**
	 * Constructs a {@link WorkShift} for the given employee and date using the
	 * provided time range and time logs.
	 *
	 * @param employee  the employee. Not null.
	 * @param worksite  the worksite providing timezone. Not null.
	 * @param date      the date within the worksite timezone. Not null.
	 * @param timeRange scheduled range. Not null.
	 * @param timeLogs  ordered time logs; must not be {@code null}
	 * @return a composed {@link WorkShift} with totals and time logs
	 */
	private WorkShift buildWorkShift(final Employee employee, final Worksite worksite, final LocalDate date,
			final TimeRange timeRange, final List<TimeLog> timeLogs) {
		final ZoneId worksiteTimeZone = worksite.getTimeZone();
		final Instant startTime = date.atTime(timeRange.getStartTime()).atZone(worksiteTimeZone).toInstant();
		// If the departure time is earlier than the arrival time, it means that the
		// worker starts work on one day and finishes on the next.
		final Instant endTime = timeRange.getStartTime().isBefore(timeRange.getEndTime())
				? date.atTime(timeRange.getEndTime()).atZone(worksiteTimeZone).toInstant()
				: date.plusDays(1).atTime(timeRange.getEndTime()).atZone(worksiteTimeZone).toInstant();

		logger.trace("Computed scheduled window: start={} end={} tz={}", startTime, endTime, worksiteTimeZone);

		final WorkShift workShift = new WorkShift();
		workShift.setEmployee(employee);
		workShift.setTotalPauseTime(Duration.ZERO);
		workShift.setTotalWorkTime(Duration.ZERO);
		workShift.setDate(date);

		for (final TimeLog timeLog : timeLogs) {
			final Instant entryTime = timeLog.getEntryTime();
			if (entryTime.isAfter(endTime.plus(FOUR_HOURS))) {
				logger.trace("Skipping remaining logs: entry {} is > end+4h {}", entryTime, endTime.plus(FOUR_HOURS));
				break;
			}
			final Instant exitTime = timeLog.getExitTime();
			if (exitTime.isBefore(startTime.minus(FOUR_HOURS))) {
				logger.trace("Ignoring log before start-4h: exit {} < {}", exitTime, startTime.minus(FOUR_HOURS));
				continue;
			}
			workShift.addTimeLog(timeLog);
			final Duration elapsedWorkTime = Duration.between(entryTime, exitTime);
			workShift.setTotalWorkTime(workShift.getTotalWorkTime().plus(elapsedWorkTime));
		}

		final List<TimeLog> workShiftTimeLogs = workShift.getTimeLogs();
		if (workShiftTimeLogs.isEmpty()) {
			logger.trace("No logs selected within window. Returning empty shift skeleton.");
			return workShift;
		}
		workShift.setTotalPauseTime(this.calculateTotalPauseDuration(workShiftTimeLogs));
		logger.trace("Composed workShift {}", workShift);
		return workShift;
	}

	/**
	 * Calculates the total pause duration from a list of time logs.
	 *
	 * @param timeLogs ordered logs of a single shift. Not null.
	 * @return accumulated pauses as {@link Duration}.
	 */
	private Duration calculateTotalPauseDuration(final List<TimeLog> timeLogs) {
		Duration totalPauseDuration = Duration.ZERO;
		for (int i = 0; i < timeLogs.size() - 1; i++) {
			final TimeLog currentLog = timeLogs.get(i);
			final TimeLog nextLog = timeLogs.get(i + 1);

			if (currentLog.getExitTime() == null || nextLog.getEntryTime() == null) {
				logger.trace("Skipping pause calc at i={} due to null values", i);
				continue;
			}

			final Duration pauseDuration = Duration.between(currentLog.getExitTime(), nextLog.getEntryTime());
			if (!pauseDuration.isNegative()) {
				totalPauseDuration = totalPauseDuration.plus(pauseDuration);
			}
		}
		logger.trace("Total pause duration computed: {}", totalPauseDuration);
		return totalPauseDuration;
	}

	/**
	 * Builds a {@link WorkShift} for a non-working day using the provided logs.
	 *
	 * @param employee the employee. Not null.
	 * @param date     the date. Not null.
	 * @param timeLogs ordered time logs around the date; must not be {@code null}
	 * @return a best-effort shift inferred from logs.
	 */
	private WorkShift buildWorkShiftForNonWorkingDay(final Employee employee, final LocalDate date,
			final List<TimeLog> timeLogs) {
		logger.trace("buildWorkShiftForNonWorkingDay(entry) date={} logsCount={}", date, timeLogs.size());
		final List<PauseInfo> longPauses = this.extractLongPauses(timeLogs);
		final List<TimeLog> workShiftTimeLogs = this.getWorkShiftTimeLogs(date, timeLogs, longPauses);

		Duration totalWorkTime = Duration.ZERO;
		Duration totalPauseTime = Duration.ZERO;

		for (int i = 0; i < workShiftTimeLogs.size(); i++) {
			final TimeLog timeLog = workShiftTimeLogs.get(i);
			final Duration workDuration = Duration.between(timeLog.getEntryTime(), timeLog.getExitTime());
			totalWorkTime = totalWorkTime.plus(workDuration);

			if (i > 0) {
				final TimeLog previousLog = workShiftTimeLogs.get(i - 1);
				final Duration pauseDuration = Duration.between(previousLog.getExitTime(), timeLog.getEntryTime());
				if (!pauseDuration.isNegative()) {
					totalPauseTime = totalPauseTime.plus(pauseDuration);
				}
			}
		}

		final WorkShift workShift = new WorkShift();
		workShift.setEmployee(employee);
		workShift.setDate(date);
		workShift.setTotalWorkTime(totalWorkTime);
		workShift.setTotalPauseTime(totalPauseTime);
		workShift.setTimeLogs(workShiftTimeLogs);

		logger.trace("Non-working-day shift composed: logs={} date={} totalWork={} totalPause={}",
				workShiftTimeLogs.size(), workShift.getDate(), workShift.getTotalWorkTime(),
				workShift.getTotalPauseTime());
		return workShift;
	}

	/**
	 * Extracts pauses of at least {@link #FOUR_HOURS} from the provided logs.
	 *
	 * @param timeLogs ordered logs. Not null.
	 * @return pauses with index and duration.
	 */
	private List<PauseInfo> extractLongPauses(final List<TimeLog> timeLogs) {
		final List<PauseInfo> pauses = new ArrayList<>();
		for (int i = 0; i < timeLogs.size() - 1; i++) {
			final TimeLog currentLog = timeLogs.get(i);
			final TimeLog nextLog = timeLogs.get(i + 1);

			if (currentLog.getExitTime() != null && nextLog.getEntryTime() != null) {
				final Duration pauseDuration = Duration.between(currentLog.getExitTime(), nextLog.getEntryTime());
				// Only consider pauses >= 4 hours
				if (pauseDuration.compareTo(FOUR_HOURS) >= 0) {
					pauses.add(new PauseInfo(i, pauseDuration));
				}
			}
		}
		logger.trace("Detected {} long pauses (>=4h)", pauses.size());
		return pauses;
	}

	/**
	 * Selects the subset of logs that belong to the shift for the given date, using
	 * detected long pauses as separators.
	 *
	 * @param date     target date in local terms. Not null.
	 * @param timeLogs ordered logs. Not null.
	 * @param pauses   detected long pauses. Not null.
	 * @return logs for the inferred shift.
	 */
	private List<TimeLog> getWorkShiftTimeLogs(final LocalDate date, final List<TimeLog> timeLogs,
			final List<PauseInfo> pauses) {
		Objects.requireNonNull(date);
		Objects.requireNonNull(timeLogs);
		Objects.requireNonNull(pauses);
		if (pauses.size() >= 2) {
			return new WeekdayTimeLogsExtractor().extract(date, timeLogs, pauses);
		}
		if (pauses.size() == 1) {
			final TimeLog first = timeLogs.getFirst();
			final Worksite worksite = Objects.requireNonNull(first.getWorksite(), "worksite must not be null");
			final ZoneId tz = Objects.requireNonNull(worksite.getTimeZone(), "TimeZone must not be null");
			final Instant exitTime = Objects.requireNonNull(first.getExitTime(), "exitTime must not be null");
			final LocalDate exitDate = exitTime.atZone(tz).toLocalDate();

			final TimeLogsExtractor extractor = exitDate.isBefore(date) ? new WeekStartTimeLogsExtractor()
					: new WeekEndTimeLogsExtractor();

			return extractor.extract(date, timeLogs, pauses);
		}
		// 0 pausas largas: un solo tramo
		return timeLogs;
	}

	/**
	 * Holds index of the log and the long pause duration that follows it.
	 */
	static class PauseInfo {
		int index;
		Duration duration;

		PauseInfo(final int index, final Duration duration) {
			this.index = index;
			this.duration = duration;
		}
	}
}
