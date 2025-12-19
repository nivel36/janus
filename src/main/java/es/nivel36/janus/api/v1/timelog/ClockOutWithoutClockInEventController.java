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
package es.nivel36.janus.api.v1.timelog;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import es.nivel36.janus.api.Mapper;
import es.nivel36.janus.service.employee.Employee;
import es.nivel36.janus.service.employee.EmployeeService;
import es.nivel36.janus.service.timelog.ClockOutWithoutClockInEvent;
import es.nivel36.janus.service.timelog.ClockOutWithoutClockInEventService;
import es.nivel36.janus.service.worksite.Worksite;
import es.nivel36.janus.service.worksite.WorksiteService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;

/**
 * REST controller responsible for exposing operations related to
 * {@link ClockOutWithoutClockInEvent} resolution and invalidation.
 */
@RestController
@RequestMapping("/api/v1/employees/{employeeEmail}/clock-out-without-clock-in-events")
public class ClockOutWithoutClockInEventController {

	private static final Logger logger = LoggerFactory.getLogger(ClockOutWithoutClockInEventController.class);

	private final ClockOutWithoutClockInEventService clockOutWithoutClockInEventService;
	private final EmployeeService employeeService;
	private final WorksiteService worksiteService;
	private final Mapper<ClockOutWithoutClockInEvent, ClockOutWithoutClockInEventResponse> clockOutWithoutClockInEventResponseMapper;

	/**
	 * Builds a controller for managing {@link ClockOutWithoutClockInEvent}
	 * resources.
	 *
	 * @param clockOutWithoutClockInEventService  application service handling event
	 *                                            resolution and invalidation; must
	 *                                            not be {@code null}
	 * @param employeeService                     service used to resolve
	 *                                            {@link Employee} entities; must
	 *                                            not be {@code null}
	 * @param worksiteService                     service used to resolve
	 *                                            {@link Worksite} entities; must
	 *                                            not be {@code null}
	 * @param clockOutWithoutClockInEventResponse mapper converting
	 *                                            {@link ClockOutWithoutClockInEvent}
	 *                                            domain objects to
	 *                                            {@link ClockOutWithoutClockInEventResponse}
	 *                                            DTOs; must not be {@code null}
	 */
	public ClockOutWithoutClockInEventController(
			final ClockOutWithoutClockInEventService clockOutWithoutClockInEventService,
			final EmployeeService employeeService, final WorksiteService worksiteService,
			final Mapper<ClockOutWithoutClockInEvent, ClockOutWithoutClockInEventResponse> clockOutWithoutClockInEventResponseMapper) {
		this.clockOutWithoutClockInEventService = Objects.requireNonNull(clockOutWithoutClockInEventService,
				"clockOutWithoutClockInEventService can't be null");
		this.employeeService = Objects.requireNonNull(employeeService, "employeeService can't be null");
		this.worksiteService = Objects.requireNonNull(worksiteService, "worksiteService can't be null");
		this.clockOutWithoutClockInEventResponseMapper = Objects.requireNonNull(clockOutWithoutClockInEventResponseMapper,
				"clockOutWithoutClockInEventResponseMapper can't be null");
	}

	/**
	 * Resolves a {@link ClockOutWithoutClockInEvent} by creating a corresponding
	 * {@link es.nivel36.janus.service.timelog.TimeLog}.
	 *
	 * @param employeeEmail the email of the employee; must not be {@code null}
	 * @param worksiteCode  the code of the worksite where the event was recorded;
	 *                      must not be {@code null}
	 * @param exitTime      the exit time of the event to resolve; must not be
	 *                      {@code null}
	 * @param request       payload containing the entry time used to resolve the
	 *                      event and an optional reason; must not be {@code null}
	 * @return the resolved {@link ClockOutWithoutClockInEventResponse}
	 */
	@PostMapping("/{exitTime}/resolve")
	public ResponseEntity<ClockOutWithoutClockInEventResponse> resolveClockOutWithoutClockInEvent( //
			final @PathVariable("employeeEmail") //
			@Pattern( //
					regexp = "^(?=.{1,254}$)[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", //
					message = "must be a valid and safe email address (max 254)" //
			) //
			String employeeEmail, //
			final @RequestParam("worksiteCode") //
			@Pattern( //
					regexp = "[A-Za-z0-9_-]{1,50}", //
					message = "code must contain only letters, digits, underscores or hyphens (max 50)") //
			String worksiteCode, //
			final @PathVariable("exitTime") Instant exitTime, //
			final @Valid @RequestBody ResolveClockOutWithoutClockInEventRequest request) {
		logger.debug("Resolve clock-out-without-clock-in event ACTION performed");

		final Employee employee = this.employeeService.findEmployeeByEmail(employeeEmail);
		final Worksite worksite = this.worksiteService.findWorksiteByCode(worksiteCode);
		final ClockOutWithoutClockInEvent clockOutWithoutClockInEvent = findClockOutWithoutClockInEvent(employee,
				worksite, exitTime);

		final ClockOutWithoutClockInEvent resolvedClockOutWithoutClockInEvent = this.clockOutWithoutClockInEventService
				.resolve(clockOutWithoutClockInEvent, request.entryTime(), toOptionalReason(request.reason()));
		final ClockOutWithoutClockInEventResponse response = this.clockOutWithoutClockInEventResponseMapper
				.map(resolvedClockOutWithoutClockInEvent);
		return ResponseEntity.ok(response);
	}

