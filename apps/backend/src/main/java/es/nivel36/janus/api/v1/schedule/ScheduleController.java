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

import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import es.nivel36.janus.api.Mapper;
import es.nivel36.janus.service.employee.EmployeeService;
import es.nivel36.janus.service.schedule.Schedule;
import es.nivel36.janus.service.schedule.ScheduleRuleDefinition;
import es.nivel36.janus.service.schedule.ScheduleService;
import es.nivel36.janus.util.Roles;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;

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
@RequestMapping("/api/v1/schedules")
public class ScheduleController {

	private static final Logger logger = LoggerFactory.getLogger(ScheduleController.class);

	private final ScheduleService scheduleService;
	private final EmployeeService employeeService;
	private final Mapper<Schedule, ScheduleResponse> scheduleResponseMapper;
	private final Mapper<ScheduleRuleRequest, ScheduleRuleDefinition> scheduleRuleDefinitionMapper;

	/**
	 * Constructs a new {@code ScheduleController} with required dependencies.
	 *
	 * @param scheduleService              service for schedule operations; can't be
	 *                                     {@code null}
	 * @param employeeService              service for employee operations; can't be
	 *                                     {@code null}
	 * @param scheduleResponseMapper       mapper for schedule responses; can't be
	 *                                     {@code null}
	 * @param scheduleRuleDefinitionMapper mapper for schedule rule definitions;
	 *                                     can't be {@code null}
	 */
	public ScheduleController(final ScheduleService scheduleService, final EmployeeService employeeService,
			final Mapper<Schedule, ScheduleResponse> scheduleResponseMapper,
			final Mapper<ScheduleRuleRequest, ScheduleRuleDefinition> scheduleRuleDefinitionMapper) {
		this.scheduleService = Objects.requireNonNull(scheduleService, "scheduleService can't be null");
		this.employeeService = Objects.requireNonNull(employeeService, "employeeService can't be null");
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
	@GetMapping
	@PreAuthorize("hasAnyRole('JANUS_EMPLOYEE', 'JANUS_USER', 'JANUS_ADMIN')")
	public ResponseEntity<Page<ScheduleResponse>> searchSchedules(
			final @RequestParam(required = false) @Pattern(regexp = "[A-Za-z0-9_-]{1,50}", message = "query must contain only letters, digits, underscores or hyphens (max 50)") String query,
			final @RequestParam(required = false) @Pattern(regexp = "^(?=.{1,254}$)[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "employeeEmail must be a valid and safe email address (max 254)") String employeeEmail,
			final Pageable pageable, final Authentication authentication) {
		logger.debug("Search schedules ACTION performed");
		final String authenticatedEmail = authentication.getName();
		final boolean hasOnlyEmployeeRole = Roles.hasOnlyEmployeeRole(authentication.getAuthorities());
		final String effectiveEmployeeEmail;
		if (hasOnlyEmployeeRole) {
			if (authenticatedEmail == null || authenticatedEmail.isBlank()) {
				throw new AccessDeniedException("Employee email claim is required");
			}
			if (employeeEmail == null) {
				throw new AccessDeniedException("Employees can only search schedules for themselves");
			}
			if (!employeeEmail.equalsIgnoreCase(authenticatedEmail)) {
				throw new AccessDeniedException("Employees can only search schedules for themselves");
			}
			effectiveEmployeeEmail = authenticatedEmail;
		} else {
			effectiveEmployeeEmail = employeeEmail;
		}

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
	@PreAuthorize("hasAnyRole('JANUS_EMPLOYEE','JANUS_USER', 'JANUS_ADMIN')")
	@GetMapping("/{scheduleCode}")
	public ResponseEntity<ScheduleResponse> findSchedule(
			final @PathVariable("scheduleCode") @Pattern(regexp = "[A-Za-z0-9_-]{1,50}", message = "code must contain only letters, digits, underscores or hyphens (max 50)") String scheduleCode,
			final Authentication authentication) {
		logger.debug("Find schedule ACTION performed");

		final String authenticatedEmail = authentication.getName();
		final boolean hasOnlyEmployeeRole = Roles.hasOnlyEmployeeRole(authentication.getAuthorities());
		if (hasOnlyEmployeeRole && !this.employeeService.isAssignedToSchedule(authenticatedEmail, scheduleCode)) {
			throw new AccessDeniedException("Employees can only search his own schedule");
		}

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
	@PreAuthorize("hasAnyRole('JANUS_USER', 'JANUS_ADMIN')")
	@PostMapping
	public ResponseEntity<ScheduleResponse> createSchedule(@Valid @RequestBody final CreateScheduleRequest request) {
		logger.debug("Create schedule ACTION performed");
		final Schedule createdSchedule = this.scheduleService.createSchedule(request.code(), request.name(),
				this.map(request.rules()));
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
	@PreAuthorize("hasAnyRole('JANUS_USER', 'JANUS_ADMIN')")
	@PutMapping("/{scheduleCode}")
	public ResponseEntity<ScheduleResponse> updateSchedule(
			final @PathVariable("scheduleCode") @Pattern(regexp = "[A-Za-z0-9_-]{1,50}", message = "code must contain only letters, digits, underscores or hyphens (max 50)") String scheduleCode,
			@Valid @RequestBody final UpdateScheduleRequest request) {
		logger.debug("Update schedule ACTION performed");

		final Schedule updatedSchedule = this.scheduleService.updateSchedule(scheduleCode, request.name(),
				this.map(request.rules()));
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
	@PreAuthorize("hasAnyRole('JANUS_USER', 'JANUS_ADMIN')")
	@DeleteMapping("/{scheduleCode}")
	public ResponseEntity<Void> deleteSchedule(
			final @PathVariable("scheduleCode") @Pattern(regexp = "[A-Za-z0-9_-]{1,50}", message = "code must contain only letters, digits, underscores or hyphens (max 50)") String scheduleCode) {
		logger.debug("Delete schedule ACTION performed");

		final Schedule schedule = this.scheduleService.findScheduleByCode(scheduleCode);
		this.scheduleService.deleteSchedule(schedule);
		return ResponseEntity.noContent().build();
	}
}
