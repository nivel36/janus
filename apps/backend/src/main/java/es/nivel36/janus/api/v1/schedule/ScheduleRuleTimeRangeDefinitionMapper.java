package es.nivel36.janus.api.v1.schedule;

import org.springframework.stereotype.Component;

import es.nivel36.janus.api.Mapper;
import es.nivel36.janus.service.schedule.ScheduleRuleTimeRangeDefinition;

@Component
public class ScheduleRuleTimeRangeDefinitionMapper
		implements Mapper<ScheduleRuleTimeRangeRequest, ScheduleRuleTimeRangeDefinition> {

	@Override
	public ScheduleRuleTimeRangeDefinition map(ScheduleRuleTimeRangeRequest object) {
		if (object == null) {
			return null;
		}
		return new ScheduleRuleTimeRangeDefinition(object.dayOfWeek(), object.effectiveWorkHours(),
				object.timeRange().startTime(), object.timeRange().endTime());
	}
}
