/*
 * Copyright 2026 Abel Ferrer Jiménez
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package es.nivel36.janus.api.v1.schedule;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;

import es.nivel36.janus.api.Mapper;
import es.nivel36.janus.policy.schedule.ScheduleAuthorizationAdapter;
import es.nivel36.janus.service.schedule.Schedule;
import es.nivel36.janus.service.schedule.ScheduleRuleDefinition;
import es.nivel36.janus.service.schedule.ScheduleService;

/**
 * Spring MVC implementation of {@link ScheduleResource}.
 */
@RestController
public class ScheduleController implements ScheduleResource {

	private static final Logger logger = LoggerFactory.getLogger(ScheduleController.class);

	private final ScheduleService scheduleService;
	private final ScheduleAuthorizationAdapter authorization;
	private final Mapper<Schedule, ScheduleResponse> scheduleResponseMapper;
	private final Mapper<Schedule, ScheduleSummaryResponse> scheduleSummaryResponseMapper;
	private final Mapper<ScheduleRuleRequest, ScheduleRuleDefinition> scheduleRuleDefinitionMapper;

	/**
	 * Creates a controller ready to delegate resource operations without accessing
	 * persistence during construction.
	 *
	 * @param  scheduleService               service for schedule operations; must
	 *                                       not be {@code null}
	 * @param  authorization                 component that determines the
	 *                                       authenticated user's schedule access;
	 *                                       must not be {@code null}
	 * @param  scheduleResponseMapper        mapper for schedule responses; must not
	 *                                       be {@code null}
	 * @param  scheduleSummaryResponseMapper mapper for schedule summaries; must not
	 *                                       be {@code null}
	 * @param  scheduleRuleDefinitionMapper  mapper for schedule rule definitions;
	 *                                       must not be {@code null}
	 * @throws NullPointerException          if any dependency is {@code null}
	 */
	public ScheduleController(
		final ScheduleService scheduleService,
		final ScheduleAuthorizationAdapter authorization,
		final @Qualifier("scheduleResponseMapper") Mapper<Schedule, ScheduleResponse> scheduleResponseMapper,
		final @Qualifier("scheduleSummaryResponseMapper") Mapper<Schedule, ScheduleSummaryResponse> scheduleSummaryResponseMapper,
		final @Qualifier("scheduleRuleDefinitionMapper") Mapper<ScheduleRuleRequest, ScheduleRuleDefinition> scheduleRuleDefinitionMapper) {
		this.scheduleService = Objects.requireNonNull(scheduleService, "scheduleService can't be null");
		this.authorization = Objects.requireNonNull(authorization, "authorization can't be null");
		this.scheduleResponseMapper = Objects
				.requireNonNull(scheduleResponseMapper, "scheduleResponseMapper can't be null");
		this.scheduleSummaryResponseMapper = Objects
				.requireNonNull(scheduleSummaryResponseMapper, "scheduleSummaryResponseMapper can't be null");
		this.scheduleRuleDefinitionMapper = Objects
				.requireNonNull(scheduleRuleDefinitionMapper, "scheduleRuleDefinitionMapper can't be null");
	}

	@Override
	public ResponseEntity<Page<ScheduleSummaryResponse>> searchSchedules(
			final String query,
			final String employeeNumber,
			final Pageable pageable,
			final Authentication authentication) {
		logger.debug("Search schedules ACTION performed");
		final String effectiveEmployeeNumber = this.authorization
				.effectiveEmployeeNumber(authentication, employeeNumber);

		final Page<ScheduleSummaryResponse> schedules = this.scheduleService
				.searchSchedules(query, effectiveEmployeeNumber, pageable).map(this.scheduleSummaryResponseMapper::map);
		return ResponseEntity.ok(schedules);
	}

	@Override
	public ResponseEntity<ScheduleResponse> findSchedule(final String scheduleCode) {
		logger.debug("Find schedule ACTION performed");

		final Schedule schedule = this.scheduleService.findScheduleByCode(scheduleCode);
		final ScheduleResponse response = this.scheduleResponseMapper.map(schedule);
		return ResponseEntity.ok(response);
	}

	@Override
	public ResponseEntity<ScheduleResponse> createSchedule(final CreateScheduleRequest request) {
		logger.debug("Create schedule ACTION performed");

		final String code = request.code().trim();
		final String name = request.name().trim();
		final Duration entryTolerance = request.entryTolerance();
		final Duration exitTolerance = request.exitTolerance();
		final List<ScheduleRuleDefinition> rules = this.mapRules(request.rules());
		final Schedule createdSchedule = this.scheduleService
				.createSchedule(code, name, entryTolerance, exitTolerance, rules);
		final ScheduleResponse response = this.scheduleResponseMapper.map(createdSchedule);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	private List<ScheduleRuleDefinition> mapRules(final List<ScheduleRuleRequest> rules) {
		return rules.stream().map(this.scheduleRuleDefinitionMapper::map).toList();
	}

	@Override
	public ResponseEntity<ScheduleResponse> updateSchedule(
			final String scheduleCode,
			final UpdateScheduleRequest request) {
		logger.debug("Update schedule ACTION performed");

		final String name = request.name().trim();
		final Duration entryTolerance = request.entryTolerance();
		final Duration exitTolerance = request.exitTolerance();
		final List<ScheduleRuleDefinition> rules = this.mapRules(request.rules());
		final Schedule updatedSchedule = this.scheduleService
				.updateSchedule(scheduleCode, name, entryTolerance, exitTolerance, rules);
		final ScheduleResponse response = this.scheduleResponseMapper.map(updatedSchedule);
		return ResponseEntity.ok(response);
	}

	@Override
	public ResponseEntity<Void> deleteSchedule(final String scheduleCode) {
		logger.debug("Delete schedule ACTION performed");

		this.scheduleService.deleteSchedule(scheduleCode);
		return ResponseEntity.noContent().build();
	}
}
