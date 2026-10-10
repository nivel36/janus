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

/**
 * Response payload describing a {@code ClockOutWithoutClockInEvent}.
 *
 * @param employee    the employee's number and full name
 * @param worksite    the worksite's code, name and time zone
 * @param exitTime    instant when the employee clocked out without a previous
 *                    clock-in
 * @param detectedAt  instant when the anomaly was detected
 * @param resolved    indicates whether the event has been resolved
 * @param invalidated indicates whether the event has been invalidated
 * @param reason      optional reason explaining the resolution or invalidation
 * @param resolution  the supplied clock-in and resulting duration; {@code null}
 *                    when the event has not been resolved
 */
public record ClockOutWithoutClockInEventResponse(
		TimeLogEmployeeResponse employee,
		TimeLogWorksiteResponse worksite,
		Instant exitTime,
		Instant detectedAt,
		boolean resolved,
		boolean invalidated,
		String reason,
		ClockOutWithoutClockInEventResolutionResponse resolution) {
}
