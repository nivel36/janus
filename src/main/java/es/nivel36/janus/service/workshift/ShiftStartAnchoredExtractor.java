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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import es.nivel36.janus.service.timelog.TimeLog;
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
	 * @param date     the date used to determine the shift start; can't be
	 *                 {@code null}
	 * @param timeLogs ordered list of time logs to extract from; can't be
	 *                 {@code null}
	 * @param pauses   list of long pauses used as segment separators; can't be
	 *                 {@code null}
	 * @return a list containing the extracted {@link TimeLog} segment, or an empty
	 *         list if no segment can be determined
	 * @throws NullPointerException if any argument is {@code null}
	 */
	@Override
	public List<TimeLog> extract(final LocalDate date, final List<TimeLog> timeLogs, final List<PauseInfo> pauses) {
		Objects.requireNonNull(date, "date must not be null");
		Objects.requireNonNull(timeLogs, "timeLogs must not be null");
		Objects.requireNonNull(pauses, "pauses must not be null");

		if (timeLogs.isEmpty()) {
			return List.of();
		}

		// 1) Anchor: first log whose entry time falls on {@code date}
		// (using the worksite local time zone)
		final int anchorIndex = findAnchorIndex(date, timeLogs);
		if (anchorIndex < 0) {
			// No shift start exists on that day
			return List.of();
		}

		// 2) Determine segment boundaries using long pauses as separators
		// PauseInfo.index() represents the index of the log AFTER which a long pause
		// occurs.
		final List<PauseInfo> byIndex = new ArrayList<>(pauses);
		byIndex.sort(Comparator.comparingInt(PauseInfo::index));

		final int leftSeparator = findLastSeparatorBefore(anchorIndex, byIndex);
		final int rightSeparator = findFirstSeparatorAtOrAfter(anchorIndex, byIndex);

		final int startIndex = leftSeparator + 1;
		final int endIndexInclusive = (rightSeparator >= 0) ? rightSeparator : (timeLogs.size() - 1);

		if (startIndex > endIndexInclusive) {
			return List.of();
		}

		return new ArrayList<>(timeLogs.subList(startIndex, endIndexInclusive + 1));
	}

	/**
	 * Finds the index of the first {@link TimeLog} whose entry time falls on the
	 * specified date when converted to the configured {@link ZoneId}.
	 *
	 * @param date     the date to match against log entry times
	 * @param timeLogs ordered list of time logs to inspect
	 * @return the index of the first matching {@link TimeLog}, or {@code -1} if
	 *         none match
	 */
	private int findAnchorIndex(final LocalDate date, final List<TimeLog> timeLogs) {
		for (int i = 0; i < timeLogs.size(); i++) {
			final Instant in = timeLogs.get(i).getEntryTime();
			if (in == null) {
				continue;
			}
			final LocalDate local = in.atZone(zoneId).toLocalDate();
			if (local.equals(date)) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Finds the index of the last pause separator that occurs strictly before the
	 * given anchor index.
	 *
	 * @param anchorIndex   index of the anchor {@link TimeLog}
	 * @param pausesByIndex list of pauses sorted by {@link PauseInfo#index()}
	 * @return the separator index, or {@code -1} if none exists before the anchor
	 */
	private int findLastSeparatorBefore(final int anchorIndex, final List<PauseInfo> pausesByIndex) {
		int last = -1;
		for (final PauseInfo p : pausesByIndex) {
			if (p.index() < anchorIndex) {
				last = p.index();
			} else {
				break;
			}
		}
		return last;
	}

	/**
	 * Finds the index of the first pause separator that occurs at or after the
	 * given anchor index.
	 *
	 * @param anchorIndex   index of the anchor {@link TimeLog}
	 * @param pausesByIndex list of pauses sorted by {@link PauseInfo#index()}
	 * @return the separator index, or {@code -1} if none exists at or after the
	 *         anchor
	 */
	private int findFirstSeparatorAtOrAfter(final int anchorIndex, final List<PauseInfo> pausesByIndex) {
		for (final PauseInfo p : pausesByIndex) {
			if (p.index() >= anchorIndex) {
				return p.index();
			}
		}
		return -1;
	}
}
