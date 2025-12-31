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
import java.util.Objects;

/**
 * Represents an immutable half-open time interval {@code [start, end)} defined by
 * two {@link Instant} values.
 *
 * <p>The interval includes the {@code start} instant and excludes the {@code end}
 * instant. Instances of this class are immutable and thread-safe.
 *
 * <p>This class provides utility methods to determine temporal relationships
 * between intervals (such as overlap or adjacency), to merge or intersect intervals,
 * and to derive new intervals based on an existing one.
 */
class TimeInterval {

	/**
	 * The starting instant of the interval (inclusive).
	 * Can't be {@code null}.
	 */
	private final Instant start;

	/**
	 * The ending instant of the interval (exclusive).
	 * Can't be {@code null} and must not be before {@link #start}.
	 */
	private final Instant end;

	/**
	 * Creates a new {@code TimeInterval} with the specified start and end instants.
	 *
	 * @param start the starting instant of the interval (inclusive)
	 * @param end the ending instant of the interval (exclusive)
	 * @throws NullPointerException if {@code start} or {@code end} is {@code null}
	 * @throws IllegalArgumentException if {@code end} is before {@code start}
	 */
	TimeInterval(final Instant start, final Instant end) {
		this.start = Objects.requireNonNull(start, "start must not be null.");
		this.end = Objects.requireNonNull(end, "end must not be null.");
		if (end.isBefore(start)) {
			throw new IllegalArgumentException("end must not be before start.");
		}
	}

	/**
	 * Determines whether this interval overlaps the specified interval.
	 *
	 * <p>Two intervals overlap if they share any instant in time.
	 * Adjacent intervals (where one ends exactly when the other starts)
	 * are <em>not</em> considered overlapping.
	 *
	 * @param other the interval to test for overlap
	 * @return {@code true} if the intervals overlap, {@code false} otherwise
	 * @throws NullPointerException if {@code other} is {@code null}
	 */
	boolean overlaps(final TimeInterval other) {
	    Objects.requireNonNull(other, "other must not be null.");
	    return this.start.isBefore(other.end) && other.start.isBefore(this.end);
	}

	/**
	 * Determines whether this interval touches the specified interval.
	 *
	 * <p>Two intervals touch if the end of one interval is exactly equal
	 * to the start of the other, without overlapping.
	 *
	 * @param other the interval to test for adjacency
	 * @return {@code true} if the intervals touch, {@code false} otherwise
	 * @throws NullPointerException if {@code other} is {@code null}
	 */
	boolean touches(final TimeInterval other) {
	    Objects.requireNonNull(other, "other must not be null.");
	    return this.end.equals(other.start) || other.end.equals(this.start);
	}

	/**
	 * Determines whether this interval either overlaps or touches
	 * the specified interval.
	 *
	 * @param other the interval to test
	 * @return {@code true} if the intervals overlap or touch,
	 *         {@code false} otherwise
	 * @throws NullPointerException if {@code other} is {@code null}
	 */
	boolean overlapsOrTouches(final TimeInterval other) {
	    Objects.requireNonNull(other, "other must not be null.");
	    return overlaps(other) || touches(other);
	}

	/**
	 * Merges this interval with the specified interval.
	 *
	 * <p>The resulting interval spans from the earliest start instant
	 * to the latest end instant of both intervals.
	 *
	 * @param other the interval to merge with
	 * @return a new {@code TimeInterval} representing the merged interval
	 * @throws NullPointerException if {@code other} is {@code null}
	 * @throws IllegalArgumentException if the intervals do not overlap
	 *                                  or touch
	 */
	TimeInterval mergeWith(final TimeInterval other) {
		Objects.requireNonNull(other);
		if (!overlapsOrTouches(other)) {
			throw new IllegalArgumentException("Intervals do not overlap or touch.");
		}
		final Instant mergedStart = this.start.isBefore(other.start) ? this.start : other.start;
		final Instant mergedEnd = this.end.isAfter(other.end) ? this.end : other.end;
		return new TimeInterval(mergedStart, mergedEnd);
	}

	/**
	 * Computes the intersection of this interval with the specified interval.
	 *
	 * @param interval the interval to intersect with
	 * @return a new {@code TimeInterval} representing the intersection,
	 *         or {@code null} if the intervals do not overlap
	 * @throws NullPointerException if {@code interval} is {@code null}
	 */
	TimeInterval intersect(final TimeInterval interval) {
		Objects.requireNonNull(interval, "interval must not be null.");
		final Instant s = this.start.isBefore(interval.start) ? interval.start : this.start;
		final Instant e = this.end.isAfter(interval.end) ? interval.end : this.end;
		return !e.isAfter(s) ? null : new TimeInterval(s, e);
	}

	/**
	 * Expands this interval by the specified duration on both sides.
	 *
	 * <p>The returned interval starts {@code margin} earlier and ends
	 * {@code margin} later than this interval.
	 *
	 * @param margin the duration to expand the interval by
	 * @return a new expanded {@code TimeInterval}
	 * @throws NullPointerException if {@code margin} is {@code null}
	 */
	TimeInterval expandBy(final Duration margin) {
		Objects.requireNonNull(margin);
		return new TimeInterval(start.minus(margin), end.plus(margin));
	}

	/**
	 * Determines whether this interval ends before the specified instant.
	 *
	 * @param instant the instant to compare with
	 * @return {@code true} if this interval ends before {@code instant},
	 *         {@code false} otherwise
	 */
	boolean endsAtOrBefore(final Instant instant) {
		return end.isBefore(instant);
	}

	/**
	 * Determines whether this interval starts after the specified instant.
	 *
	 * @param instant the instant to compare with
	 * @return {@code true} if this interval starts after {@code instant},
	 *         {@code false} otherwise
	 */
	boolean startsAtOrAfter(final Instant instant) {
		return start.isAfter(instant);
	}

	/**
	 * Returns the starting instant of this interval.
	 *
	 * @return the start instant (inclusive)
	 */
	Instant startsAt() {
		return start;
	}

	/**
	 * Returns the ending instant of this interval.
	 *
	 * @return the end instant (exclusive)
	 */
	Instant endsAt() {
		return end;
	}

	/**
	 * Returns the duration of this interval.
	 *
	 * @return the {@link Duration} between {@link #startsAt()} and {@link #endsAt()}
	 */
	Duration duration() {
		return Duration.between(this.start, this.end);
	}
}
