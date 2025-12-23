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

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

import es.nivel36.janus.service.timelog.TimeLog;
import es.nivel36.janus.service.timelog.TimeLogs;
import es.nivel36.janus.service.workshift.UnscheduledShiftStrategy.PauseInfo;

/**
 * {@link TimeLogsExtractor} implementation that extracts a contiguous segment
 * of {@link TimeLog} entries anchored to the start of a work shift.
 *
 * <p>
 * The extractor identifies the first {@link TimeLog} whose entry time falls on
 * the specified {@link LocalDate}, using the local date derived from the
 * configured {@link ZoneId}. This log is used as the anchor point.
 * </p>
 *
 * <p>
 * Once the anchor is found, the extractor determines the segment boundaries by
 * using long pauses (represented by {@link PauseInfo}) as separators. The
 * resulting segment includes all {@link TimeLog} entries between the closest
 * separator before the anchor and the closest separator at or after the anchor.
 * </p>
 *
 * <p>
 * If no anchor can be found for the given date, or if the calculated segment is
 * empty, an empty list is returned.
 * </p>
 */
final class ShiftStartAnchoredExtractor implements TimeLogsExtractor {

	/**
	 * Time zone used to convert {@link java.time.Instant} entry times into local
	 * dates when determining the anchor {@link TimeLog}.
	 */
	private final ZoneId zoneId;

	/**
	 * Creates a new extractor bound to the given time zone.
	 *
	 * @param zoneId the time zone used to resolve local dates from entry times;
	 *               can't be {@code null}
	 * @throws NullPointerException if {@code zoneId} is {@code null}
	 */
	ShiftStartAnchoredExtractor(final ZoneId zoneId) {
		this.zoneId = Objects.requireNonNull(zoneId, "zoneId must not be null");
	}

	/**
	 * Extracts a list of {@link TimeLog} entries for the work shift anchored on the
	 * specified date.
	 *
	 * <p>
	 * The method locates the first {@link TimeLog} whose entry time falls on the
	 * given {@code date} (according to the configured {@link ZoneId}). This log
	 * acts as the anchor. The returned list includes all logs between the nearest
	 * long pause before the anchor and the nearest long pause at or after the
	 * anchor.
	 * </p>
	 *
	 * <p>
	 * If no anchor is found, if the input list is empty, or if the computed range
	 * is invalid, an empty list is returned.
	 * </p>
	 * 
	 * Precondition:</br>
	 * - pauses must contain at least two pause</br>
	 * - timeLogs must contain at least two timeLog
	 *
	 * @param date     the date used to determine the shift start; can't be
	 *                 {@code null}
	 * @param timeLogs ordered list of time logs to extract from; can't be
	 *                 {@code null}
	 * @param pauses   list of long pauses used as segment separators; can't be
	 *                 {@code null}
	 * @return a list containing the extracted {@link TimeLog} segment, or an empty
	 *         list if no segment can be determined
	 * @throws NullPointerException  if any argument is {@code null}
	 * @throws IllegalStateException if preconditions are not met
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

		final TimeLog anchor = findAnchor(date, timeLogs);
		if (anchor == null) {
			return new TimeLogs(List.of());
		}

		final PauseInfo leftPause = findLastPauseBefore(anchor, pauses);
		final PauseInfo rightPause = findFirstPauseAfterOrAt(anchor, pauses);

		final int startIndex = (leftPause != null) ? indexOfOrFail(timeLogs, leftPause.after(), "left pause 'after'")
				: 0;

		final int endIndex = (rightPause != null) ? indexOfOrFail(timeLogs, rightPause.before(), "right pause 'before'")
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
			if (in != null && in.atZone(zoneId).toLocalDate().equals(date)) {
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
