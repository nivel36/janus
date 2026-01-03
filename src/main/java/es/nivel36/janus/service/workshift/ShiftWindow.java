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
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Objects;

import es.nivel36.janus.service.schedule.TimeRange;

/**
 * Immutable time window representing a scheduled shift, defined using
 * {@link Instant} boundaries.
 *
 * <p>
 * The window is delimited by a start instant (inclusive) and an end instant
 * that represents a hard boundary. While the end may be interpreted as
 * exclusive or inclusive by convention, this implementation does not allow any
 * instant strictly after {@code end} to be considered part of the window.
 *
 * <p>
 * Instances of this record are immutable and validate that the end instant is
 * not before the start instant.
 *
 * @param start The start of the shift window (inclusive). Can't be
 *              {@code null}.
 * @param end   The end of the shift window. Can't be {@code null} and must not
 *              be before {@code start}.
 */
final class ShiftWindow {

	private final TimeInterval interval;

	private ShiftWindow(final TimeInterval interval) {
		this.interval = Objects.requireNonNull(interval, "interval can't be null");
	}

	/**
	 * Computes the scheduled shift window for the given date and time range.
	 *
	 * <p>
	 * If the end time occurs before the start time, the end of the window is
	 * assumed to fall on the following day.
	 *
	 * @param worksite  Worksite providing the time zone context. Can't be
	 *                  {@code null}.
	 * @param date      Local date expressed in the worksite time zone. Can't be
	 *                  {@code null}.
	 * @param timeRange Scheduled time range within the given date. Can't be
	 *                  {@code null}.
	 *
	 * @return A {@link ShiftWindow} representing the scheduled shift as absolute
	 *         instants
	 *
	 * @throws NullPointerException if {@code worksite}, {@code date},
	 *                              {@code timeRange}, or any of their required
	 *                              components is {@code null}
	 */
	static ShiftWindow scheduled(final LocalDate date, final TimeRange timeRange, final ZoneId zoneId) {
		Objects.requireNonNull(date);
		Objects.requireNonNull(timeRange);
		Objects.requireNonNull(zoneId);
		final LocalTime startLocal = timeRange.getStartTime();
		final LocalTime endLocal = timeRange.getEndTime();
		final Instant start = date.atTime(startLocal).atZone(zoneId).toInstant();
		final LocalDate endDate = startLocal.isBefore(endLocal) ? date : date.plusDays(1);
		final Instant end = endDate.atTime(endLocal).atZone(zoneId).toInstant();
		return new ShiftWindow(new TimeInterval(start, end));
	}

	TimeInterval expandedBy(final Duration margin) {
		return interval.expandBy(margin);
	}

	TimeInterval interval() {
		return interval;
	}
}
