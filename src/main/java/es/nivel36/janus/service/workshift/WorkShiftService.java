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

import es.nivel36.janus.service.employee.Employee;
import es.nivel36.janus.service.schedule.ScheduleService;
import es.nivel36.janus.service.schedule.TimeRange;
import es.nivel36.janus.service.timelog.TimeLog;
import es.nivel36.janus.service.timelog.TimeLogService;
import es.nivel36.janus.service.worksite.Worksite;

/**
 * Service class responsible for managing work shifts for employees. This class
 * interacts with {@link TimeLogService} and {@link ScheduleService} to
 * determine the work shifts of employees based on their time logs and
 * schedules.
 */
@Service
public class WorkShiftService {

	private static final Logger logger = LoggerFactory.getLogger(WorkShiftService.class);
	private static final Duration FOUR_HOURS = Duration.ofHours(4);

	private TimeLogService timeLogservice;
	private ScheduleService scheduleService;

	public WorkShiftService(final TimeLogService timeLogService, final ScheduleService scheduleService) {
		this.timeLogservice = Objects.requireNonNull(timeLogService, "TimeLog Service can't be null");
		this.scheduleService = Objects.requireNonNull(scheduleService, "Schedule Service can't be null");
	}

	/**
	 * Retrieves the work shift for a specified employee on a given date.
	 *
	 * @param employee the employee whose work shift is to be retrieved. Can not be
	 *                 {@code null}
	 * @param date     the date for which the work shift is to be retrieved. Can not
	 *                 be {@code null}
	 * @return the {@link WorkShift} for the employee on the specified date, or
	 *         {@code null} if no time logs are found
	 * @throws NullPointerException if the employee or date is {@code null}
	 */
	public WorkShift getWorkShift(final Employee employee, final Worksite worksite, final LocalDate date) {
		Objects.requireNonNull(employee, "Employee cant be null");
		Objects.requireNonNull(worksite, "Worksite cant be null");
		Objects.requireNonNull(date, "Date cant be null");
		logger.debug("Getting work shift for employee {} at {}", employee, date);

		final Page<TimeLog> timeLogs = this.timeLogservice.findTimeLogsByEmployeeAndWorksiteAndDate(employee, worksite,
				date, Pageable.ofSize(100));
		if (timeLogs.isEmpty()) {
			final WorkShift workShift = new WorkShift();
			workShift.setEmployee(employee);
			return workShift;
		}
		if (timeLogs.getTotalPages() > 1) {
			throw new IllegalStateException("Too many time logs for one day");
		}

		final List<TimeLog> timeLogsList = timeLogs.getContent();
		final Optional<TimeRange> timeRange = this.scheduleService.findTimeRangeForEmployeeByDate(employee, date);

		return timeRange.map(tr -> this.buildWorkShift(employee, worksite, date, tr, timeLogsList))
				.orElseGet(() -> this.buildWorkShiftForNonWorkingDay(employee, date, timeLogsList));
	}

	/**
	 * Constructs a {@link WorkShift} for the given employee on the specified date
	 * using the provided time range and time logs.
	 *
	 * @param employee  the employee for whom the work shift is being constructed.
	 *                  Cannot be {@code null}.
	 * @param date      the date for which the work shift is being constructed.
	 *                  Cannot be {@code null}.
	 * @param timeRange the time range defining the scheduled start and end of the
	 *                  work shift. Cannot be {@code null}.
	 * @param timeLogs  the list of time logs associated with the employee on the
	 *                  specified date. Cannot be {@code null}.
	 * @return the constructed {@link WorkShift} object containing the employee's
	 *         work shift details.
	 */
	private WorkShift buildWorkShift(final Employee employee, final Worksite worksite, final LocalDate date,
			final TimeRange timeRange, final List<TimeLog> timeLogs) {
		// We need to find the start and end of the work shift. We have the times, but
		// we also need the day.
		final Instant startTime = date.atTime(timeRange.getStartTime()).atZone(worksite.getTimeZone()).toInstant();
		final Instant endTime;
		if (timeRange.getStartTime().isBefore(timeRange.getEndTime())) {
			// The work shift starts and ends on the same day.
			endTime = date.atTime(timeRange.getEndTime()).atZone(worksite.getTimeZone()).toInstant();
		} else {
			// The work shift starts on one day and ends the following day.
			endTime = date.plusDays(1).atTime(timeRange.getEndTime()).atZone(worksite.getTimeZone()).toInstant();
		}
		final WorkShift workShift = new WorkShift();
		workShift.setEmployee(employee);
		workShift.setTotalPauseTime(Duration.ZERO);
		workShift.setTotalWorkTime(Duration.ZERO);

		// We look for the time logs that fall within the work shift. We cannot rely
		// on the scheduled shift because the employee might have started working before
		// or finished after the official shift. What we do is extend the range by four
		// hours
		// before and after. If the log falls within this margin, we consider it part of
		// the shift.
		for (final TimeLog timeLog : timeLogs) {
			final Instant entryTime = timeLog.getEntryTime();
			if (entryTime.isAfter(endTime.plus(4, ChronoUnit.HOURS))) {
				// The employee has clocked in at least four hours after the scheduled end
				// of the work shift. This means it belongs to the next work shift, and since
				// the logs are ordered, we can stop searching for records of this shift.
				break;
			}
			final Instant exitTime = timeLog.getExitTime();
			if (exitTime.isBefore(startTime.minus(4, ChronoUnit.HOURS))) {
				// The exit log is at least four hours before the scheduled start time.
				// This means it belongs to the previous work shift, and we ignore it.
				continue;
			}
			workShift.addTimeLog(timeLog);
			final Duration elapsedWorkTime = Duration.between(entryTime, exitTime);
			workShift.setTotalWorkTime(workShift.getTotalWorkTime().plus(elapsedWorkTime));
		}

		final List<TimeLog> workShiftTimeLogs = workShift.getTimeLogs();
		if (workShiftTimeLogs.isEmpty()) {
			return workShift;
		}
		workShift.setTotalPauseTime(this.calculateTotalPauseDuration(workShiftTimeLogs));
		workShift.setStartTime(workShiftTimeLogs.getFirst().getEntryTime());
		workShift.setEndTime(workShiftTimeLogs.getLast().getExitTime());
		return workShift;
	}

