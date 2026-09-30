package es.nivel36.janus.api.v1.schedule;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;

import org.springframework.stereotype.Component;

import es.nivel36.janus.api.Mapper;
import es.nivel36.janus.service.schedule.ScheduleRuleTimeRangeDefinition;

@Component
public class ScheduleRuleTimeRangeDefinitionMapper
		implements Mapper<ScheduleRuleTimeRangeRequest, ScheduleRuleTimeRangeDefinition> {

	@Override
	public ScheduleRuleTimeRangeDefinition map(final ScheduleRuleTimeRangeRequest scheduleRuleTimeRangeRequest) {
		if (scheduleRuleTimeRangeRequest == null) {
			return null;
		}
		final DayOfWeek dayOfWeek = scheduleRuleTimeRangeRequest.dayOfWeek();
		final Duration effectiveWorkHours = scheduleRuleTimeRangeRequest.effectiveWorkHours();
		final ScheduleTimeRangeRequest timeRange = scheduleRuleTimeRangeRequest.timeRange();
		final LocalTime startTime = timeRange.startTime();
		final LocalTime endTime = timeRange.endTime();
		return new ScheduleRuleTimeRangeDefinition(dayOfWeek, effectiveWorkHours, startTime, endTime);
	}
}
