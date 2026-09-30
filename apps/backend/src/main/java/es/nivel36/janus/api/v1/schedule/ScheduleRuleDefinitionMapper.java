package es.nivel36.janus.api.v1.schedule;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import es.nivel36.janus.api.Mapper;
import es.nivel36.janus.service.schedule.ScheduleRuleDefinition;
import es.nivel36.janus.service.schedule.ScheduleRuleTimeRangeDefinition;

@Component
public class ScheduleRuleDefinitionMapper implements Mapper<ScheduleRuleRequest, ScheduleRuleDefinition> {

	private final Mapper<ScheduleRuleTimeRangeRequest, ScheduleRuleTimeRangeDefinition> scheduleRuleTimeRangeDefinitionMapper;

	public ScheduleRuleDefinitionMapper(
			final @Qualifier("scheduleRuleTimeRangeDefinitionMapper") Mapper<ScheduleRuleTimeRangeRequest, ScheduleRuleTimeRangeDefinition> scheduleRuleTimeRangeDefinitionMapper) {
		this.scheduleRuleTimeRangeDefinitionMapper = Objects.requireNonNull( //
				scheduleRuleTimeRangeDefinitionMapper, //
				"scheduleRuleTimeRangeDefinitionMapper can't be null");
	}

	@Override
	public ScheduleRuleDefinition map(final ScheduleRuleRequest scheduleRuleRequest) {
		if (scheduleRuleRequest == null) {
			return null;
		}
		final String name = scheduleRuleRequest.name();
		final LocalDate startDate = scheduleRuleRequest.startDate();
		final LocalDate endDate = scheduleRuleRequest.endDate();
		final List<ScheduleRuleTimeRangeDefinition> scheduleRuleTimeRangeDefinitions = this
				.mapScheduleRuleTimeRangeDefinition(scheduleRuleRequest.dayOfWeekRanges());
		return new ScheduleRuleDefinition(name, startDate, endDate, scheduleRuleTimeRangeDefinitions);
	}

	public List<ScheduleRuleTimeRangeDefinition> mapScheduleRuleTimeRangeDefinition(
			final List<ScheduleRuleTimeRangeRequest> scheduleRuleTimeRangeRequest) {
		return scheduleRuleTimeRangeRequest.stream().map(this.scheduleRuleTimeRangeDefinitionMapper::map).toList();
	}
}
