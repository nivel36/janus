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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Represents an immutable collection of {@link TimeInterval} instances that are
 * automatically normalized by merging overlapping or touching intervals.
 *
 * <p>
 * This class provides aggregate operations over multiple time intervals, such
 * as computing the total covered duration and the total gap duration between
 * consecutive intervals.
 *
 * <p>
 * Instances of this class are immutable and thread-safe. The intervals are
 * guaranteed to be ordered by start time and non-overlapping.
 */
final class TimeIntervals {

	/**
	 * The normalized list of time intervals.
	 *
	 * <p>
	 * This list is immutable, sorted by {@link TimeInterval#startsAt()}, and
	 * contains no overlapping or touching intervals.
	 */
	private final List<TimeInterval> intervals;

	private TimeIntervals(final List<TimeInterval> intervals) {
		this.intervals = List.copyOf(intervals);
	}

	/**
	 * Creates a {@code TimeIntervals} instance from the specified list of
	 * intervals.
	 *
	 * <p>
	 * The provided intervals are merged if they overlap or touch, and the resulting
	 * collection is normalized before being stored.
	 *
	 * @param intervals the list of intervals to include
	 * @return a new {@code TimeIntervals} instance containing the merged intervals
	 * @throws NullPointerException if {@code intervals} is {@code null}
	 */
	static TimeIntervals of(final List<TimeInterval> intervals) {
		return new TimeIntervals(mergeInternal(intervals));
	}

	/**
	 * Returns the total duration covered by all intervals.
	 *
	 * <p>
	 * This is the sum of the durations of all normalized intervals.
	 *
	 * @return the total covered {@link Duration}, or {@link Duration#ZERO} if there
	 *         are no intervals
	 */
	Duration totalCoveredDuration() {
		return intervals.stream().map(TimeInterval::duration).reduce(Duration.ZERO, Duration::plus);
	}

	/**
	 * Returns the total duration of gaps between consecutive intervals.
	 *
	 * <p>
	 * The gap duration is defined as the time between the end of one interval and
	 * the start of the next interval in chronological order.
	 *
	 * @return the total gap {@link Duration}, or {@link Duration#ZERO} if there are
	 *         fewer than two intervals
	 */
	Duration totalGapDuration() {
		if (intervals.size() < 2) {
			return Duration.ZERO;
		}
		Duration total = Duration.ZERO;
		for (int i = 0; i < intervals.size() - 1; i++) {
			total = total.plus(Duration.between(intervals.get(i).endsAt(), intervals.get(i + 1).startsAt()));
		}
		return total;
	}

	private static List<TimeInterval> mergeInternal(final List<TimeInterval> intervals) {
		Objects.requireNonNull(intervals, "intervals must not be null.");
		if (intervals.isEmpty()) {
			return List.of();
		}

		final List<TimeInterval> sorted = new ArrayList<>(intervals);
		sorted.sort(Comparator.comparing(TimeInterval::startsAt));

		final List<TimeInterval> result = new ArrayList<>();
		TimeInterval current = sorted.getFirst();

		for (int i = 1; i < sorted.size(); i++) {
			final TimeInterval next = sorted.get(i);

			if (current.overlapsOrTouches(next)) {
				current = current.mergeWith(next);
			} else {
				result.add(current);
				current = next;
			}
		}

		result.add(current);
		return List.copyOf(result);
	}
}
