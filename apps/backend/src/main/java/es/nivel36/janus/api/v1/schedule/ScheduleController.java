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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;

import es.nivel36.janus.api.Mapper;
import es.nivel36.janus.policy.schedule.ScheduleAuthorizationAdapter;
import es.nivel36.janus.service.employee.EmployeeService;
import es.nivel36.janus.service.schedule.Schedule;
import es.nivel36.janus.service.schedule.ScheduleRuleDefinition;
import es.nivel36.janus.service.schedule.ScheduleService;
import es.nivel36.janus.util.EmailAddresses;

/**
 * REST controller responsible for exposing CRUD operations for {@link Schedule}
 * aggregates.
 *
 * <p>
 * This controller provides endpoints to create, retrieve, update, delete, and
 * search schedules. It enforces security constraints based on user roles and
 * ensures that employees can only access their own schedules where applicable.
 *
 * <p>
 * The controller delegates business logic to {@link ScheduleService} and
 * {@link EmployeeService}, and uses {@link Mapper} components to transform
 * between domain models and API representations.
 *
 * <p>
 * Typical usage involves interacting with the exposed HTTP endpoints under
 * {@code /api/v1/schedules}.
 */
@RestController
public class ScheduleController implements ScheduleResource {

	private static final Logger logger = LoggerFactory.getLogger(ScheduleController.class);

	private final ScheduleService scheduleService;
	private final ScheduleAuthorizationAdapter authorization;
	private final Mapper<Schedule, ScheduleResponse> scheduleResponseMapper;
	private final Mapper<ScheduleRuleRequest, ScheduleRuleDefinition> scheduleRuleDefinitionMapper;

	/**
	 * Constructs a new {@code ScheduleController} with required dependencies.
	 *
	 * @param scheduleService              service for schedule operations; can't be
	 *                                     {@code null}
	 * @param scheduleResponseMapper       mapper for schedule responses; can't be
	 *                                     {@code null}
	 * @param scheduleRuleDefinitionMapper mapper for schedule rule definitions;
	 *                                     can't be {@code null}
	 */
	public ScheduleController(final ScheduleService scheduleService, final ScheduleAuthorizationAdapter authorization,
			final @Qualifier("scheduleResponseMapper") Mapper<Schedule, ScheduleResponse> scheduleResponseMapper,
			final @Qualifier("scheduleRuleDefinitionMapper") Mapper<ScheduleRuleRequest, ScheduleRuleDefinition> scheduleRuleDefinitionMapper) {
		this.scheduleService = Objects.requireNonNull(scheduleService, "scheduleService can't be null");
		this.authorization = Objects.requireNonNull(authorization, "authorization can't be null");
		this.scheduleResponseMapper = Objects.requireNonNull(scheduleResponseMapper,
				"scheduleResponseMapper can't be null");
		this.scheduleRuleDefinitionMapper = Objects.requireNonNull(scheduleRuleDefinitionMapper,
				"scheduleRuleDefinitionMapper can't be null");
	}

	/**
	 * Searches schedules based on optional filtering criteria.
	 *
	 * <p>
	 * Employees with only the {@code JANUS_EMPLOYEE} role are restricted to
	 * searching their own schedules and must provide their own email address.
	 *
	 * @param query          optional search query; may be {@code null}
	 * @param employeeEmail  optional employee email filter; may be {@code null}
	 * @param pageable       pagination information; can't be {@code null}
	 * @param authentication current authentication context; can't be {@code null}
	 * @return a {@link ResponseEntity} containing a paginated list of matching
	 *         schedules
	 * @throws AccessDeniedException if the authenticated user is not allowed to
	 *                               perform the search
	 */
	@Override
	public ResponseEntity<Page<ScheduleResponse>> searchSchedules(
			final String query,
			final String employeeEmail,
			final Pageable pageable, final Authentication authentication) {
		logger.debug("Search schedules ACTION performed");
		final String effectiveEmployeeEmail = this.authorization.effectiveEmployeeEmail(authentication, EmailAddresses.canonicalize(employeeEmail));

		final Page<ScheduleResponse> schedules = this.scheduleService
				.searchSchedules(query, effectiveEmployeeEmail, pageable).map(this.scheduleResponseMapper::map);
		return ResponseEntity.ok(schedules);
	}

