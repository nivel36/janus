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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import es.nivel36.janus.service.schedule.TimeRange;
import es.nivel36.janus.service.timelog.TimeLog;
import es.nivel36.janus.service.timelog.TimeLogs;
import es.nivel36.janus.service.worksite.Worksite;

/**
 * Shift inference strategy for scheduled shifts.
 *
 * <p>
 * This strategy selects {@link TimeLog} entries that overlap a scheduled shift
 * window expanded by a configurable margin, and returns the scheduled window as
 * a clipping boundary to ensure accurate aggregation.
 *
 * <p>
 * The selection margin is applied symmetrically before the start and after the
 * end of the scheduled window.
 */
final class ScheduledShiftStrategy implements ShiftInferenceStrategy {

	private final ShiftPolicy policy;
	private final TimeRange timeRange;
	private final Worksite worksite;

	/**
	 * Creates a new {@code ScheduledShiftStrategy}.
	 *
	 * @param policy Policy containing the selection margin used to expand the
	 *               scheduled window. Can't be {@code null}.
	 *
	 * @throws NullPointerException if {@code policy} is {@code null}
	 */
	ScheduledShiftStrategy(final ShiftPolicy policy, final TimeRange timeRange, final Worksite worksite) {
		this.policy = Objects.requireNonNull(policy, "policy must not be null.");
		this.timeRange = Objects.requireNonNull(timeRange, "timeRange must not be null.");
		this.worksite = Objects.requireNonNull(worksite, "worksite must not be null.");
	}

	/**
	 * Infers a shift result based on scheduled shift information.
	 *
	 * <p>
	 * The inference process:
	 * <ul>
	 * <li>computes the scheduled {@link ShiftWindow} for the given context,</li>
	 * <li>expands that window using the policy selection margin,</li>
	 * <li>selects all {@link TimeLog} entries overlapping the expanded window,
	 * and</li>
	 * <li>returns the original scheduled window as a clipping boundary.</li>
	 * </ul>
	 *
	 * @param context Context containing worksite, date, schedule and ordered logs.
	 *                Can't be {@code null}.
	 *
	 * @return A {@link ShiftInferenceResult} containing the selected logs and the
	 *         scheduled window as clip window
	 *
	 * @throws NullPointerException if {@code context}, its {@code worksite}, or its
	 *                              {@code timeRange} is {@code null}
	 */
	@Override
	public TimeLogs infer(final LocalDate date, final TimeLogs orderedLogs) {
		Objects.requireNonNull(date, "date can't be null");
		Objects.requireNonNull(orderedLogs, "orderedLogs can't be null");
		final ZoneId timeZone = this.worksite.getTimeZone();
		final ShiftWindow window = ShiftWindow.scheduled(date, this.timeRange, timeZone);
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
