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
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
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
 * Composes work shifts from time logs selected by a
 * {@link ShiftInferenceStrategy}.
 * <p>
 * Selected logs are associated with the new shift. Work time is the duration
 * covered by their merged intervals, and pause time is the sum of gaps between
 * those intervals. Logs with missing bounds or an exit before entry remain
 * associated with the shift but do not contribute to its durations.
 */
final class WorkShiftComposer {

	private final ShiftInferenceStrategy inferenceStrategy;

	/**
	 * Creates a new {@code WorkShiftComposer} with the specified inference
	 * strategy.
	 *
	 * @param  inferenceStrategy    the strategy used to infer work shifts from time
	 *                              logs. Must not be {@code null}.
	 * @throws NullPointerException if {@code inferenceStrategy} is {@code null}
	 */
	WorkShiftComposer(final ShiftInferenceStrategy inferenceStrategy) {
		this.inferenceStrategy = Objects.requireNonNull(inferenceStrategy, "inferenceStrategy must not be null.");
	}

	/**
	 * Creates a shift for the selected logs and assigns those logs to it.
	 * <p>
	 * Full recorded intervals contribute to work and pause durations; they are not
	 * clipped to scheduled bounds. An empty selection produces a shift with no logs
	 * and zero durations. The shift is not persisted by this operation.
	 *
	 * @param  employee              the employee owning the shift; must not be
	 *                               {@code null}
	 * @param  date                  the local shift date; must not be {@code null}
	 * @param  orderedLogs           candidate logs in entry-time order; must not be
	 *                               {@code null}
	 * @return                       the newly composed shift
	 * @throws NullPointerException  if any argument is {@code null}
	 * @throws IllegalStateException if a selected log is assigned to a different
	 *                               employee/date shift
	 */
	WorkShift compose(final Employee employee, final LocalDate date, final TimeLogs orderedLogs) {
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
