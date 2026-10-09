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
package es.nivel36.janus.api.v1.timelog;

import es.nivel36.janus.service.timelog.EventAlreadyFinalizedException;

import es.nivel36.janus.service.ResourceNotFoundException;

import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import es.nivel36.janus.validation.EmployeeNumber;
import es.nivel36.janus.validation.WorksiteCode;
import jakarta.validation.Valid;

/**
 * HTTP contract for viewing and finalizing clock-out-without-clock-in events.
 * <p>
 * An event is selected by employee number, worksite code and exact exit
 * instant. Access requires an authenticated identity with an existing local
 * profile. Administrators and users may act on any employee's events; employees
 * may act only on events belonging to their linked employee.
 */
@RequestMapping("/api/v1")
public interface ClockOutWithoutClockInEventResource {

	/**
	 * Resolves or invalidates the selected event according to the requested action.
	 * <p>
	 * Resolution requires manual time entry to be enabled and creates a closed time
	 * log using the requested entry and the event's exit. Invalidation creates no
	 * log. Both actions finalize the event and cannot be repeated. The reason is
	 * trimmed, and a {@code null} or blank reason is treated as absent.
	 *
	 * @param  employeeNumber                 the exact employee number
	 * @param  worksiteCode                   the exact worksite code
	 * @param  exitTime                       the exact recorded exit instant
	 * @param  request                        the validated final transition and
	 *                                        optional explanation
	 * @return                                an HTTP {@code 200 OK} response
	 *                                        containing the finalized event
	 * @throws ResourceNotFoundException      if the worksite or event is absent
	 * @throws EventAlreadyFinalizedException if the event is already final
	 * @throws AccessDeniedException          if access is denied or manual entry is
	 *                                        disabled for resolution
	 * @see                                   es.nivel36.janus.service.timelog.ClockOutWithoutClockInEventService#resolve(
	 *                                        es.nivel36.janus.service.timelog.ClockOutWithoutClockInEvent,
	 *                                        Instant, java.util.Optional)
	 */
	@PatchMapping("/employees/{employeeNumber}/worksites/{worksiteCode}/clock-out-without-clock-in-events/{exitTime}")
	@PreAuthorize("@clockOutWithoutClockInEventAuthorization.canTransition(authentication, #employeeNumber, #request.action())")
	ResponseEntity<ClockOutWithoutClockInEventResponse> transitionClockOutWithoutClockInEvent(
			@PathVariable("employeeNumber")
			@EmployeeNumber
			String employeeNumber,
			@PathVariable("worksiteCode")
			@WorksiteCode
			String worksiteCode,
			@PathVariable("exitTime")
			Instant exitTime,
			@RequestBody
			@Valid
			TransitionClockOutWithoutClockInEventRequest request);

	/**
	 * Returns the event matching the employee, worksite and exact exit instant.
	 *
	 * @param  employeeNumber            the exact employee number
	 * @param  worksiteCode              the exact worksite code
	 * @param  exitTime                  the exact recorded exit instant
	 * @return                           an HTTP {@code 200 OK} response containing
	 *                                   the event
	 * @throws ResourceNotFoundException if the worksite or event is absent
	 * @throws AccessDeniedException     if the caller cannot view the employee's
	 *                                   events
	 */
	@GetMapping("/employees/{employeeNumber}/worksites/{worksiteCode}/clock-out-without-clock-in-events/{exitTime}")
	@PreAuthorize("@clockOutWithoutClockInEventAuthorization.canView(authentication, #employeeNumber)")
	ResponseEntity<ClockOutWithoutClockInEventResponse> findClockOutWithoutClockInEvent(
			@PathVariable("employeeNumber")
			@EmployeeNumber
			String employeeNumber,
			@PathVariable("worksiteCode")
			@WorksiteCode
			String worksiteCode,
			@PathVariable("exitTime")
			Instant exitTime);

}
