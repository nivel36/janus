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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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

import es.nivel36.janus.service.ResourceAlreadyExistsException;
import es.nivel36.janus.service.ResourceNotFoundException;
import es.nivel36.janus.validation.EmployeeNumber;
import es.nivel36.janus.validation.ScheduleCode;
import es.nivel36.janus.validation.SearchQuery;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;

/**
 * HTTP contract for schedules at {@code /api/v1/schedules}. Requests require a
 * validated bearer JWT and a provisioned actor. Authorization precedes
 * execution. JANUS_ADMIN and JANUS_USER have full access; callers with only
 * JANUS_EMPLOYEE may search and view their own assigned schedule.
 */
@RequestMapping("/api/v1/schedules")
public interface ScheduleResource {

	/**
	 * Returns a page of schedule summaries without rules within the caller's
	 * authorized employee scope.
	 * <p>
	 * Query is optional single-line text with 1-100 characters, used literally
	 * without trimming against code and name, partially and case-insensitively.
	 * Employee number must match {@code [A-Za-z0-9_-]{1,50}} without trimming.
	 * Filters combine with AND. Absent filters include schedules without rules,
	 * ranges or employees. For employee-only callers, the server derives the
	 * employee number from the actor; a different requested employee is denied.
	 * </p>
	 * <p>
	 * Returns HTTP 200 without changing schedules. Public sort fields are
	 * {@code code} and {@code name}. Default ordering is code ascending, with code
	 * as a unique tie-breaker unless explicitly ordered. Page size is capped at
	 * {@code spring.data.rest.max-page-size} (100 by default).
	 * </p>
	 *
	 * @param  query                        optional literal fragment; null disables
	 *                                      text filtering
	 * @param  employeeNumber               optional exact employee filter; null
	 *                                      omits the filter for elevated callers
	 * @param  pageable                     requested page and ordering; HTTP
	 *                                      defaults are page 0, size 20
	 * @param  authentication               current validated authentication
	 * @return                              HTTP 200 containing schedule summaries
	 *                                      and page metadata, possibly empty
	 * @throws ConstraintViolationException if a filter violates its constraint
	 * @throws IllegalArgumentException     if paging or sorting is unsupported
	 * @throws AccessDeniedException        if the actor is unprovisioned or the
	 *                                      scope is denied
	 */
	@GetMapping
	@ApiResponse(responseCode = "200", description = "Search results within the authorized employee scope")
	@ApiResponse(responseCode = "403", description = "Employee association missing or employee filter outside the authorized scope")
	@PreAuthorize("@scheduleAuthorization.canSearch(authentication, #employeeNumber)")
	ResponseEntity<Page<ScheduleSummaryResponse>> searchSchedules(
			@RequestParam(required = false)
			@SearchQuery
			String query,
			@RequestParam(required = false)
			@EmployeeNumber
			String employeeNumber,
			@PageableDefault(size = 20, sort = "code")
			Pageable pageable,
			Authentication authentication);

	/**
	 * Retrieves an authorized schedule, including all rules and time ranges. Code
	 * must match {@code [A-Za-z0-9_-]{1,50}} without trimming. Elevated callers may
	 * view any schedule; employee-only callers may view their own assigned
	 * schedule. A successful lookup does not change the aggregate.
	 *
	 * @param  scheduleCode                 exact immutable business code
	 * @return                              HTTP 200 containing the schedule
	 * @throws ConstraintViolationException if the code is invalid
	 * @throws ResourceNotFoundException    if the schedule is absent
	 * @throws AccessDeniedException        if the caller cannot view the schedule
	 */
	@GetMapping("/{scheduleCode}")
	@PreAuthorize("@scheduleAuthorization.canView(authentication, #scheduleCode)")
	ResponseEntity<ScheduleResponse> findSchedule(
			@PathVariable("scheduleCode")
			@ScheduleCode
			String scheduleCode);

	/**
	 * Creates a schedule as a provisioned JANUS_ADMIN or JANUS_USER. The nonnull
	 * payload must pass Bean Validation, including nested definitions. Code is
	 * unique and immutable. Name is trimmed and tolerances are nonnegative. Empty
	 * rules or day ranges are allowed. Rule dates must be ordered, each rule has at
	 * most one range per weekday, and effective work cannot exceed the range
	 * duration. Overnight ranges are supported. Invalid requests do not persist a
	 * schedule.
	 *
	 * @param  request                        complete validated schedule definition
	 * @return                                HTTP 201 containing the created
	 *                                        schedule
	 * @throws ResourceAlreadyExistsException if the code already exists
	 * @throws IllegalArgumentException       if a rule definition is inconsistent
	 * @throws AccessDeniedException          if the caller cannot create schedules
	 */
	@PostMapping
	@PreAuthorize("@scheduleAuthorization.canCreate(authentication)")
	ResponseEntity<ScheduleResponse> createSchedule(
			@RequestBody
			@Valid
			CreateScheduleRequest request);

	/**
	 * Replaces an existing schedule as a provisioned JANUS_ADMIN or JANUS_USER.
	 * Code must match {@code [A-Za-z0-9_-]{1,50}} without trimming; the nonnull
	 * payload has creation's constraints. Name is trimmed, tolerances and all rules
	 * are replaced, and code and employee assignments are preserved. Empty rules
	 * remove the complete rule set. Invalid requests do not modify the schedule.
	 *
	 * @param  scheduleCode                 exact immutable business code of the
	 *                                      target
	 * @param  request                      complete validated replacement
	 * @return                              HTTP 200 containing the updated schedule
	 * @throws ConstraintViolationException if the code is invalid
	 * @throws ResourceNotFoundException    if the target is absent
	 * @throws IllegalArgumentException     if a rule definition is inconsistent
	 * @throws AccessDeniedException        if the caller cannot update schedules
	 */
	@PutMapping("/{scheduleCode}")
	@PreAuthorize("@scheduleAuthorization.canUpdate(authentication)")
	ResponseEntity<ScheduleResponse> updateSchedule(
			@PathVariable("scheduleCode")
			@ScheduleCode
			String scheduleCode,
			@RequestBody
			@Valid
			UpdateScheduleRequest request);

	/**
	 * Deletes an unassigned schedule as a provisioned JANUS_ADMIN or JANUS_USER.
	 * Code must match {@code [A-Za-z0-9_-]{1,50}} without trimming and the target
	 * must exist. Rules and ranges are removed with the schedule; assigned
	 * schedules are preserved and return HTTP 409.
	 *
	 * @param  scheduleCode                 exact immutable business code of the
	 *                                      target
	 * @return                              HTTP 204 with an empty body
	 * @throws ConstraintViolationException if the code is invalid
	 * @throws ResourceNotFoundException    if the target is absent
	 * @throws IllegalStateException        if employees are assigned
	 * @throws AccessDeniedException        if the caller cannot delete schedules
	 */
	@DeleteMapping("/{scheduleCode}")
	@PreAuthorize("@scheduleAuthorization.canDelete(authentication)")
	ResponseEntity<Void> deleteSchedule(
			@PathVariable("scheduleCode")
			@ScheduleCode
			String scheduleCode);
}
