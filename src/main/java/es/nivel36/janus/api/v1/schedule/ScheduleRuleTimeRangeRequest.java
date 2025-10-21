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
package es.nivel36.janus.api.v1.schedule;

import java.time.DayOfWeek;
import java.time.Duration;

import es.nivel36.janus.service.schedule.DayOfWeekTimeRange;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Defines the request payload for day-specific {@link DayOfWeekTimeRange}
 * definitions.
 *
 * @param dayOfWeek          day of the week when the shift starts; must not be
 *                           {@code null}
 * @param effectiveWorkHours effective working duration for the range as an
 *                           ISO-8601 {@link Duration}; must not be {@code null}
 * @param timeRange          allowed clock-in and clock-out bounds; must not be
 *                           {@code null}
 */
public record ScheduleRuleTimeRangeRequest( //
                @NotNull(message = "dayOfWeek must not be null") //
                DayOfWeek dayOfWeek, //

                @NotNull(message = "effectiveWorkHours must not be null") //
                Duration effectiveWorkHours, //

                @NotNull(message = "timeRange must not be null") //
                @Valid ScheduleTimeRangeRequest timeRange //
) {
}
