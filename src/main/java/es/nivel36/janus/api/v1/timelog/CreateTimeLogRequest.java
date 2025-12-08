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
package es.nivel36.janus.api.v1.timelog;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnore;

import es.nivel36.janus.service.timelog.TimeLog;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload for creating a new {@link TimeLog}.
 * <p>
 * This record encapsulates the entry and exit instants associated with a time
 * log. Both fields are non {@code null} and in UTC (ISO-8601) format (e.g.
 * 2025-09-12T08:30:00Z).
 * <p>
 * The entry time must be before the exit time.
 * 
 * @param entryTime the entry instant in UTC (ISO-8601); can't be {@code null}
 * @param exitTime  the exit instant in UTC (ISO-8601); must be after entryTime
 *                  and can't be {@code null}
 */
public record CreateTimeLogRequest(@NotNull Instant entryTime, @NotNull Instant exitTime) {

	/**
	 * Validates that {@code exitTime} is not before {@code entryTime} when both are
	 * provided.
	 *
	 * @return {@code true} if the date range is valid or incomplete, {@code false}
	 *         otherwise
	 */
	@JsonIgnore
	@AssertTrue(message = "exitTime must be after entryTime")
	public boolean isTimeRangeValid() {
		if (this.entryTime == null || this.exitTime == null) {
			return true;
		}
		return exitTime.isAfter(entryTime);
	}
}
