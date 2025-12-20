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

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

import es.nivel36.janus.service.schedule.dto.CreateScheduleDefinition;
import es.nivel36.janus.service.schedule.dto.ScheduleRuleDefinition;
import es.nivel36.janus.service.schedule.dto.ScheduleRuleTimeRangeDefinition;
import es.nivel36.janus.service.schedule.dto.UpdateScheduleDefinition;

/**
 * Maps schedule-related HTTP requests into domain-neutral DTOs that can be
 * consumed by the application layer.
 */
@Component
public class ScheduleRequestMapper {

	/**
	 * Builds a {@link CreateScheduleDefinition} from the incoming request payload.
	 *
	 * @param request validated request describing the schedule to create; must not
	 *                be {@code null}
	 * @return a domain-neutral DTO
	 */
	public CreateScheduleDefinition toCreateDefinition(final CreateScheduleRequest request) {
		Objects.requireNonNull(request, "request can't be null");
		final List<ScheduleRuleDefinition> ruleDefinitions = this.toRuleDefinitions(request.rules());
		return new CreateScheduleDefinition(request.code(), request.name(), ruleDefinitions);
	}

	/**
	 * Builds an {@link UpdateScheduleDefinition} from the incoming request payload.
	 *
	 * @param request validated request describing the schedule to update; must not
	 *                be {@code null}
	 * @return a domain-neutral DTO
	 */
	public UpdateScheduleDefinition toUpdateDefinition(final UpdateScheduleRequest request) {
		Objects.requireNonNull(request, "request can't be null");
		final List<ScheduleRuleDefinition> ruleDefinitions = this.toRuleDefinitions(request.rules());
		return new UpdateScheduleDefinition(request.name(), ruleDefinitions);
	}

	private List<ScheduleRuleDefinition> toRuleDefinitions(final List<ScheduleRuleRequest> rules) {
		if (rules == null) {
			return Collections.emptyList();
		}
		return rules.stream() //
				.filter(Objects::nonNull) //
				.map(this::toRuleDefinition) //
				.toList();
	}

	private ScheduleRuleDefinition toRuleDefinition(final ScheduleRuleRequest ruleRequest) {
		final List<ScheduleRuleTimeRangeDefinition> dayOfWeekRanges = this
				.toRuleTimeRangeDefinitions(ruleRequest.dayOfWeekRanges());
		return new ScheduleRuleDefinition(ruleRequest.name(), ruleRequest.startDate(), ruleRequest.endDate(),
				dayOfWeekRanges);
	}

	private List<ScheduleRuleTimeRangeDefinition> toRuleTimeRangeDefinitions(
			final List<ScheduleRuleTimeRangeRequest> timeRanges) {
		if (timeRanges == null) {
			return Collections.emptyList();
		}
		return timeRanges.stream() //
				.filter(Objects::nonNull) //
				.map(this::toRuleTimeRangeDefinition) //
				.toList();
	}

	private ScheduleRuleTimeRangeDefinition toRuleTimeRangeDefinition(final ScheduleRuleTimeRangeRequest request) {
		final ScheduleTimeRangeRequest timeRange = request.timeRange();
		return new ScheduleRuleTimeRangeDefinition(request.dayOfWeek(), request.effectiveWorkHours(),
				timeRange.startTime(), timeRange.endTime());
	}
}
