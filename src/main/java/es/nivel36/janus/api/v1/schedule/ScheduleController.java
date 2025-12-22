/*
 * Copyright 2025 Abel Ferrer Jiménez
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.nivel36.janus.api.Mapper;
import es.nivel36.janus.service.schedule.Schedule;
import es.nivel36.janus.service.schedule.ScheduleRuleDefinition;
import es.nivel36.janus.service.schedule.ScheduleService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;

/**
 * REST controller exposing CRUD operations for {@link Schedule} aggregates.
 */
@RestController
@RequestMapping("/api/v1/schedules")
public class ScheduleController {

	private static final Logger logger = LoggerFactory.getLogger(ScheduleController.class);

	private final ScheduleService scheduleService;
	private final Mapper<Schedule, ScheduleResponse> scheduleResponseMapper;
	private final Mapper<ScheduleRuleRequest, ScheduleRuleDefinition> scheduleRuleDefinitionMapper;

	public ScheduleController(final ScheduleService scheduleService,
			final Mapper<Schedule, ScheduleResponse> scheduleResponseMapper,
			final Mapper<ScheduleRuleRequest, ScheduleRuleDefinition> scheduleRuleDefinitionMapper) {
		this.scheduleService = Objects.requireNonNull(scheduleService, "scheduleService can't be null");
		this.scheduleResponseMapper = Objects.requireNonNull(scheduleResponseMapper,
				"scheduleResponseMapper can't be null");
		this.scheduleRuleDefinitionMapper = Objects.requireNonNull(scheduleRuleDefinitionMapper,
				"scheduleRuleDefinitionMapper can't be null");
	}

	/**
	 * Retrieves all schedules registered in the system.
	 *
	 * @return a {@link ResponseEntity} containing the list of schedules
	 */
	@GetMapping
	public ResponseEntity<List<ScheduleResponse>> getSchedules() {
		logger.debug("List schedules ACTION performed");

		final List<Schedule> schedules = this.scheduleService.findAllSchedules();
		final List<ScheduleResponse> responses = schedules.stream().map(scheduleResponseMapper::map).toList();
		return ResponseEntity.ok(responses);
	}

	/**
	 * Retrieves a specific schedule by its unique code.
	 *
	 * @param scheduleCode the unique code of the schedule; must not be {@code null}
	 * @return a {@link ResponseEntity} containing the requested schedule
	 */
	@GetMapping("/{scheduleCode}")
	public ResponseEntity<ScheduleResponse> findSchedule(final @PathVariable("scheduleCode") //
	@Pattern( //
			regexp = "[A-Za-z0-9_-]{1,50}", //
			message = "code must contain only letters, digits, underscores or hyphens (max 50)" //
	) //
	String scheduleCode) {
		logger.debug("Find schedule ACTION performed");

		final Schedule schedule = this.scheduleService.findScheduleByCode(scheduleCode);
		final ScheduleResponse response = this.scheduleResponseMapper.map(schedule);
		return ResponseEntity.ok(response);
	}

	/**
	 * Creates a new schedule.
	 *
	 * @param request the payload describing the schedule to create; must not be
	 *                {@code null}
	 * @return a {@link ResponseEntity} containing the created schedule
	 */
	@PostMapping
	public ResponseEntity<ScheduleResponse> createSchedule(@Valid @RequestBody final CreateScheduleRequest request) {
		logger.debug("Create schedule ACTION performed");
		final Schedule createdSchedule = this.scheduleService.createSchedule(request.code(), request.name(),
				this.map(request.rules()));
		final ScheduleResponse response = this.scheduleResponseMapper.map(createdSchedule);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	public List<ScheduleRuleDefinition> map(List<ScheduleRuleRequest> rules) {
		return rules.stream().map(scheduleRuleDefinitionMapper::map).toList();
	}

	/**
	 * Updates an existing schedule identified by its code.
	 *
	 * @param scheduleCode the code of the schedule to update; must not be
	 *                     {@code null}
	 * @param request      the payload describing the new schedule data; must not be
	 *                     {@code null}
	 * @return a {@link ResponseEntity} containing the updated schedule
	 */
	@PutMapping("/{scheduleCode}")
	public ResponseEntity<ScheduleResponse> updateSchedule(final @PathVariable("scheduleCode") //
	@Pattern( //
			regexp = "[A-Za-z0-9_-]{1,50}", //
			message = "code must contain only letters, digits, underscores or hyphens (max 50)" //
	) //
	String scheduleCode, @Valid @RequestBody final UpdateScheduleRequest request) {
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
	 * @return a {@link ResponseEntity} with an empty body and HTTP 204 status
	 */
	@DeleteMapping("/{scheduleCode}")
	public ResponseEntity<Void> deleteSchedule(final @PathVariable("scheduleCode") //
	@Pattern( //
			regexp = "[A-Za-z0-9_-]{1,50}", //
			message = "code must contain only letters, digits, underscores or hyphens (max 50)" //
	) //
	String scheduleCode) {
		logger.debug("Delete schedule ACTION performed");

		final Schedule schedule = this.scheduleService.findScheduleByCode(scheduleCode);
		this.scheduleService.deleteSchedule(schedule);
		return ResponseEntity.noContent().build();
	}
}
