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

import java.util.List;

import es.nivel36.janus.service.schedule.Schedule;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Request payload used to update an existing {@link Schedule} aggregate.
 *
 * @param name  new human readable name describing the schedule; must contain
 *              between 1 and 250 allowed characters
 * @param rules collection of rule definitions that replace the previous ones;
 *              must not be {@code null}
 */
public record UpdateScheduleRequest( //
                @NotBlank(message = "name must not be blank") //
                @Pattern( //
                                regexp = "^[\\p{L}0-9 _'.,-]{1,250}$", //
                                message = "name must contain only letters, digits, spaces, and basic punctuation (max 250)" //
                ) //
                String name, //

                @NotNull(message = "rules must not be null") //
                List<@Valid ScheduleRuleRequest> rules //
) {
}
