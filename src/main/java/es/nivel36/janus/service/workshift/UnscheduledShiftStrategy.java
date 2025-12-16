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

import es.nivel36.janus.service.timelog.TimeLog;
import es.nivel36.janus.service.worksite.Worksite;

/**
 * Unscheduled shift inference: uses long pauses as separators and selects a
 * "best effort" set of logs for the requested date. No clip window is returned.
 *
 * <p>
 * This strategy delegates the ambiguous "which side of a single long pause"
 * decision to extractors.
 */
final class UnscheduledShiftStrategy implements ShiftInferenceStrategy {

	private final ShiftPolicy policy;
	private final Worksite worksite;

	/**
	 * Creates the strategy.
	 *
	 * @param policy policy containing long pause threshold, not null
	 */
	UnscheduledShiftStrategy(final ShiftPolicy policy, final Worksite worksite) {
		this.policy = Objects.requireNonNull(policy, "policy must not be null.");
		this.worksite = Objects.requireNonNull(worksite, "worksite must not be null.");
	}

	@Override
	public List<TimeLog> infer(LocalDate date, List<TimeLog> orderedLogs) {

		final List<PauseInfo> longPauses = this.extractLongPauses(orderedLogs, this.policy.longPauseThreshold());
		final List<TimeLog> selected = this.selectByPauses(date, orderedLogs, longPauses);
		return List.copyOf(selected);
	}

	/**
	 * Extracts pauses of at least {@code threshold} between consecutive logs.
	 *
	 * @param timeLogs  ordered logs, not null
	 * @param threshold long pause threshold, not null
	 * @return long pauses
	 */
	private List<PauseInfo> extractLongPauses(final List<TimeLog> timeLogs, final Duration threshold) {
		Objects.requireNonNull(timeLogs, "timeLogs must not be null.");
		Objects.requireNonNull(threshold, "threshold must not be null.");

		final List<PauseInfo> pauses = new ArrayList<>();
		for (int i = 0; i < timeLogs.size() - 1; i++) {
			final TimeLog current = timeLogs.get(i);
			final TimeLog next = timeLogs.get(i + 1);

			final Instant out = current.getExitTime();
			final Instant nextIn = next.getEntryTime();
			if (out == null || nextIn == null) {
				continue;
			}

			final Duration gap = Duration.between(out, nextIn);
			if (!gap.isNegative() && gap.compareTo(threshold) >= 0) {
				pauses.add(new PauseInfo(i, gap));
			}
		}
		return pauses;
	}

	/**
	 * Selects which logs belong to the inferred shift for {@code date} using pause
	 * separators.
	 *
	 * @param date     target date, not null
	 * @param timeLogs ordered logs, not null
	 * @param pauses   long pauses, not null
	 * @return selected logs for the inferred shift
	 */
	private List<TimeLog> selectByPauses(final LocalDate date, final List<TimeLog> timeLogs,
			final List<PauseInfo> pauses) {
		Objects.requireNonNull(date, "date must not be null.");
		Objects.requireNonNull(timeLogs, "timeLogs must not be null.");
		Objects.requireNonNull(pauses, "pauses must not be null.");

		if (timeLogs.isEmpty()) {
			return List.of();
		}

		if (pauses.size() >= 2) {
			return new ShiftStartAnchoredExtractor(worksite.getTimeZone()).extract(date, timeLogs, pauses);
		}

		if (pauses.size() == 1) {
			final TimeLog first = timeLogs.get(pauses.getFirst().index());
			final Worksite worksite = Objects.requireNonNull(first.getWorksite(), "worksite must not be null.");
			final ZoneId tz = Objects.requireNonNull(worksite.getTimeZone(), "worksite.timeZone must not be null.");
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
	 * Holds index of the log and the long pause duration that follows it.
	 *
	 * @param index    index of the log after which the pause occurs
	 * @param duration pause duration
	 */
	record PauseInfo(int index, Duration duration) {

		/**
		 * Creates pause info.
		 *
		 * @param index    index, must be >= 0
		 * @param duration duration, not null
		 */
		public PauseInfo {
			Objects.requireNonNull(duration, "duration must not be null.");
			if (index < 0) {
				throw new IllegalArgumentException("index must be >= 0.");
			}
		}
	}
}
