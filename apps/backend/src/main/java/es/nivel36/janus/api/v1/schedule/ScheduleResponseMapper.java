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
package es.nivel36.janus.api.v1.schedule;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Component;

import es.nivel36.janus.api.Mapper;
import es.nivel36.janus.api.v1.schedule.ScheduleResponse.DayOfWeekTimeRangeResponse;
import es.nivel36.janus.api.v1.schedule.ScheduleResponse.ScheduleRuleResponse;
import es.nivel36.janus.api.v1.schedule.ScheduleResponse.TimeRangeResponse;
import es.nivel36.janus.service.schedule.DayOfWeekTimeRange;
import es.nivel36.janus.service.schedule.Schedule;
import es.nivel36.janus.service.schedule.ScheduleRule;

/**
 * Maps {@link Schedule} aggregates to {@link ScheduleResponse} DTOs.
 */
@Component
public class ScheduleResponseMapper implements Mapper<Schedule, ScheduleResponse> {

	@Override
	public ScheduleResponse map(final Schedule schedule) {
		if (schedule == null) {
			return null;
		}
		final List<ScheduleRuleResponse> rules = this.mapRules(schedule.getRules());
		return new ScheduleResponse(schedule.getCode(), schedule.getName(), rules);
	}

	private List<ScheduleRuleResponse> mapRules(final Set<ScheduleRule> rules) {
		if (rules == null || rules.isEmpty()) {
			return Collections.emptyList();
		}
		return rules.stream().filter(Objects::nonNull).map(this::mapRule).toList();
	}

	private ScheduleRuleResponse mapRule(final ScheduleRule rule) {
		final List<DayOfWeekTimeRangeResponse> dayOfWeekRanges = this.mapDayOfWeekRanges(rule.getDayOfWeekRanges());
		return new ScheduleRuleResponse(rule.getName(), rule.getStartDate(), rule.getEndDate(), dayOfWeekRanges);
	}

	private List<DayOfWeekTimeRangeResponse> mapDayOfWeekRanges(final List<DayOfWeekTimeRange> ranges) {
		if (ranges == null || ranges.isEmpty()) {
			return Collections.emptyList();
		}
		return ranges.stream().filter(Objects::nonNull).map(this::mapDayOfWeekRange).toList();
	}

	private DayOfWeekTimeRangeResponse mapDayOfWeekRange(final DayOfWeekTimeRange range) {
		final TimeRangeResponse timeRangeResponse;
		if (range.getTimeRange() != null) {
			timeRangeResponse = new TimeRangeResponse(range.getTimeRange().getStartTime(),
					range.getTimeRange().getEndTime());
		} else {
			timeRangeResponse = null;
		}
		return new DayOfWeekTimeRangeResponse(range.getDayOfWeek(), range.getEffectiveWorkHours(), timeRangeResponse);
	}
}
