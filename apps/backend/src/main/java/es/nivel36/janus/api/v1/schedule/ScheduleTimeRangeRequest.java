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
package es.nivel36.janus.api.v1.schedule;

import java.time.Duration;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import es.nivel36.janus.service.schedule.TimeRange;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

/**
 * Defines the bounds of a {@link TimeRange} in schedule requests.
 *
 * @param startTime lower bound for the allowed time window; must not be {@code null}
 * @param endTime   upper bound for the allowed time window; must not be {@code null} and must be
 *                  after {@code startTime}
 */
public record ScheduleTimeRangeRequest( //
		@NotNull(message = "startTime must not be null") //
		LocalTime startTime, //

		@NotNull(message = "endTime must not be null") //
		LocalTime endTime //
) {

	/**
	 * Validates that {@code endTime} is after {@code startTime} when both are
	 * provided.
	 *
	 * @return {@code true} if the time range is valid or incomplete, {@code false}
	 *         otherwise
	 */
	@JsonIgnore
	@AssertTrue(message = "endTime must be after startTime")
	public boolean isTimeRangeValid() {
		if (this.startTime == null || this.endTime == null) {
			return true;
		}
		return this.endTime.isAfter(this.startTime);
	}

	/** Returns the duration represented by this request. */
	@JsonIgnore
	public Duration duration() {
		if (this.startTime == null || this.endTime == null || !isTimeRangeValid()) {
			return Duration.ZERO;
		}
		return Duration.between(this.startTime, this.endTime);
	}
}
