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
import java.util.Objects;

/**
 * Immutable policy object that defines the temporal thresholds used by shift
 * inference and aggregation logic.
 *
 * <p>
 * This policy is used to determine:
 * <ul>
 * <li>Which logs are considered relevant around a scheduled shift window.</li>
 * <li>When a pause between activities is long enough to infer a separation
 * between two distinct shifts.</li>
 * </ul>
 *
 * <p>
 * Instances of this record are immutable and validate that all thresholds are
 * non-negative and not {@code null}.
 *
 * @param selectionMargin    Temporal margin applied when selecting candidate
 *                           logs around a scheduled shift window. Can't be
 *                           {@code null} and must be non-negative.
 * @param longPauseThreshold Duration that defines when a pause is considered
 *                           long enough to separate two inferred shifts. Can't
 *                           be {@code null} and must be non-negative.
 */
record ShiftPolicy(Duration selectionMargin, Duration longPauseThreshold) {

	/**
	 * Creates a new {@code ShiftPolicy} with the specified thresholds.
	 *
	 * @param selectionMargin    Temporal margin used during scheduled shift
	 *                           selection. Can't be {@code null} and must be
	 *                           non-negative.
	 * @param longPauseThreshold Threshold that defines a long pause between shifts.
	 *                           Can't be {@code null} and must be non-negative.
	 *
	 * @throws NullPointerException     if {@code selectionMargin} or
	 *                                  {@code longPauseThreshold} is {@code null}
	 * @throws IllegalArgumentException if {@code selectionMargin} or
	 *                                  {@code longPauseThreshold} is negative
	 */
	ShiftPolicy {
		Objects.requireNonNull(selectionMargin, "selectionMargin must not be null.");
		Objects.requireNonNull(longPauseThreshold, "longPauseThreshold must not be null.");
		if (selectionMargin.isNegative()) {
			throw new IllegalArgumentException("selectionMargin must not be negative.");
		}
		if (longPauseThreshold.isNegative()) {
			throw new IllegalArgumentException("longPauseThreshold must not be negative.");
		}
	}

	/**
	 * Returns a default {@code ShiftPolicy} instance: 4 hours margin and 4 hours
	 * long-pause threshold.
	 *
	 * @return a default {@code ShiftPolicy} instance
	 */
	static ShiftPolicy defaultPolicy() {
		return new ShiftPolicy(Duration.ofHours(4), Duration.ofHours(4));
	}
}