	/**
	 * Calculates the total pause duration from a list of time logs.
	 *
	 * @param timeLogs the list of time logs to calculate the pause duration from.
	 *                 Cannot be {@code null}.
	 * @return the total pause duration as a {@link Duration}.
	 */
	private Duration calculateTotalPauseDuration(final List<TimeLog> timeLogs) {
		Duration totalPauseDuration = Duration.ZERO;
		for (int i = 0; i < timeLogs.size() - 1; i++) {
			final TimeLog currentLog = timeLogs.get(i);
			final TimeLog nextLog = timeLogs.get(i + 1);

			if (currentLog.getExitTime() == null || nextLog.getEntryTime() == null) {
				continue;
			}

			final Duration pauseDuration = Duration.between(currentLog.getExitTime(), nextLog.getEntryTime());
			if (!pauseDuration.isNegative()) {
				totalPauseDuration = totalPauseDuration.plus(pauseDuration);
			}
		}
		return totalPauseDuration;
	}

	/**
	 * Constructs a {@link WorkShift} for a non-working day, based on the provided
	 * employee, date, and time logs.
	 *
	 * @param employee the employee for whom the work shift is being constructed.
	 *                 Cannot be {@code null}.
	 * @param date     the date for which the work shift is being constructed.
	 *                 Cannot be {@code null}.
	 * @param timeLogs the list of time logs associated with the employee on the
	 *                 specified date. Cannot be {@code null}.
	 * @return the constructed {@link WorkShift} object for a non-working day.
	 */
	private WorkShift buildWorkShiftForNonWorkingDay(final Employee employee, final LocalDate date,
			final List<TimeLog> timeLogs) {
		final List<PauseInfo> pauses = this.getPauses(timeLogs);
		final List<TimeLog> workShiftTimeLogs = this.getWorkShiftTimeLogs(date, timeLogs, pauses);

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
		workShift.setStartTime(workShiftTimeLogs.getFirst().getEntryTime());
		workShift.setEndTime(workShiftTimeLogs.getLast().getExitTime());
		workShift.setTotalWorkTime(totalWorkTime);
		workShift.setTotalPauseTime(totalPauseTime);
		workShift.setTimeLogs(workShiftTimeLogs);

		return workShift;
	}

	/**
	 * Retrieves a list of pauses from the provided time logs.
	 *
	 * @param timeLogs the list of time logs from which pauses are to be extracted.
	 *                 Cannot be {@code null}.
	 * @return a list of {@link PauseInfo} objects containing the index and duration
	 *         of each pause.
	 */
	private List<PauseInfo> getPauses(final List<TimeLog> timeLogs) {
		final List<PauseInfo> pauses = new ArrayList<>();
		for (int i = 0; i < timeLogs.size() - 1; i++) {
			final TimeLog currentLog = timeLogs.get(i);
			final TimeLog nextLog = timeLogs.get(i + 1);

			if (currentLog.getExitTime() != null && nextLog.getEntryTime() != null) {
				final Duration pauseDuration = Duration.between(currentLog.getExitTime(), nextLog.getEntryTime());
				// We only consider pauses longer than four hours. If a worker takes a break of
				// more than four hours, we consider it as ending or starting a work shift.
				if (pauseDuration.compareTo(FOUR_HOURS) >= 0) {
					pauses.add(new PauseInfo(i, pauseDuration));
				}
			}
		}
		return pauses;
	}

