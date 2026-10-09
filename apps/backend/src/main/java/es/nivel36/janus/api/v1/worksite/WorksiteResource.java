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
package es.nivel36.janus.api.v1.worksite;

import java.time.Instant;

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
import es.nivel36.janus.validation.SearchQuery;
import es.nivel36.janus.validation.WorksiteCode;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;

/**
 * HTTP contract for worksites at {@code /api/v1/worksites}. Requests require a
 * validated bearer JWT and a previously provisioned actor. Authorization
 * precedes execution; restricted employees operate within their persistent
 * employee association. Worksite deletion is logical and preserves time logs.
 */
@RequestMapping("/api/v1/worksites")
public interface WorksiteResource {

	/**
	 * Returns a page of worksites within the caller's authorized employee scope.
	 * <p>
	 * {@code JANUS_USER} and {@code JANUS_ADMIN} may search all worksites or
	 * request an employee filter. {@code JANUS_EMPLOYEE} requires a persistent
	 * employee link and may only request their own number; omitting it applies that
	 * number automatically. An employee filter includes GLOBAL worksites and
	 * worksites assigned to that employee. Text and visibility restrictions combine
	 * with AND.
	 * </p>
	 * <p>
	 * Query matches code, name, description or address literally, partially and
	 * case-insensitively, without trimming. Null disables the text filter; a
	 * supplied query must contain 1-100 single-line characters. Employee numbers
	 * match {@code [A-Za-z0-9_-]{1,50}} exactly without trimming. Sort fields are
	 * code, name, timeZone, scope, description and address. Page size is capped at
	 * {@code spring.data.rest.max-page-size}; default ordering is ascending code.
	 * Ascending code breaks ties unless code is explicitly ordered.
	 * </p>
	 *
	 * @param  query                        optional literal worksite fragment
	 * @param  employeeNumber               optional employee visibility filter
	 * @param  pageable                     requested page and public ordering;
	 *                                      defaults to page 0, size 20
	 * @param  authentication               trusted authentication of the current
	 *                                      caller
	 * @return                              HTTP 200 containing worksite responses
	 *                                      and page metadata, possibly empty
	 * @throws ConstraintViolationException if a service parameter constraint fails
	 * @throws IllegalArgumentException     if paging or a sort field is unsupported
	 * @throws AccessDeniedException        if the caller cannot search the
	 *                                      requested scope
	 */
	@GetMapping
	@PreAuthorize("@worksiteAuthorization.canSearch(authentication, #employeeNumber)")
	ResponseEntity<Page<WorksiteResponse>> searchWorksites(
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
	 * Returns an existing visible worksite by its exact business code.
	 * {@code JANUS_USER} and {@code JANUS_ADMIN} may view any worksite; a linked
	 * {@code JANUS_EMPLOYEE} may view GLOBAL worksites and their assigned
	 * worksites. Code must match {@code [A-Za-z0-9_-]{1,50}} without trimming.
	 * Deleted worksites are absent.
	 *
	 * @param  worksiteCode              exact worksite code
	 * @return                           HTTP 200 containing the visible worksite
	 * @throws ResourceNotFoundException if the target is absent for an elevated
	 *                                   caller
	 * @throws AccessDeniedException     if the caller cannot view the target
	 */
	@GetMapping("/{worksiteCode}")
	@PreAuthorize("@worksiteAuthorization.canView(authentication, #worksiteCode)")
	ResponseEntity<WorksiteResponse> findWorksite(
			@PathVariable
			@WorksiteCode
			String worksiteCode);

	/**
	 * Returns statistics for an existing worksite over {@code [start, end)}.
	 * {@code JANUS_USER} and {@code JANUS_ADMIN} may query any worksite;
	 * {@code JANUS_EMPLOYEE} must be assigned to it. Code must match
	 * {@code [A-Za-z0-9_-]{1,50}} without trimming. Both instants are required and
	 * end must be strictly after start. Counts use time-log entry times; erroneous
	 * logs are logs without an exit time. The allowed-employee count reports
	 * current explicit assignments, including for GLOBAL worksites.
	 *
	 * @param  worksiteCode              exact worksite code
	 * @param  start                     inclusive entry-time boundary
	 * @param  end                       exclusive entry-time boundary
	 * @return                           HTTP 200 containing the interval and its
	 *                                   statistics
	 * @throws IllegalArgumentException  if end is not strictly after start
	 * @throws ResourceNotFoundException if the target is absent
	 * @throws AccessDeniedException     if the caller cannot query its statistics
	 */
	@GetMapping("/{worksiteCode}/stats")
	@PreAuthorize("@worksiteAuthorization.canViewStats(authentication, #worksiteCode)")
	ResponseEntity<WorksiteStatsResponse> stats(
			@PathVariable
			@WorksiteCode
			String worksiteCode,
			@RequestParam
			Instant start,
			@RequestParam
			Instant end);

	/**
	 * Creates a worksite from a complete validated request. {@code JANUS_USER} and
	 * {@code JANUS_ADMIN} may create either scope. {@code JANUS_EMPLOYEE} may
	 * create ASSIGNED worksites only when employee creation is enabled. Text values
	 * are trimmed; {@code null} description and address remain {@code null}.
	 * Creating a worksite does not assign employees to it.
	 *
	 * @param  request                        nonnull validated identifying and
	 *                                        descriptive data
	 * @return                                HTTP 201 containing the persisted
	 *                                        worksite
	 * @throws ResourceAlreadyExistsException if a visible worksite already uses the
	 *                                        code
	 * @throws AccessDeniedException          if creation is not authorized
	 */
	@PostMapping
	@PreAuthorize("@worksiteAuthorization.canCreate(authentication, #request.scope())")
	ResponseEntity<WorksiteResponse> createWorksite(
			@RequestBody
			@Valid
			CreateWorksiteRequest request);

	/**
	 * Replaces the descriptive data and scope of an existing worksite. Code must
	 * match {@code [A-Za-z0-9_-]{1,50}} without trimming and the complete request
	 * must pass Bean Validation. {@code JANUS_USER} and {@code JANUS_ADMIN} may
	 * update any worksite. {@code JANUS_EMPLOYEE} must be assigned, employee
	 * creation must be enabled and the requested scope must be ASSIGNED. Scope may
	 * remain unchanged or expand from ASSIGNED to GLOBAL. Text values are trimmed;
	 * {@code null} description and address clear those fields. Code, assignments
	 * and time logs are preserved.
	 *
	 * @param  worksiteCode              exact code of the worksite to update
	 * @param  request                   nonnull validated replacement data
	 * @return                           HTTP 200 containing the updated worksite
	 * @throws IllegalArgumentException  if the scope transition is not allowed
	 * @throws ResourceNotFoundException if the target is absent
	 * @throws AccessDeniedException     if the caller cannot update the target
	 */
	@PutMapping("/{worksiteCode}")
	@PreAuthorize("@worksiteAuthorization.canUpdate(authentication, #worksiteCode, #request.scope())")
	ResponseEntity<WorksiteResponse> updateWorksite(
			@PathVariable
			@WorksiteCode
			String worksiteCode,
			@RequestBody
			@Valid
			UpdateWorksiteRequest request);

	/**
	 * Logically deletes an existing worksite as {@code JANUS_USER} or
	 * {@code JANUS_ADMIN}. Code must match {@code [A-Za-z0-9_-]{1,50}} without
	 * trimming. Assigned employees must be removed first. Deleted worksites
	 * disappear from lookup and search while historical time logs remain stored.
	 *
	 * @param  worksiteCode              exact code of the worksite to delete
	 * @return                           HTTP 204 with an empty body
	 * @throws ResourceNotFoundException if the target is absent
	 * @throws IllegalStateException     if employees are still assigned
	 * @throws AccessDeniedException     if the caller cannot delete worksites
	 */
	@DeleteMapping("/{worksiteCode}")
	@PreAuthorize("@worksiteAuthorization.canDelete(authentication)")
	ResponseEntity<Void> deleteWorksite(
			@PathVariable
			@WorksiteCode
			String worksiteCode);

	/**
	 * Idempotently assigns an existing employee to an existing worksite as
	 * {@code JANUS_USER} or {@code JANUS_ADMIN}. Both identifiers must match
	 * {@code [A-Za-z0-9_-]{1,50}} without trimming. Either scope accepts explicit
	 * assignments; both sides of the association are updated.
	 *
	 * @param  worksiteCode              exact worksite code
	 * @param  employeeNumber            exact employee number
	 * @return                           HTTP 204 with an empty body, including if
	 *                                   already assigned
	 * @throws ResourceNotFoundException if either resource is absent
	 * @throws AccessDeniedException     if the caller cannot manage assignments
	 */
	@PutMapping("/{worksiteCode}/employees/{employeeNumber}")
	@PreAuthorize("@worksiteAuthorization.canManageAssignments(authentication)")
	ResponseEntity<Void> assignEmployeeToWorksite(
			@PathVariable
			@WorksiteCode
			String worksiteCode,
			@PathVariable
			@EmployeeNumber
			String employeeNumber);

	/**
	 * Idempotently removes an employee assignment as {@code JANUS_USER} or
	 * {@code JANUS_ADMIN}. Both resources must exist and their identifiers must
	 * match {@code [A-Za-z0-9_-]{1,50}} without trimming. Both sides of the
	 * association are updated; employee and worksite records are preserved.
	 *
	 * @param  worksiteCode              exact worksite code
	 * @param  employeeNumber            exact employee number
	 * @return                           HTTP 204 with an empty body, including if
	 *                                   no assignment existed
	 * @throws ResourceNotFoundException if either resource is absent
	 * @throws AccessDeniedException     if the caller cannot manage assignments
	 */
	@DeleteMapping("/{worksiteCode}/employees/{employeeNumber}")
	@PreAuthorize("@worksiteAuthorization.canManageAssignments(authentication)")
	ResponseEntity<Void> removeEmployeeFromWorksite(
			@PathVariable
			@WorksiteCode
			String worksiteCode,
			@PathVariable
			@EmployeeNumber
			String employeeNumber);
}
