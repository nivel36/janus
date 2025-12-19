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

import jakarta.validation.constraints.NotNull;

/**
 * Request payload used to resolve a {@code ClockOutWithoutClockInEvent}.
 *
 * @param entryTime mandatory entry time that will be used to build the resolving
 *                  time log
 * @param reason    optional reason explaining why the event is being resolved
 */
public record ResolveClockOutWithoutClockInEventRequest(@NotNull Instant entryTime, String reason) {
}
