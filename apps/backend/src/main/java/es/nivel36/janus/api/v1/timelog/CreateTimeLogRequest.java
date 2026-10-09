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
package es.nivel36.janus.api.v1.timelog;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnore;

import es.nivel36.janus.service.timelog.TimeLog;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload for creating a closed {@link TimeLog}.
 * <p>
 * Validation requires both instants and an exit strictly after entry.
 * Construction alone does not validate these conditions.
 *
 * @param entryTime the clock-in instant; must not be {@code null} when
 *                  validated
 * @param exitTime  the clock-out instant; must be non-null and strictly after
 *                  {@code entryTime} when validated
 */
public record CreateTimeLogRequest(@NotNull(message = "entryTime must not be null")
Instant entryTime, @NotNull(message = "exitTime must not be null")
Instant exitTime) {

	/**
	 * Returns whether {@code exitTime} is after {@code entryTime} when both are
	 * provided.
	 *
	 * @return {@code true} if the time range is valid or incomplete, {@code false}
	 *         otherwise
	 */
	@JsonIgnore
	@AssertTrue(message = "exitTime must be after entryTime")
	public boolean isTimeRangeValid() {
		if (this.entryTime == null || this.exitTime == null) {
			return true;
		}
		return this.exitTime.isAfter(this.entryTime);
	}
}
