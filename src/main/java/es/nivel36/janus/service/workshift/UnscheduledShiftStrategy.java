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
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import es.nivel36.janus.service.timelog.TimeLog;
import es.nivel36.janus.service.timelog.TimeLogs;
import es.nivel36.janus.service.worksite.Worksite;

/**
 * {@link ShiftInferenceStrategy} implementation that infers a work shift when
 * no scheduled shift information is available.
 *
 * <p>
 * This strategy analyzes an ordered list of {@link TimeLog} entries and
 * identifies long pauses between consecutive logs based on the configured
 * {@link ShiftPolicy}. Depending on the number and position of these pauses,
 * the strategy determines which segment of logs belongs to the inferred shift
 * for a given {@link LocalDate}.
 *
 * <p>
 * The inference rules are:
 * <ul>
 * <li>If two or more long pauses are found, the shift is inferred using a
 * start-anchored extraction strategy.</li>
 * <li>If exactly one long pause is found, either the left or right segment
 * around the pause is selected depending on the pause position relative to the
 * target date.</li>
 * <li>If no long pauses are found, all logs are treated as a single continuous
 * shift.</li>
 * </ul>
 */
final class UnscheduledShiftStrategy implements ShiftInferenceStrategy {

	/**
	 * Policy defining thresholds and rules used to detect long pauses between
	 * {@link TimeLog} entries.
	 */
	private final ShiftPolicy policy;

	/**
	 * Worksite associated with the inferred shift, mainly used to resolve time
	 * zone–dependent date calculations.
	 */
	private final Worksite worksite;

	/**
	 * Creates a new {@code UnscheduledShiftStrategy}.
	 *
	 * @param policy   policy defining long pause thresholds; can't be {@code null}
	 * @param worksite worksite used for time zone resolution; can't be {@code null}
	 */
	UnscheduledShiftStrategy(final ShiftPolicy policy, final Worksite worksite) {
		this.policy = Objects.requireNonNull(policy, "policy must not be null.");
		this.worksite = Objects.requireNonNull(worksite, "worksite must not be null.");
	}

	/**
	 * Infers the {@link TimeLog} entries that belong to the shift for the given
	 * date.
	 *
	 * <p>
	 * The input logs must be ordered chronologically. The method detects long
	 * pauses according to the configured {@link ShiftPolicy} and delegates the
	 * selection of the appropriate segment to specialized extractors.
	 *
	 * @param date        date for which the shift is being inferred; can't be
	 *                    {@code null}
	 * @param orderedLogs chronologically ordered time logs; can't be {@code null}
	 * @return an immutable list of {@link TimeLog} entries belonging to the
	 *         inferred shift; never {@code null}
	 */
	@Override
	public TimeLogs infer(LocalDate date, TimeLogs orderedLogs) {
		Objects.requireNonNull(date, "date must not be null.");
		Objects.requireNonNull(orderedLogs, "orderedLogs must not be null.");
		if (orderedLogs.isEmpty()) {
			return orderedLogs;
		}
		final Duration longPauseThreshold = this.policy.longPauseThreshold();
		final List<PauseInfo> longPauses = this.extractLongPauses(orderedLogs, longPauseThreshold);
		return this.selectByPauses(date, orderedLogs, longPauses);
	}

	private List<PauseInfo> extractLongPauses(final TimeLogs timeLogs, final Duration threshold) {
		final List<PauseInfo> pauses = new ArrayList<>();
		final Iterator<TimeLog> it = timeLogs.iterator();	

		TimeLog current = it.next();
		while (it.hasNext()) {
			final TimeLog next = it.next();
			final Instant out = current.getExitTime();
			if (out == null) {
				throw new IllegalStateException("TimeLog without exit in a closed sequence");
			}
			final Instant nextIn = next.getEntryTime();
			final Duration gap = Duration.between(out, nextIn);
			if (gap.isNegative()) {
				throw new IllegalStateException("Exit time is after next entry time: " + current + " -> " + next);
			}
			if (gap.compareTo(threshold) >= 0) {
				final PauseInfo pauseInfo = new PauseInfo(current, next, gap);
				pauses.add(pauseInfo);
			}
			current = next;
		}
		return pauses;
	}

	private TimeLogs selectByPauses(final LocalDate date, final TimeLogs timeLogs, final List<PauseInfo> pauses) {
		if (pauses.size() >= 2) {
			return new ShiftStartAnchoredExtractor(worksite.getTimeZone()).extract(date, timeLogs, pauses);
		}

		if (pauses.size() == 1) {
			final TimeLog first = pauses.getFirst().before();
			final Worksite firstWorksite = first.getWorksite();
			final ZoneId tz = firstWorksite.getTimeZone();
			final Instant firstExit = Objects.requireNonNull(first.getExitTime(), "first.exitTime must not be null.");
			final LocalDate firstExitDate = firstExit.atZone(tz).toLocalDate();

			final TimeLogsExtractor extractor = firstExitDate.isBefore(date) ? new RightSegmentExtractor()
					: new LeftSegmentExtractor();

			return extractor.extract(date, timeLogs, pauses);
		}

		// No long pauses: treat as a single continuous shift
		return timeLogs;
	}

	/**
	 * Value object representing a long pause between two consecutive
	 * {@link TimeLog} entries.
	 *
	 * @param before   the {@link TimeLog} occurring before the pause; can't be
	 *                 {@code null}
	 * @param after    the {@link TimeLog} occurring after the pause; can't be
	 *                 {@code null}
	 * @param duration duration of the pause; can't be {@code null}
	 */
	record PauseInfo(TimeLog before, TimeLog after, Duration duration) {

		public PauseInfo {
			Objects.requireNonNull(duration, "duration must not be null.");
			Objects.requireNonNull(after, "after must not be null.");
			Objects.requireNonNull(before, "before must not be null.");
		}
	}
}
