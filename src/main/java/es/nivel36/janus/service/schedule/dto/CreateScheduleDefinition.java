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
package es.nivel36.janus.service.schedule.dto;

import java.util.List;

/**
 * Domain-neutral DTO describing the data required to create a schedule.
 *
 * @param code  unique business identifier assigned to the schedule
 * @param name  human readable name describing the schedule
 * @param rules collection of rule definitions associated with the schedule
 */
public record CreateScheduleDefinition( //
		String code, //
		String name, //
		List<ScheduleRuleDefinition> rules) {
}
