package es.nivel36.janus.api.v1.schedule;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import es.nivel36.janus.api.Mapper;
import es.nivel36.janus.service.schedule.ScheduleRuleDefinition;
import es.nivel36.janus.service.schedule.ScheduleRuleTimeRangeDefinition;

/**
 * Maps schedule rule requests to definitions under the {@link Mapper} contract.
 * <p>
 * Non-null requests must contain a non-null range list. Range order is
 * preserved; this mapper does not validate the request or trim its name.
 */
@Component
public class ScheduleRuleDefinitionMapper implements Mapper<ScheduleRuleRequest, ScheduleRuleDefinition> {

	private final Mapper<ScheduleRuleTimeRangeRequest, ScheduleRuleTimeRangeDefinition> scheduleRuleTimeRangeDefinitionMapper;

	/**
	 * Constructs a mapper for schedule rules and their day-specific ranges.
	 *
	 * @param  scheduleRuleTimeRangeDefinitionMapper the mapper for individual
	 *                                               ranges; must not be
	 *                                               {@code null}
	 * @throws NullPointerException                  if the range mapper is
	 *                                               {@code null}
	 */
	public ScheduleRuleDefinitionMapper(
		final @Qualifier("scheduleRuleTimeRangeDefinitionMapper") Mapper<ScheduleRuleTimeRangeRequest, ScheduleRuleTimeRangeDefinition> scheduleRuleTimeRangeDefinitionMapper) {
		this.scheduleRuleTimeRangeDefinitionMapper = Objects.requireNonNull(
				scheduleRuleTimeRangeDefinitionMapper,
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

	/**
	 * Returns definitions for the supplied ranges in encounter order.
	 *
	 * @param  scheduleRuleTimeRangeRequest the range requests; must not be
	 *                                      {@code null}
	 * @return                              an unmodifiable list of mapped
	 *                                      definitions
	 * @throws NullPointerException         if the list is {@code null}
	 */
	public List<ScheduleRuleTimeRangeDefinition> mapScheduleRuleTimeRangeDefinition(
			final List<ScheduleRuleTimeRangeRequest> scheduleRuleTimeRangeRequest) {
		return scheduleRuleTimeRangeRequest.stream().map(this.scheduleRuleTimeRangeDefinitionMapper::map).toList();
	}
}
