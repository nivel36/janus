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
package es.nivel36.janus.api.v1.employee;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
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
import es.nivel36.janus.validation.WorksiteCode;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;

/**
 * HTTP contract for employee records at {@code /api/v1/employees}. Requests
 * require a validated bearer JWT and a provisioned local profile. Authorization
 * precedes execution and uses the persistent subject-to-employee link.
 */
@RequestMapping("/api/v1/employees")
public interface EmployeeResource {

	/**
	 * Returns a page of employees for a provisioned {@code JANUS_ADMIN} or
	 * {@code JANUS_USER}.
	 * <p>
	 * Optional query text contains 1-100 single-line characters and is used
	 * literally without trimming. Optional schedule and worksite codes must match
	 * {@code [A-Za-z0-9_-]{1,50}}. Pageable must be nonnull and paged.
	 * </p>
	 * <p>
	 * Returns HTTP 200 without changing employees. Text matches employee number,
	 * name, surname or email partially and case-insensitively; code filters match
	 * exactly and combine with AND. Absent filters include all employees. Public
	 * sort fields are employeeNumber, name, surname, email and scheduleCode. The
	 * default order is ascending employee number, also appended as a tie-breaker
	 * unless explicitly sorted. Page size is capped at the configured
	 * {@code spring.data.rest.max-page-size} (100 by default).
	 * </p>
	 *
	 * @param  query                        optional literal text fragment;
	 *                                      {@code null} disables this filter
	 * @param  scheduleCode                 optional exact schedule code;
	 *                                      {@code null} disables this filter
	 * @param  worksiteCode                 optional exact worksite code;
	 *                                      {@code null} disables this filter
	 * @param  pageable                     requested page and ordering; HTTP
	 *                                      defaults are page 0, size 20
	 * @return                              HTTP 200 containing employee responses
	 *                                      and page metadata, possibly empty
	 * @throws ConstraintViolationException if a service parameter constraint fails
	 * @throws IllegalArgumentException     if paging or a sort field is unsupported
	 * @throws AccessDeniedException        if the caller cannot search employees
	 */
	@GetMapping
	@PreAuthorize("@employeeAuthorization.canSearch(authentication)")
	ResponseEntity<Page<EmployeeResponse>> searchEmployees(
			@RequestParam(required = false)
			@SearchQuery
			String query,
			@RequestParam(required = false)
			@ScheduleCode
			String scheduleCode,
			@RequestParam(required = false)
			@WorksiteCode
			String worksiteCode,
			@PageableDefault(size = 20, sort = "employeeNumber")
			Pageable pageable);

	/**
	 * Retrieves an employee by its exact immutable number without changing it. The
	 * number must match {@code [A-Za-z0-9_-]{1,50}} without trimming. Provisioned
	 * {@code JANUS_ADMIN} and {@code JANUS_USER} may read any employee;
	 * {@code JANUS_EMPLOYEE} may read only the employee linked to their persistent
	 * profile.
	 *
	 * @param  employeeNumber               exact stable number of the employee
	 * @return                              HTTP 200 containing the employee's
	 *                                      public data and schedule code
	 * @throws ConstraintViolationException if the number violates service
	 *                                      constraints
	 * @throws ResourceNotFoundException    if the employee is absent for an
	 *                                      elevated caller
	 * @throws AccessDeniedException        if the caller cannot view the employee
	 */
	@GetMapping("/{employeeNumber}")
	@PreAuthorize("@employeeAuthorization.canView(authentication, #employeeNumber)")
	ResponseEntity<EmployeeResponse> findEmployee(
			@PathVariable("employeeNumber")
			@EmployeeNumber
			String employeeNumber);

