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

import java.time.LocalDate;
import java.util.List;

import es.nivel36.janus.service.schedule.ScheduleRule;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Defines the structure of a rule contained in {@link CreateScheduleRequest} or
 * {@link UpdateScheduleRequest}.
 *
 * @param name            human readable name of the rule; must contain between 1
 *                        and 250 allowed characters
 * @param startDate       optional start date delimiting when the rule becomes
 *                        active
 * @param endDate         optional end date delimiting when the rule stops being
 *                        active
 * @param dayOfWeekRanges day specific working ranges that compose the rule; must
 *                        not be {@code null}
 */
public record ScheduleRuleRequest( //
                @NotBlank(message = "name must not be blank") //
                @Pattern( //
                                regexp = "^[\\p{L}0-9 _'.,-]{1,250}$", //
                                message = "name must contain only letters, digits, spaces, and basic punctuation (max 250)" //
                ) //
                String name, //

                LocalDate startDate, //

                LocalDate endDate, //

                @NotNull(message = "dayOfWeekRanges must not be null") //
                List<@Valid ScheduleRuleTimeRangeRequest> dayOfWeekRanges //
) {

        /**
         * Validates that {@code endDate} is not before {@code startDate} when both are
         * provided.
         *
         * @return {@code true} if the date range is valid or incomplete, {@code false}
         *         otherwise
         */
        @AssertTrue(message = "endDate must be on or after startDate")
        public boolean isDateRangeValid() {
                if (this.startDate == null || this.endDate == null) {
                        return true;
                }
                return !this.endDate.isBefore(this.startDate);
        }
}
