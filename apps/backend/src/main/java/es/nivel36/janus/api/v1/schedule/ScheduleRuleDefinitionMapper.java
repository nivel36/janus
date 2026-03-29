package es.nivel36.janus.api.v1.schedule;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

import es.nivel36.janus.api.Mapper;
import es.nivel36.janus.service.schedule.ScheduleRuleDefinition;
import es.nivel36.janus.service.schedule.ScheduleRuleTimeRangeDefinition;

@Component
public class ScheduleRuleDefinitionMapper implements Mapper<ScheduleRuleRequest, ScheduleRuleDefinition> {

	private final Mapper<ScheduleRuleTimeRangeRequest, ScheduleRuleTimeRangeDefinition> scheduleRuleTimeRangeDefinitionMapper;

	public ScheduleRuleDefinitionMapper(
			final Mapper<ScheduleRuleTimeRangeRequest, ScheduleRuleTimeRangeDefinition> scheduleRuleTimeRangeDefinitionMapper) {
		this.scheduleRuleTimeRangeDefinitionMapper = Objects.requireNonNull(scheduleRuleTimeRangeDefinitionMapper,
				"scheduleRuleTimeRangeDefinitionMapper can't be null");
	}

	@Override
	public ScheduleRuleDefinition map(final ScheduleRuleRequest object) {
		if (object == null) {
			return null;
		}
		return new ScheduleRuleDefinition(object.name(), object.startDate(), object.endDate(),
				this.mapScheduleRuleTimeRangeDefinition(object.dayOfWeekRanges()));
	}

	public List<ScheduleRuleTimeRangeDefinition> mapScheduleRuleTimeRangeDefinition(
			final List<ScheduleRuleTimeRangeRequest> scheduleRuleTimeRangeRequest) {
		return scheduleRuleTimeRangeRequest.stream().map(this.scheduleRuleTimeRangeDefinitionMapper::map).toList();
	}
}
