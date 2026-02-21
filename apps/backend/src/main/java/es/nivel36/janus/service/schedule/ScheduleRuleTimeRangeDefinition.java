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
package es.nivel36.janus.service.schedule;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;

/**
 * Domain-neutral DTO describing the timing constraints of a rule for a
 * particular day of the week.
 *
 * @param dayOfWeek          the day of the week when the shift starts
 * @param effectiveWorkHours intended working duration for the range
 * @param startTime          lower bound for the allowed time window
 * @param endTime            upper bound for the allowed time window
 */
public record ScheduleRuleTimeRangeDefinition( //
		DayOfWeek dayOfWeek, //
		Duration effectiveWorkHours, //
		LocalTime startTime, //
		LocalTime endTime) {
}
