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

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import es.nivel36.janus.service.schedule.TimeRange;
import es.nivel36.janus.service.timelog.TimeLog;
import es.nivel36.janus.service.timelog.TimeLogs;

/**
 * Shift inference strategy based on scheduled shifts.
 *
 * <p>
 * This strategy selects {@link TimeLog} entries that overlap a scheduled
 * {@link ShiftWindow} expanded by a configurable selection margin defined
 * in the {@link ShiftPolicy}. The scheduled window itself is used as the
 * reference boundary for inference.
 *
 * <p>
 * The selection margin is applied symmetrically before the start and after
 * the end of the scheduled shift window.
 *
 * <p>
 * This strategy is intended to be used when a fixed scheduled time range
 * is available and time logs must be correlated against it.
 */
final class ScheduledShiftStrategy implements ShiftInferenceStrategy {

	/**
	 * Policy defining the selection margin used to expand the scheduled shift
	 * window.
	 */
	private final ShiftPolicy policy;

	/**
	 * Scheduled time range defining the base shift window.
	 */
	private final TimeRange timeRange;

	/**
	 * Time zone used to compute the scheduled shift window.
	 */
	private final ZoneId timeZone;

	/**
	 * Creates a new {@code ScheduledShiftStrategy}.
	 *
	 * @param policy
	 *        Policy containing the selection margin used to expand the scheduled
	 *        shift window. Can't be {@code null}.
	 * @param timeRange
	 *        Scheduled time range used to build the shift window. Can't be
	 *        {@code null}.
	 * @param timeZone
	 *        Time zone used to compute the scheduled shift window. Can't be
	 *        {@code null}.
	 *
	 * @throws NullPointerException if any argument is {@code null}
	 */
	ScheduledShiftStrategy(
			final ShiftPolicy policy,
			final TimeRange timeRange,
			final ZoneId timeZone) {

		this.policy = Objects.requireNonNull(policy, "policy must not be null.");
		this.timeRange = Objects.requireNonNull(timeRange, "timeRange must not be null.");
		this.timeZone = Objects.requireNonNull(timeZone, "timeZone must not be null.");
	}

	/**
	 * Infers time logs associated with a scheduled shift for the given date.
	 *
	 * <p>
	 * The inference associates all {@link TimeLog} entries whose time interval
	 * overlaps the scheduled {@link ShiftWindow} expanded by the selection
	 * margin defined in the {@link ShiftPolicy}.
	 *
	 * @param date
	 *        Date for which the scheduled shift window is computed. Can't be
	 *        {@code null}.
	 * @param orderedLogs
	 *        Ordered collection of time logs to be evaluated. Can't be
	 *        {@code null}.
	 *
	 * @return A {@link TimeLogs} instance containing all logs that overlap the
	 *         expanded scheduled shift window.
	 *
	 * @throws NullPointerException if {@code date} or {@code orderedLogs} is
	 *                              {@code null}
	 */
	@Override
	public TimeLogs infer(final LocalDate date, final TimeLogs orderedLogs) {
		Objects.requireNonNull(date, "date can't be null");
		Objects.requireNonNull(orderedLogs, "orderedLogs can't be null");

		final ShiftWindow window = ShiftWindow.scheduled(date, this.timeRange, this.timeZone);
		final Duration selectionMargin = this.policy.selectionMargin();
		final TimeInterval expandedWindow = window.expandedBy(selectionMargin);

		final List<TimeLog> selected = new ArrayList<>();
		for (final TimeLog log : orderedLogs) {
			final Instant in = log.getEntryTime();
			if (expandedWindow.endsAtOrBefore(in)) {
				break;
			}
			final Instant out = log.getExitTime();
			final TimeInterval timeLogInterval = new TimeInterval(in, out);
			if (expandedWindow.overlaps(timeLogInterval)) {
				selected.add(log);
			}
		}
		return new TimeLogs(selected);
	}
}