	/**
	 * Creates an employee as a provisioned {@code JANUS_ADMIN} or
	 * {@code JANUS_USER}.
	 * <p>
	 * Request must be nonnull and pass Bean Validation. Employee number and
	 * schedule code must match {@code [A-Za-z0-9_-]{1,50}} and the schedule must
	 * exist. Names contain 1-255 letters, spaces, dots, commas, apostrophes or
	 * hyphens and must not be blank. Email must satisfy {@code @Email} and have at
	 * most 254 characters. Employee number and normalized email must be unique.
	 * </p>
	 * <p>
	 * Returns HTTP 201 with the persisted employee. Names are trimmed and email is
	 * trimmed and lowercased. Creation does not provision or link an AppUser.
	 * Invalid requests and authorization denials do not create an employee.
	 * </p>
	 *
	 * @param  request                        complete validated employee definition
	 * @return                                HTTP 201 containing the created
	 *                                        employee
	 * @throws ResourceNotFoundException      if the schedule is absent
	 * @throws ResourceAlreadyExistsException if employee number or email is in use
	 * @throws AccessDeniedException          if the caller cannot create employees
	 */
	@PostMapping
	@PreAuthorize("@employeeAuthorization.canCreate(authentication)")
	ResponseEntity<EmployeeResponse> createEmployee(
			@RequestBody
			@Valid
			CreateEmployeeRequest request);

	/**
	 * Replaces an employee's personal data and schedule in one transaction.
	 * <p>
	 * Employee number must match {@code [A-Za-z0-9_-]{1,50}} without trimming,
	 * request must be nonnull and satisfy the same payload constraints as creation,
	 * and employee and schedule must exist. Provisioned {@code JANUS_ADMIN} and
	 * {@code JANUS_USER} may edit any employee; {@code JANUS_EMPLOYEE} may edit
	 * only their linked employee.
	 * </p>
	 * <p>
	 * Returns HTTP 200 with trimmed names, normalized email and the new schedule.
	 * Employee number, local profile, worksites and time records are preserved. A
	 * duplicate email or invalid request leaves the employee unchanged.
	 * </p>
	 *
	 * @param  employeeNumber                 exact immutable number of the employee
	 *                                        to update
	 * @param  request                        complete validated personal-data and
	 *                                        schedule replacement
	 * @return                                HTTP 200 containing the updated
	 *                                        employee
	 * @throws ConstraintViolationException   if a service parameter constraint
	 *                                        fails
	 * @throws ResourceNotFoundException      if employee or schedule is absent
	 * @throws ResourceAlreadyExistsException if the new email is in use
	 * @throws AccessDeniedException          if the caller cannot update the
	 *                                        employee
	 */
	@PutMapping("/{employeeNumber}")
	@PreAuthorize("@employeeAuthorization.canUpdate(authentication, #employeeNumber)")
	ResponseEntity<EmployeeResponse> updateEmployee(
			@PathVariable("employeeNumber")
			@EmployeeNumber
			String employeeNumber,
			@RequestBody
			@Valid
			UpdateEmployeeRequest request);

	/**
	 * Deletes an existing employee as a provisioned {@code JANUS_ADMIN} or
	 * {@code JANUS_USER}. The number must match {@code [A-Za-z0-9_-]{1,50}} without
	 * trimming. Returns HTTP 204 with no body after deletion. Referenced employees
	 * may be rejected by persistence constraints; this operation does not delete a
	 * provider account.
	 *
	 * @param  employeeNumber               exact immutable number of the employee
	 *                                      to delete
	 * @return                              HTTP 204 with an empty body
	 * @throws ConstraintViolationException if the number violates service
	 *                                      constraints
	 * @throws ResourceNotFoundException    if the employee is absent
	 * @throws AccessDeniedException        if the caller cannot delete employees
	 */
	@DeleteMapping("/{employeeNumber}")
	@PreAuthorize("@employeeAuthorization.canDelete(authentication)")
	ResponseEntity<Void> deleteEmployee(
			@PathVariable("employeeNumber")
			@EmployeeNumber
			String employeeNumber);
}
