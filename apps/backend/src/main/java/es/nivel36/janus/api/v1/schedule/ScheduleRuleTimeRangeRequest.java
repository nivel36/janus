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
package es.nivel36.janus.api.v1.schedule;

import java.time.DayOfWeek;
import java.time.Duration;

import com.fasterxml.jackson.annotation.JsonIgnore;

import es.nivel36.janus.service.schedule.DayOfWeekTimeRange;
import es.nivel36.janus.validation.NonNegativeDuration;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

/**
 * Defines the request payload for day-specific {@link DayOfWeekTimeRange}
 * definitions.
 * <p>
 * The constraints below apply during Bean Validation. Construction alone stores
 * the supplied values without validation or normalization.
 *
 * @param dayOfWeek          day of the week when the shift starts; must not be
 *                           {@code null}
 * @param effectiveWorkHours effective working duration for the range as an
 *                           ISO-8601 {@link Duration}; must not be
 *                           {@code null}, negative, or greater than the
 *                           time-range duration
 * @param timeRange          allowed clock-in and clock-out bounds; must not be
 *                           {@code null}
 */
public record ScheduleRuleTimeRangeRequest(@NotNull(message = "dayOfWeek must not be null")
DayOfWeek dayOfWeek,

		@NotNull(message = "effectiveWorkHours must not be null")
		@NonNegativeDuration
		Duration effectiveWorkHours,

		@NotNull(message = "timeRange must not be null")
		@Valid
		ScheduleTimeRangeRequest timeRange) {

	/**
	 * Returns whether effective work fits within the requested range duration.
	 * <p>
	 * An absent duration or range is accepted here and rejected by its
	 * required-value constraint. A range with missing or equal bounds has zero
	 * duration for this comparison.
	 *
	 * @return {@code true} if a required value is absent or effective work does not
	 *         exceed the range duration; {@code false} otherwise
	 */
	@JsonIgnore
	@AssertTrue(message = "effectiveWorkHours must not exceed the time range duration")
	public boolean isEffectiveWorkHoursWithinTimeRange() {
		return this.effectiveWorkHours == null || this.timeRange == null
				|| this.effectiveWorkHours.compareTo(this.timeRange.duration()) <= 0;
	}
}
