/*
 * Copyright 2026 Abel Ferrer Jiménez
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

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import es.nivel36.janus.service.employee.Employee;
import es.nivel36.janus.service.timelog.TimeLog;
import es.nivel36.janus.service.timelog.TimeLogs;

/**
 * Composes {@link WorkShift} instances from a set of ordered {@link TimeLog}
 * entries using a {@link ShiftInferenceStrategy}.
 *
 * <p>
 * This class delegates the responsibility of selecting and optionally clipping
 * relevant time logs to the configured inference strategy, and then derives
 * aggregate values such as total work time and total pause time.
 *
 * <p>
 * Instances of this class are immutable and thread-safe provided that the
 * supplied {@link ShiftInferenceStrategy} is thread-safe.
 */
final class WorkShiftComposer {

	/**
	 * Strategy used to infer which time logs belong to a work shift and how they
	 * should be interpreted. Can't be {@code null}.
	 */
	private final ShiftInferenceStrategy inferenceStrategy;

	/**
	 * Creates a new {@code WorkShiftComposer} with the specified inference
	 * strategy.
	 *
	 * @param inferenceStrategy the strategy used to infer work shifts from time
	 *                          logs. Can't be {@code null}.
	 * @throws NullPointerException if {@code inferenceStrategy} is {@code null}
	 */
	WorkShiftComposer(final ShiftInferenceStrategy inferenceStrategy) {
		this.inferenceStrategy = Objects.requireNonNull(inferenceStrategy, "inferenceStrategy must not be null.");
	}

	/**
	 * Composes a {@link WorkShift} for the given employee and date based on the
	 * provided ordered time logs.
	 *
	 * <p>
	 * The inference strategy determines which logs are relevant and whether a
	 * clipping window applies. The resulting work shift contains the selected logs
	 * and has its total work time and pause time calculated accordingly.
	 *
	 * @param employee    the employee for whom the work shift is composed. Can't be
	 *                    {@code null}.
	 * @param date        the date of the work shift. Can't be {@code null}.
	 * @param orderedLogs the ordered list of time logs to evaluate. Can't be
	 *                    {@code null}.
	 * @return a composed {@code WorkShift} instance. If no logs are selected, the
	 *         returned work shift will contain no time logs and zero durations.
	 * @throws NullPointerException if any of the parameters is {@code null}
	 */
	WorkShift compose(Employee employee, LocalDate date, TimeLogs orderedLogs) {
		Objects.requireNonNull(employee, "employee can't be null");
		Objects.requireNonNull(date, "date can't be null");
		Objects.requireNonNull(orderedLogs, "orderedLogs can't be null");
		final TimeLogs selectedLogs = this.inferenceStrategy.infer(date, orderedLogs);
		if (selectedLogs.isEmpty()) {
			return new WorkShift(employee, date, List.of());
		}
		final WorkShift shift = new WorkShift(employee, date, selectedLogs.asList());
		final TimeIntervals timeIntervals = this.toIntervals(selectedLogs);
		shift.setTotalWorkTime(timeIntervals.totalCoveredDuration());
		shift.setTotalPauseTime(timeIntervals.totalGapDuration());
		return shift;
	}

	private TimeIntervals toIntervals(final TimeLogs logs) {

		final List<TimeInterval> intervals = new ArrayList<>();
		for (final TimeLog log : logs) {
			final Instant in = log.getEntryTime();
			final Instant out = log.getExitTime();
			if (in == null || out == null || out.isBefore(in)) {
				continue;
			}

			final TimeInterval interval = new TimeInterval(in, out);
			intervals.add(interval);
		}

		return TimeIntervals.of(intervals);
	}
}
