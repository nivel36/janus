package es.nivel36.janus.api.v1.schedule;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

import es.nivel36.janus.api.Mapper;
import es.nivel36.janus.service.schedule.ScheduleRuleDefinition;
import es.nivel36.janus.service.schedule.ScheduleRuleTimeRangeDefinition;

@Component
public class ScheduleRuleDefinitionMapper implements Mapper<ScheduleRuleRequest, ScheduleRuleDefinition> {

	private Mapper<ScheduleRuleTimeRangeRequest, ScheduleRuleTimeRangeDefinition> scheduleRuleTimeRangeDefinitionMapper;

	public ScheduleRuleDefinitionMapper(
			Mapper<ScheduleRuleTimeRangeRequest, ScheduleRuleTimeRangeDefinition> scheduleRuleTimeRangeDefinitionMapper) {
		this.scheduleRuleTimeRangeDefinitionMapper = Objects.requireNonNull(scheduleRuleTimeRangeDefinitionMapper,
				"scheduleRuleTimeRangeDefinitionMapper can't be null");
	}

	@Override
	public ScheduleRuleDefinition map(ScheduleRuleRequest object) {
		if (object == null) {
			return null;
		}
		return new ScheduleRuleDefinition(object.name(), object.startDate(), object.endDate(),
				mapScheduleRuleTimeRangeDefinition(object.dayOfWeekRanges()));
	}

	public List<ScheduleRuleTimeRangeDefinition> mapScheduleRuleTimeRangeDefinition(
			List<ScheduleRuleTimeRangeRequest> scheduleRuleTimeRangeRequest) {
		return scheduleRuleTimeRangeRequest.stream().map(scheduleRuleTimeRangeDefinitionMapper::map).toList();
	}
}
