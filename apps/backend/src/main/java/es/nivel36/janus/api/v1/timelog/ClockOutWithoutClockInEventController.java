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
package es.nivel36.janus.api.v1.timelog;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.RestController;

import es.nivel36.janus.api.Mapper;
import es.nivel36.janus.service.applicationsettings.ApplicationSettingsService;
import es.nivel36.janus.service.employee.Employee;
import es.nivel36.janus.service.employee.EmployeeService;
import es.nivel36.janus.service.timelog.ClockOutWithoutClockInEvent;
import es.nivel36.janus.service.timelog.ClockOutWithoutClockInEventService;
import es.nivel36.janus.service.worksite.Worksite;
import es.nivel36.janus.service.worksite.WorksiteService;

/**
 * REST controller responsible for exposing operations related to
 * {@link ClockOutWithoutClockInEvent} resolution and invalidation.
 */
@RestController
public class ClockOutWithoutClockInEventController implements ClockOutWithoutClockInEventResource {

	private static final Logger logger = LoggerFactory.getLogger(ClockOutWithoutClockInEventController.class);

	private final ClockOutWithoutClockInEventService clockOutWithoutClockInEventService;
	private final EmployeeService employeeService;
	private final ApplicationSettingsService applicationSettingsService;
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
	 * @param applicationSettingsService          service used to read global
	 *                                            application settings; must not be
	 *                                            {@code null}
	 * @param worksiteService                     service used to resolve
	 *                                            {@link Worksite} entities; must
	 *                                            not be {@code null}
	 * @param clockOutWithoutClockInEventResponse mapper converting
	 *                                            {@link ClockOutWithoutClockInEvent}
	 *                                            domain objects to
	 *                                            {@link ClockOutWithoutClockInEventResponse}
	 *                                            DTOs; must not be {@code null}
	 */
	public ClockOutWithoutClockInEventController( //
			final ClockOutWithoutClockInEventService clockOutWithoutClockInEventService, //
			final EmployeeService employeeService, //
			final ApplicationSettingsService applicationSettingsService, //
			final WorksiteService worksiteService, //
			final @Qualifier("clockOutWithoutClockInEventResponseMapper") Mapper<ClockOutWithoutClockInEvent, ClockOutWithoutClockInEventResponse> clockOutWithoutClockInEventResponseMapper) {
		this.clockOutWithoutClockInEventService = Objects.requireNonNull( //
				clockOutWithoutClockInEventService, //
				"clockOutWithoutClockInEventService can't be null"); //
		this.employeeService = Objects.requireNonNull( //
				employeeService, //
				"employeeService can't be null"); //
		this.applicationSettingsService = Objects.requireNonNull( //
				applicationSettingsService, //
				"applicationSettingsService can't be null"); //
		this.worksiteService = Objects.requireNonNull( //
				worksiteService, //
				"worksiteService can't be null"); //
		this.clockOutWithoutClockInEventResponseMapper = Objects.requireNonNull( //
				clockOutWithoutClockInEventResponseMapper, //
				"clockOutWithoutClockInEventResponseMapper can't be null");
	}

	@Override
	public ResponseEntity<ClockOutWithoutClockInEventResponse> transitionClockOutWithoutClockInEvent(
			final String employeeNumber, final String worksiteCode, final Instant exitTime,
			final TransitionClockOutWithoutClockInEventRequest request) {
		logger.debug("Transition clock-out-without-clock-in event ACTION performed: {}", request.action());
		final Employee employee = this.employeeService.findEmployeeByEmployeeNumber(employeeNumber);
		final Worksite worksite = this.worksiteService.findWorksiteByCode(worksiteCode.trim());
		final ClockOutWithoutClockInEvent event = this.clockOutWithoutClockInEventService
				.findClockOutWithoutClockInEventByEmployeeAndWorksiteAndExitTime(employee, worksite, exitTime);
		final Optional<String> reason = this.toOptionalReason(request.reason());
		final ClockOutWithoutClockInEvent transitioned;
		if (request.action() == ClockOutWithoutClockInEventAction.RESOLVE) {
			this.assertManualTimeEntryAllowed();
			transitioned = this.clockOutWithoutClockInEventService.resolve(event, request.entryTime(), reason);
		} else {
			transitioned = this.clockOutWithoutClockInEventService.invalidate(event, reason);
		}
		return ResponseEntity.ok(this.clockOutWithoutClockInEventResponseMapper.map(transitioned));
	}

	/**
	 * Retrieves a {@link ClockOutWithoutClockInEvent} by employee, worksite, and
	 * exit time.
	 *
	 * @param employeeNumber the number of the employee; must not be {@code null}
	 * @param worksiteCode   the code of the worksite where the event was recorded;
	 *                       must not be {@code null}
	 * @param exitTime       the exit time of the event; must not be {@code null}
	 * @return the requested {@link ClockOutWithoutClockInEventResponse}
	 */
	@Override
	public ResponseEntity<ClockOutWithoutClockInEventResponse> findClockOutWithoutClockInEvent( //
			final String employeeNumber, //
			final String worksiteCode, //
			final Instant exitTime) {
		logger.debug("Find clock-out-without-clock-in event ACTION performed");

		final Employee employee = this.employeeService.findEmployeeByEmployeeNumber(employeeNumber);
		final Worksite worksite = this.worksiteService.findWorksiteByCode(worksiteCode.trim());
		final ClockOutWithoutClockInEvent clockOutWithoutClockInEvent = this.clockOutWithoutClockInEventService
				.findClockOutWithoutClockInEventByEmployeeAndWorksiteAndExitTime(employee, worksite, exitTime);
		final ClockOutWithoutClockInEventResponse response = this.clockOutWithoutClockInEventResponseMapper
				.map(clockOutWithoutClockInEvent);
		return ResponseEntity.ok(response);
	}

	private void assertManualTimeEntryAllowed() {
		if (!this.applicationSettingsService.findApplicationSettings().isEmployeeManualTimelogEntryAllowed()) {
			throw new AccessDeniedException("Manual timelog entry is disabled for employees");
		}
	}

	private Optional<String> toOptionalReason(final String reason) {
		return Optional.ofNullable(reason).map(String::trim).filter(str -> !str.isBlank());
	}
}
