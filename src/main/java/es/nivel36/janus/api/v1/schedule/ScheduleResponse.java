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
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import es.nivel36.janus.service.schedule.Schedule;

/**
 * API response representing a {@link Schedule} aggregate and its rules.
 *
 * @param code  unique business identifier of the schedule
 * @param name  human readable name describing the schedule
 * @param rules rules associated with the schedule
 */
public record ScheduleResponse(String code, String name, List<ScheduleRuleResponse> rules) {

        /**
         * Response fragment describing a single rule inside a schedule.
         *
         * @param name            human readable name of the rule
         * @param startDate       optional start date delimiting when the rule becomes
         *                        active
         * @param endDate         optional end date delimiting when the rule stops being
         *                        active
         * @param dayOfWeekRanges day specific working ranges belonging to the rule
         */
        public record ScheduleRuleResponse(String name, LocalDate startDate, LocalDate endDate,
                        List<DayOfWeekTimeRangeResponse> dayOfWeekRanges) {
        }

        /**
         * Response fragment describing the time range allowed for a specific day of the
         * week.
         *
         * @param dayOfWeek          day when the shift starts
         * @param effectiveWorkHours effective working duration expected for the shift
         * @param timeRange          allowed start and end times for the shift
         */
        public record DayOfWeekTimeRangeResponse(DayOfWeek dayOfWeek, Duration effectiveWorkHours,
                        TimeRangeResponse timeRange) {
        }

        /**
         * Response fragment representing the start and end times of a shift.
         *
         * @param startTime lower bound for the shift
         * @param endTime   upper bound for the shift
         */
        public record TimeRangeResponse(LocalTime startTime, LocalTime endTime) {
        }
}