	/**
	 * Retrieves the time logs for the work shift based on the date, time logs, and
	 * pauses.
	 *
	 * @param date     the date for which the work shift time logs are to be
	 *                 retrieved. Cannot be {@code null}.
	 * @param timeLogs the list of time logs to consider for the work shift. Cannot
	 *                 be {@code null}.
	 * @param pauses   the list of pauses to consider when determining the work
	 *                 shift time logs. Cannot be {@code null}.
	 * @return a list of {@link TimeLog} objects representing the time logs for the
	 *         work shift.
	 */
	private List<TimeLog> getWorkShiftTimeLogs(final LocalDate date, final List<TimeLog> timeLogs,
			final List<PauseInfo> pauses) {
		List<TimeLog> workShiftTimeLogs;
		if (pauses.size() >= 2) {
			// We have at least two breaks longer than four hours. This means that,maybe, we
			// are on
			// a day where the worker has worked the previous day and the next one.
			workShiftTimeLogs = this.extractTimeLogsOfWorkWeekday(timeLogs, pauses);
		} else if (pauses.size() == 1) {
			// We only have one break in a day. We need to determine if the break is at the
			// start or the end of the working week.
			final TimeLog firstTimeLog = timeLogs.getFirst();
			final ZoneId timeZone = firstTimeLog.getWorksite().getTimeZone();
			final LocalDate exitDate = firstTimeLog.getExitTime().atZone(timeZone).toLocalDate();
			if (exitDate.isBefore(date)) {
				// It is the start of the working week (usually between Monday and Tuesday).
				workShiftTimeLogs = this.extractTimeLogsOfWorkWeekStart(timeLogs, pauses);
			} else {
				// It is the end of the working week (usually between Thursday and Friday).
				workShiftTimeLogs = this.extractTimeLogsOfWorkWeekEnd(timeLogs, pauses);
			}
		} else {
			// There are no breaks longer than 4 hours. All time logs are from the same work
			// shift.
			workShiftTimeLogs = timeLogs;
		}
		return workShiftTimeLogs;
	}

	/**
	 * Extracts time logs from the end of the work week based on the provided
	 * pauses.
	 *
	 * @param timeLogs the list of time logs to extract from. Cannot be
	 *                 {@code null}.
	 * @param pauses   the list of pauses to consider for extraction. Cannot be
	 *                 {@code null}.
	 * @return a list of {@link TimeLog} objects representing the extracted time
	 *         logs.
	 */
	private List<TimeLog> extractTimeLogsOfWorkWeekEnd(final List<TimeLog> timeLogs, final List<PauseInfo> pauses) {
		return timeLogs.subList(0, pauses.getFirst().index + 1);
	}

	/**
	 * Extracts time logs from the start of the work week based on the provided
	 * pauses.
	 *
	 * @param timeLogs the list of time logs to extract from. Cannot be
	 *                 {@code null}.
	 * @param pauses   the list of pauses to consider for extraction. Cannot be
	 *                 {@code null}.
	 * @return a list of {@link TimeLog} objects representing the extracted time
	 *         logs.
	 */
	private List<TimeLog> extractTimeLogsOfWorkWeekStart(final List<TimeLog> timeLogs, final List<PauseInfo> pauses) {
		return timeLogs.subList(pauses.getFirst().index + 1, timeLogs.size());
	}

	/**
	 * Extracts time logs for a weekday based on the provided time logs and pauses.
	 *
	 * @param timeLogs the list of time logs to extract from. Cannot be
	 *                 {@code null}.
	 * @param pauses   the list of pauses to consider for extraction. Cannot be
	 *                 {@code null}.
	 * @return a list of {@link TimeLog} objects representing the extracted time
	 *         logs.
	 */
	private List<TimeLog> extractTimeLogsOfWorkWeekday(final List<TimeLog> timeLogs, final List<PauseInfo> pauses) {
		pauses.sort((p1, p2) -> p2.duration.compareTo(p1.duration));

		PauseInfo firstLongestPause = pauses.get(0);
		PauseInfo secondLongestPause = pauses.get(1);

		if (firstLongestPause.index > secondLongestPause.index) {
			final PauseInfo temp = firstLongestPause;
			firstLongestPause = secondLongestPause;
			secondLongestPause = temp;
		}

		final int startIndex = firstLongestPause.index + 1;
		final int endIndex = secondLongestPause.index;

		return timeLogs.subList(startIndex, endIndex + 1);
	}

	private static class PauseInfo {
		int index;
		Duration duration;

		PauseInfo(final int index, final Duration duration) {
			this.index = index;
			this.duration = duration;
		}
	}
}