	/**
	 * Retrieves a specific schedule by its unique code.
	 *
	 * <p>
	 * Employees with only the {@code JANUS_EMPLOYEE} role can only access schedules
	 * they are assigned to.
	 *
	 * @param scheduleCode   the unique code of the schedule; must not be
	 *                       {@code null}
	 * @param authentication current authentication context; can't be {@code null}
	 * @return a {@link ResponseEntity} containing the requested schedule
	 * @throws AccessDeniedException if the authenticated user is not allowed to
	 *                               access the schedule
	 */
	@Override
	public ResponseEntity<ScheduleResponse> findSchedule(
			final String scheduleCode,
			final Authentication authentication) {
		logger.debug("Find schedule ACTION performed");

		final Schedule schedule = this.scheduleService.findScheduleByCode(scheduleCode);
		final ScheduleResponse response = this.scheduleResponseMapper.map(schedule);
		return ResponseEntity.ok(response);
	}

	/**
	 * Creates a new schedule.
	 *
	 * @param request the payload describing the schedule to create; can't be
	 *                {@code null}
	 * @return a {@link ResponseEntity} containing the created schedule with HTTP
	 *         status {@code 201 CREATED}
	 */
	@Override
	public ResponseEntity<ScheduleResponse> createSchedule(final CreateScheduleRequest request) {
		logger.debug("Create schedule ACTION performed");
		final String code = request.code().trim();
		final String name = request.name().trim();
		final Duration entryTolerance = request.entryTolerance();
		final Duration exitTolerance = request.exitTolerance();
		final List<ScheduleRuleDefinition> rules = this.map(request.rules());
		final Schedule createdSchedule = this.scheduleService.createSchedule(code, name, entryTolerance, exitTolerance,
				rules);
		final ScheduleResponse response = this.scheduleResponseMapper.map(createdSchedule);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	/**
	 * Maps a list of {@link ScheduleRuleRequest} objects into a list of
	 * {@link ScheduleRuleDefinition} domain objects.
	 *
	 * @param rules the list of rule requests to map; can't be {@code null}
	 * @return a list of mapped {@link ScheduleRuleDefinition} instances
	 */
	public List<ScheduleRuleDefinition> map(final List<ScheduleRuleRequest> rules) {
		return rules.stream().map(this.scheduleRuleDefinitionMapper::map).toList();
	}

	/**
	 * Updates an existing schedule identified by its code.
	 *
	 * @param scheduleCode the code of the schedule to update; can't be {@code null}
	 * @param request      the payload describing the new schedule data; can't be
	 *                     {@code null}
	 * @return a {@link ResponseEntity} containing the updated schedule
	 */
	@Override
	public ResponseEntity<ScheduleResponse> updateSchedule(
			final String scheduleCode,
			final UpdateScheduleRequest request) {
		logger.debug("Update schedule ACTION performed");

		final Schedule updatedSchedule = this.scheduleService.updateSchedule(scheduleCode, request.name(),
				request.entryTolerance(), request.exitTolerance(), this.map(request.rules()));
		final ScheduleResponse response = this.scheduleResponseMapper.map(updatedSchedule);
		return ResponseEntity.ok(response);
	}

	/**
	 * Deletes the schedule identified by the given code.
	 *
	 * @param scheduleCode the unique code of the schedule; must not be {@code null}
	 * @return a {@link ResponseEntity} with an empty body and HTTP status
	 *         {@code 204 NO CONTENT}
	 */
	@Override
	public ResponseEntity<Void> deleteSchedule(
			final String scheduleCode) {
		logger.debug("Delete schedule ACTION performed");

		final Schedule schedule = this.scheduleService.findScheduleByCode(scheduleCode);
		this.scheduleService.deleteSchedule(schedule);
		return ResponseEntity.noContent().build();
	}
}