	/**
	 * Invalidates a {@link ClockOutWithoutClockInEvent}.
	 *
	 * @param employeeEmail the email of the employee; must not be {@code null}
	 * @param worksiteCode  the code of the worksite where the event was recorded;
	 *                      must not be {@code null}
	 * @param exitTime      the exit time of the event to invalidate; must not be
	 *                      {@code null}
	 * @param request       optional reason explaining why the event is being
	 *                      invalidated; may be {@code null}
	 * @return the invalidated {@link ClockOutWithoutClockInEventResponse}
	 */
	@PostMapping("/{exitTime}/invalidate")
	public ResponseEntity<ClockOutWithoutClockInEventResponse> invalidateClockOutWithoutClockInEvent( //
			final @PathVariable("employeeEmail") //
			@Pattern( //
					regexp = "^(?=.{1,254}$)[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", //
					message = "must be a valid and safe email address (max 254)" //
			) //
			String employeeEmail, //
			final @RequestParam("worksiteCode") //
			@Pattern( //
					regexp = "[A-Za-z0-9_-]{1,50}", //
					message = "code must contain only letters, digits, underscores or hyphens (max 50)") //
			String worksiteCode, //
			final @PathVariable("exitTime") Instant exitTime, //
			final @RequestBody(required = false) InvalidateClockOutWithoutClockInEventRequest request) {
		logger.debug("Invalidate clock-out-without-clock-in event ACTION performed");

		final Employee employee = this.employeeService.findEmployeeByEmail(employeeEmail);
		final Worksite worksite = this.worksiteService.findWorksiteByCode(worksiteCode);
		final ClockOutWithoutClockInEvent clockOutWithoutClockInEvent = findClockOutWithoutClockInEvent(employee,
				worksite, exitTime);

		final Optional<String> reason = request == null ? Optional.empty() : toOptionalReason(request.reason());
		final ClockOutWithoutClockInEvent invalidatedClockOutWithoutClockInEvent = this.clockOutWithoutClockInEventService
				.invalidate(clockOutWithoutClockInEvent, reason);
		final ClockOutWithoutClockInEventResponse response = this.clockOutWithoutClockInEventResponseMapper
				.map(invalidatedClockOutWithoutClockInEvent);
		return ResponseEntity.ok(response);
	}

	/**
	 * Retrieves a {@link ClockOutWithoutClockInEvent} by employee, worksite, and
	 * exit time.
	 *
	 * @param employeeEmail the email of the employee; must not be {@code null}
	 * @param worksiteCode  the code of the worksite where the event was recorded;
	 *                      must not be {@code null}
	 * @param exitTime      the exit time of the event; must not be {@code null}
	 * @return the requested {@link ClockOutWithoutClockInEventResponse}
	 */
	@GetMapping("/{exitTime}")
	public ResponseEntity<ClockOutWithoutClockInEventResponse> findClockOutWithoutClockInEvent( //
			final @PathVariable("employeeEmail") //
			@Pattern( //
					regexp = "^(?=.{1,254}$)[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", //
					message = "must be a valid and safe email address (max 254)" //
			) //
			String employeeEmail, //
			final @RequestParam("worksiteCode") //
			@Pattern( //
					regexp = "[A-Za-z0-9_-]{1,50}", //
					message = "code must contain only letters, digits, underscores or hyphens (max 50)") //
			String worksiteCode, //
			final @PathVariable("exitTime") Instant exitTime) {
		logger.debug("Find clock-out-without-clock-in event ACTION performed");

		final Employee employee = this.employeeService.findEmployeeByEmail(employeeEmail);
		final Worksite worksite = this.worksiteService.findWorksiteByCode(worksiteCode);
		final ClockOutWithoutClockInEvent clockOutWithoutClockInEvent = findClockOutWithoutClockInEvent(employee,
				worksite, exitTime);
		final ClockOutWithoutClockInEventResponse response = this.clockOutWithoutClockInEventResponseMapper
				.map(clockOutWithoutClockInEvent);
		return ResponseEntity.ok(response);
	}

	private ClockOutWithoutClockInEvent findClockOutWithoutClockInEvent(final Employee employee, final Worksite worksite,
			final Instant exitTime) {
		return Objects.requireNonNull(this.clockOutWithoutClockInEventService
				.findClockOutWithoutClockInEventByEmployeeAndWorksiteAndExitTime(employee, worksite, exitTime).getBody(),
				"clockOutWithoutClockInEvent can't be null");
	}

	private Optional<String> toOptionalReason(final String reason) {
		return Optional.ofNullable(reason).filter(str -> !str.isBlank());
	}
}
