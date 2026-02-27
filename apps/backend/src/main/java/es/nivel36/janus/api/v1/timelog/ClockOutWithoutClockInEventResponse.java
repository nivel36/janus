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
package es.nivel36.janus.api.v1.timelog;

import java.time.Instant;

/**
 * Response payload describing a {@code ClockOutWithoutClockInEvent}.
 *
 * @param employeeEmail           email of the employee associated with the
 *                                event
 * @param worksiteCode            code of the worksite where the event occurred
 * @param exitTime                instant when the employee clocked out without
 *                                a previous clock-in
 * @param detectedAt              instant when the anomaly was detected
 * @param resolved                indicates whether the event has been resolved
 * @param invalidated             indicates whether the event has been
 *                                invalidated
 * @param reason                  optional reason explaining the resolution or
 *                                invalidation
 * @param resolvedTimeLogEntry    entry time of the resolving time log, if
 *                                present
 * @param resolvedTimeLogExitTime exit time of the resolving time log, if
 *                                present
 */
public record ClockOutWithoutClockInEventResponse(String employeeEmail, String worksiteCode, Instant exitTime,
		Instant detectedAt, boolean resolved, boolean invalidated, String reason, Instant resolvedTimeLogEntry,
		Instant resolvedTimeLogExitTime) {
}
