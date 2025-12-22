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
package es.nivel36.janus.service.schedule;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalTime;
import java.util.Objects;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;

/**
 * Represents a time range defined by a start time and an end time.
 *
 * <p>
 * A {@code TimeRange} models a contiguous period within a 24-hour day. It is
 * primarily used as a value object embedded in other entities, such as
 * {@link DayOfWeekTimeRange}, to express working hours or other time-based
 * constraints.
 * </p>
 *
 * <p>
 * The range may span across midnight. In such cases, the end time is considered
 * to belong to the following day (for example, {@code 22:00 → 06:00}).
 * </p>
 *
 * <p>
 * This class is {@link Embeddable} and has no independent identity. Equality and
 * hash code are based solely on the {@code startTime} and {@code endTime}
 * values.
 * </p>
 */
@Embeddable
public class TimeRange implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * The inclusive start time of the range.
	 *
	 * <p>
	 * Must not be {@code null}.
	 * </p>
	 */
	@NotNull
	private LocalTime startTime;

	/**
	 * The exclusive end time of the range.
	 *
	 * <p>
	 * Must not be {@code null}. If the end time is before the start time, the range
	 * is interpreted as spanning midnight.
	 * </p>
	 */
	@NotNull
	private LocalTime endTime;

	/**
	 * Protected no-argument constructor required by persistence frameworks.
	 *
	 * <p>
	 * This constructor must not be used directly in application code. It exists
	 * solely to allow frameworks such as JPA to instantiate the embeddable.
	 * </p>
	 */
	TimeRange() {
	}

	/**
	 * Creates a new {@code TimeRange} with the given start and end times.
	 *
	 * <p>
	 * The resulting range represents the interval {@code [startTime, endTime)}.
	 * If {@code endTime} is before {@code startTime}, the range is assumed to span
	 * across midnight into the next day.
	 * </p>
	 *
	 * @param startTime the start time of the range; must not be {@code null}
	 * @param endTime   the end time of the range; must not be {@code null}
	 *
	 * @throws NullPointerException     if {@code startTime} or {@code endTime} is
	 *                                  {@code null}
	 * @throws IllegalArgumentException if {@code startTime} and {@code endTime}
	 *                                  represent the same instant
	 */
	public TimeRange(final LocalTime startTime, final LocalTime endTime) {
		this.startTime = Objects.requireNonNull(startTime, "startTime can't be null");
		this.endTime = Objects.requireNonNull(endTime, "endTime can't be null");
		if (startTime.equals(endTime)) {
			throw new IllegalArgumentException("startTime and endTime must not be equal");
		}
	}

	/**
	 * Returns the duration of this time range.
	 *
	 * <p>
	 * If the range spans across midnight, the returned duration accounts for the
	 * wrap-around to the next day.
	 * </p>
	 *
	 * @return the {@link Duration} represented by this time range; never
	 *         {@code null}
	 */
	public Duration getDuration() {
		Duration duration = Duration.between(this.startTime, this.endTime);
		if (duration.isNegative()) {
			duration = duration.plusHours(24);
		}
		return duration;
	}

	/**
	 * Returns the start time of the range.
	 *
	 * @return the start time; never {@code null}
	 */
	public LocalTime getStartTime() {
		return this.startTime;
	}

	/**
	 * Returns the end time of the range.
	 *
	 * @return the end time; never {@code null}
	 */
	public LocalTime getEndTime() {
		return this.endTime;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.startTime, this.endTime);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || this.getClass() != obj.getClass()) {
			return false;
		}
		final TimeRange other = (TimeRange) obj;
		return Objects.equals(this.startTime, other.startTime)
				&& Objects.equals(this.endTime, other.endTime);
	}

	@Override
	public String toString() {
		return this.startTime + " to " + this.endTime;
	}
}
