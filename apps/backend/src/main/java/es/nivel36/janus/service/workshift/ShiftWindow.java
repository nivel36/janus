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

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Objects;

import es.nivel36.janus.service.schedule.TimeRange;

/**
 * Immutable half-open window for a scheduled shift in a specific time zone.
 * <p>
 * The start is inclusive and the end is exclusive. Overnight ranges end on the
 * following local date; local date-time boundaries are resolved using
 * {@link java.time.LocalDateTime#atZone(ZoneId)}.
 */
final class ShiftWindow {

	private final TimeInterval interval;

	private ShiftWindow(final TimeInterval interval) {
		this.interval = Objects.requireNonNull(interval, "interval can't be null");
	}

	/**
	 * Returns the scheduled shift window for the supplied local date and zone.
	 * <p>
	 * An end time before or equal to the start time belongs to the following day.
	 *
	 * @param  date                     the local date on which the shift starts;
	 *                                  must not be {@code null}
	 * @param  timeRange                the scheduled local time bounds; must not be
	 *                                  {@code null}
	 * @param  zoneId                   the zone used to resolve local date-times;
	 *                                  must not be {@code null}
	 * @return                          the scheduled window as absolute instants
	 * @throws NullPointerException     if any argument or required time bound is
	 *                                  {@code null}
	 * @throws IllegalArgumentException if the resolved end instant is before the
	 *                                  start
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
		return this.interval.expandBy(margin);
	}

	TimeInterval interval() {
		return this.interval;
	}
}
