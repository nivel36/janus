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
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

import es.nivel36.janus.service.timelog.TimeLog;
import es.nivel36.janus.service.timelog.TimeLogs;
import es.nivel36.janus.service.workshift.UnscheduledShiftStrategy.PauseInfo;

/**
 * Selects a contiguous segment anchored on the first log starting on a local
 * date.
 * <p>
 * The configured zone determines the anchor date. Chronologically ordered long
 * pauses delimit the segment; a missing separator extends the segment to the
 * corresponding end of the log collection. No anchor produces an empty result.
 */
final class ShiftStartAnchoredExtractor implements TimeLogsExtractor {

	private final ZoneId zoneId;

	/**
	 * Creates a new extractor bound to the given time zone.
	 *
	 * @param  zoneId               the time zone used to resolve local dates from
	 *                              entry times; must not be {@code null}
	 * @throws NullPointerException if {@code zoneId} is {@code null}
	 */
	ShiftStartAnchoredExtractor(final ZoneId zoneId) {
		this.zoneId = Objects.requireNonNull(zoneId, "zoneId must not be null");
	}

	/**
	 * Returns the segment containing the first log starting on the requested date.
	 * <p>
	 * At least two logs and two pauses are required. Pause references must identify
	 * logs in the supplied collection and define consistent segment boundaries.
	 *
	 * @param  date                  the local date used to select the anchor; must
	 *                               not be {@code null}
	 * @param  timeLogs              the chronologically ordered logs; must not be
	 *                               {@code null}
	 * @param  pauses                the chronologically ordered long pauses; must
	 *                               not be {@code null}
	 * @return                       the anchored segment, or an empty collection if
	 *                               no anchor exists
	 * @throws NullPointerException  if an argument or required pause reference is
	 *                               {@code null}
	 * @throws IllegalStateException if fewer than two logs or pauses are supplied,
	 *                               a boundary log is absent, or the segment
	 *                               boundaries are reversed
	 */
	@Override
	public TimeLogs extract(final LocalDate date, final TimeLogs timeLogs, final List<PauseInfo> pauses) {
		Objects.requireNonNull(date, "date must not be null");
		Objects.requireNonNull(timeLogs, "timeLogs must not be null");
		Objects.requireNonNull(pauses, "pauses must not be null");

		if (pauses.size() < 2) {
			throw new IllegalStateException("At least two pauses are required");
		}
		if (timeLogs.size() < 2) {
			throw new IllegalStateException("At least two time logs are required");
		}

		final TimeLog anchor = this.findAnchor(date, timeLogs);
		if (anchor == null) {
			return new TimeLogs(List.of());
		}

		final PauseInfo leftPause = this.findLastPauseBefore(anchor, pauses);
		final PauseInfo rightPause = this.findFirstPauseAfterOrAt(anchor, pauses);

		final int startIndex = leftPause != null ? indexOfOrFail(timeLogs, leftPause.after(), "left pause 'after'") : 0;

		final int endIndex = rightPause != null ? indexOfOrFail(timeLogs, rightPause.before(), "right pause 'before'")
				: timeLogs.size() - 1;

		if (startIndex > endIndex) {
			throw new IllegalStateException(
					"Invalid range computed: startIndex=" + startIndex + ", endIndex=" + endIndex);
		}

		return timeLogs.slice(startIndex, endIndex + 1);
	}

	private TimeLog findAnchor(final LocalDate date, final TimeLogs timeLogs) {
		for (final TimeLog log : timeLogs) {
			final Instant in = log.getEntryTime();
			if (in != null && in.atZone(this.zoneId).toLocalDate().equals(date)) {
				return log;
			}
		}
		return null;
	}

	private PauseInfo findLastPauseBefore(final TimeLog anchor, final List<PauseInfo> pauses) {
		PauseInfo candidate = null;
		for (final PauseInfo pause : pauses) {
			if (pause.after().equals(anchor)) {
				return pause;
			}
			if (pause.after().getEntryTime().isBefore(anchor.getEntryTime())) {
				candidate = pause;
			}
		}
		return candidate;
	}

	private PauseInfo findFirstPauseAfterOrAt(final TimeLog anchor, final List<PauseInfo> pauses) {
		for (final PauseInfo pause : pauses) {
			if (pause.before().equals(anchor) || pause.before().getEntryTime().isAfter(anchor.getEntryTime())) {
				return pause;
			}
		}
		return null;
	}

	private static int indexOfOrFail(final TimeLogs logs, final TimeLog log, final String label) {
		final int index = logs.indexOf(log);
		if (index < 0) {
			throw new IllegalStateException(label + " not found in timeLogs");
		}
		return index;
	}
}
