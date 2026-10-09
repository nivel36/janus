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
package es.nivel36.janus.service.employee;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import es.nivel36.janus.service.ResourceAlreadyExistsException;
import es.nivel36.janus.service.ResourceNotFoundException;
import es.nivel36.janus.service.schedule.Schedule;
import es.nivel36.janus.service.schedule.ScheduleService;
import es.nivel36.janus.service.timelog.TimeLog;
import es.nivel36.janus.service.workshift.WorkShift;
import es.nivel36.janus.service.worksite.Worksite;
import es.nivel36.janus.util.EmailAddresses;
import es.nivel36.janus.util.LikePatterns;
import es.nivel36.janus.validation.EmployeeNumber;
import es.nivel36.janus.validation.ScheduleCode;
import es.nivel36.janus.validation.SearchQuery;
import es.nivel36.janus.validation.WorksiteCode;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Transactional entry point for employee creation, lookup, search, replacement,
 * deletion and working-time counts. Callers authorize operations before
 * invoking this service. Parameter constraints are enforced through the Spring
 * proxy.
 */
@Validated
@Service
public class EmployeeService {

	private static final Logger logger = LoggerFactory.getLogger(EmployeeService.class);

	private final EmployeeRepository employeeRepository;
	private final ScheduleService scheduleService;
	private final int maxPageSize;

	/**
	 * Creates a service without accessing persistence. Dependencies must be nonnull
	 * and the configured page-size limit must be positive.
	 *
	 * @param  employeeRepository       repository used to manage {@link Employee}
	 *                                  entities
	 * @param  scheduleService          service used to resolve schedules inside
	 *                                  secured employee operations
	 * @param  maxPageSize              positive limit from
	 *                                  {@code spring.data.rest.max-page-size}
	 * @throws NullPointerException     if either dependency is {@code null}
	 * @throws IllegalArgumentException if maxPageSize is not positive
	 */
	public EmployeeService(
		final EmployeeRepository employeeRepository,
		final ScheduleService scheduleService,
		final @Value("${spring.data.rest.max-page-size}") int maxPageSize) {
		this.employeeRepository = Objects.requireNonNull(employeeRepository, "employeeRepository cannot be null.");
		this.scheduleService = Objects.requireNonNull(scheduleService, "scheduleService cannot be null.");
		if (maxPageSize < 1) {
			throw new IllegalArgumentException("maxPageSize must be positive");
		}
		this.maxPageSize = maxPageSize;
	}

	/**
	 * Returns an employee by its persistent identifier.
	 *
	 * @param  id                           the persistent employee identifier; must
	 *                                      not be {@code null}
	 * @return                              the matching employee
	 * @throws ResourceNotFoundException    if no employee exists with that
	 *                                      identifier
	 * @throws ConstraintViolationException if {@code id} is {@code null} when
	 *                                      method validation is active
	 * @throws IllegalArgumentException     if {@code id} is {@code null} on a
	 *                                      direct call to the repository
	 */
	@Transactional(readOnly = true)
	public Employee findEmployeeById(final @NotNull Long id) {
		logger.debug("Finding employee by id {}", id);
		return this.employeeRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("There is no employee with id " + id));
	}

	/**
	 * Returns the employee whose stored email exactly matches the supplied value.
	 * <p>
	 * The input is not trimmed or lowercased. Callers seeking normalized contact
	 * matching must normalize it before invoking this operation.
	 *
	 * @param  email                        the nonblank valid email of at most
	 *                                      {@code 254} characters
	 * @return                              the matching employee, or an empty
	 *                                      optional if no email matches
	 * @throws ConstraintViolationException if a parameter constraint fails when
	 *                                      method validation is active
	 */
	@Transactional(readOnly = true)
	public Optional<Employee> findEmployeeByEmail(final @NotBlank @Email @Size(max = 254) String email) {
		return this.employeeRepository.findByEmail(email);
	}

	/**
	 * Looks up an employee by its exact immutable number without changing it.
	 * Number must match {@code [A-Za-z0-9_-]{1,50}} without trimming. The schedule
	 * is loaded. A missing employee does not mark the caller's transaction for
	 * rollback.
	 *
	 * @param  employeeNumber               exact stable employee number
	 * @return                              matching employee with readable schedule
	 * @throws ConstraintViolationException if the number is invalid through the
	 *                                      Spring proxy
	 * @throws ResourceNotFoundException    if no employee exists with this number
	 */
	@Transactional(readOnly = true, noRollbackFor = ResourceNotFoundException.class)
	public Employee findEmployeeByEmployeeNumber(final @NotBlank @EmployeeNumber String employeeNumber) {
		logger.debug("Finding employee with employee number {}", employeeNumber);
		final Employee employee = this.employeeRepository.findByEmployeeNumber(employeeNumber);
		if (employee == null) {
			throw new ResourceNotFoundException("There is no employee with number " + employeeNumber);
		}
		return employee;
	}

	/**
	 * Finds the identifiers of employees who have at least one closed, nondeleted
	 * {@link TimeLog} entered since the specified instant and not associated with a
	 * {@link WorkShift}.
	 *
	 * @param  start                        the lower bound instant (inclusive).
	 *                                      Must not be {@code null}.
	 * @return                              a list of employee identifiers matching
	 *                                      the criteria
	 * @throws ConstraintViolationException if {@code start} is {@code null} when
	 *                                      method validation is active
	 */
	@Transactional(readOnly = true)
	public List<Long> findEmployeesWithoutWorkshiftsSince(final @NotNull Instant start) {
		logger.debug("Finding employees without workshift from date: {}", start);

		final List<Long> employeesWithoutWorkshift = this.employeeRepository.findWithoutWorkshiftsSince(start);

		logger.trace("Found {} employees without workshift", employeesWithoutWorkshift.size());
		return employeesWithoutWorkshift;
	}

	/**
	 * Creates and persists a new {@link Employee}.
	 * <p>
	 * The caller must authorize creation. Employee number must match
	 * {@code [A-Za-z0-9_-]{1,50}}; names must be nonblank and contain 1-255
	 * letters, spaces, dots, commas, apostrophes or hyphens. Email must
	 * satisfy @Email and have at most 254 characters; schedule must be nonnull.
	 * Employee number and normalized email must be unique. Creation trims and
	 * lowercases email, retains the number and names as supplied and does not link
	 * a local profile.
	 * </p>
	 *
	 * @param  employeeNumber                 immutable unique employee number
	 * @param  name                           the first name of the employee. Must
	 *                                        not be {@code null} or blank.
	 * @param  surname                        the surname of the employee. Must not
	 *                                        be {@code null} or blank.
	 * @param  email                          the unique email of the employee. Must
	 *                                        not be {@code null} or blank.
	 * @param  schedule                       the {@link Schedule} assigned to the
	 *                                        employee. Must not be {@code null}.
	 * @return                                the newly created {@link Employee}
	 * @throws NullPointerException           if any parameter is {@code null}
	 * @throws IllegalArgumentException       if any string parameter is blank or
	 *                                        the normalized email exceeds 254
	 *                                        characters
	 * @throws ResourceAlreadyExistsException if employee number or normalized email
	 *                                        is in use
	 * @throws ConstraintViolationException   if a parameter constraint fails
	 *                                        through the Spring proxy
	 */
	@Transactional
	public Employee createEmployee(
			final @NotBlank @EmployeeNumber String employeeNumber,
			final @NotBlank @Pattern(regexp = "^[\\p{L} .,'-]{1,255}$") String name,
			final @NotBlank @Pattern(regexp = "^[\\p{L} .,'-]{1,255}$") String surname,
			final @NotBlank @Email @Size(max = 254) String email,
			final @NotNull Schedule schedule) {
		final String normalizedEmail = EmailAddresses.canonicalize(email);
		logger.debug("Creating employee with employee number {}", employeeNumber);

		if (this.employeeRepository.existsByEmployeeNumber(employeeNumber)) {
			logger.warn("Employee creation rejected: reason=number_in_use, employeeNumber={}", employeeNumber);
			throw new ResourceAlreadyExistsException("Employee with number " + employeeNumber + " already exists");
		}
		final boolean emailInUse = this.employeeRepository.existsByEmail(normalizedEmail);
		if (emailInUse) {
			logger.warn("Employee creation rejected: reason=email_in_use, employeeNumber={}", employeeNumber);
			throw new ResourceAlreadyExistsException("Employee with email " + normalizedEmail + " already exists");
		}

		final Employee employee = new Employee(employeeNumber, name, surname, normalizedEmail, schedule);

		final Employee savedEmployee = this.employeeRepository.save(employee);
		logger.info("Employee created: employeeId={}, employeeNumber={}", savedEmployee.getId(), employeeNumber);
		return savedEmployee;
	}

	/**
	 * Replaces personal data and schedule of an employee identified by its
	 * immutable number.
	 * <p>
	 * The caller must authorize the change and supply arguments satisfying the same
	 * number, name and email constraints as creation, plus a valid schedule code.
	 * Employee and schedule must exist. Email is trimmed and lowercased before
	 * checking uniqueness. All checks precede mutation. Employee number and
	 * profile, worksite and time-record associations are preserved.
	 * </p>
	 *
	 * @param  employeeNumber                 the immutable employee number to
	 *                                        update. Must not be {@code null} or
	 *                                        blank.
	 * @param  newName                        the new first name. Must not be
	 *                                        {@code null} or blank.
	 * @param  newSurname                     the new surname. Must not be
	 *                                        {@code null} or blank.
	 * @param  newEmail                       replacement unique contact email
	 * @param  scheduleCode                   code of the new {@link Schedule}. Must
	 *                                        not be {@code null}.
	 * @return                                the updated {@link Employee}
	 * @throws NullPointerException           if a replacement name or email is
	 *                                        {@code null} on a direct call
	 * @throws IllegalArgumentException       if a name or email is blank or the
	 *                                        normalized email exceeds 254
	 *                                        characters
	 * @throws ResourceNotFoundException      if no employee exists with the given
	 *                                        number or no schedule exists with the
	 *                                        given code
	 * @throws ResourceAlreadyExistsException if the replacement email is in use
	 * @throws ConstraintViolationException   if a parameter constraint fails
	 *                                        through the Spring proxy
	 */
	@Transactional
	public Employee updateEmployee(
			final @NotBlank @EmployeeNumber String employeeNumber,
			final @NotBlank @Pattern(regexp = "^[\\p{L} .,'-]{1,255}$") String newName,
			final @NotBlank @Pattern(regexp = "^[\\p{L} .,'-]{1,255}$") String newSurname,
			final @NotBlank @Size(max = 254) @Email String newEmail,
			final @NotBlank @ScheduleCode String scheduleCode) {
		final String normalizedEmail = EmailAddresses.canonicalize(newEmail);
		logger.atDebug().addKeyValue("employeeNumber", employeeNumber).addKeyValue("scheduleCode", scheduleCode)
				.log("Updating employee");

		final Schedule newSchedule = this.scheduleService.findScheduleByCode(scheduleCode);
		final Employee employee = this.findEmployeeByEmployeeNumber(employeeNumber);
		if (!employee.getEmail().equals(normalizedEmail) && this.employeeRepository.existsByEmail(normalizedEmail)) {
			throw new ResourceAlreadyExistsException("Employee with email " + normalizedEmail + " already exists");
		}
		employee.setFullName(newName, newSurname);
		employee.changeEmail(normalizedEmail);
		employee.setSchedule(newSchedule);

		return employee;
	}

	/**
	 * Deletes an existing {@link Employee} by its exact employee number.
	 * <p>
	 * The caller must authorize deletion. Removal is committed with the
	 * transaction; references from profiles or time records may prevent it through
	 * persistence constraints. This operation does not cascade deletion to a local
	 * profile or a provider account.
	 * </p>
	 *
	 * @param  employeeNumber               natural key of the employee to delete
	 * @throws ResourceNotFoundException    if the employee does not exist
	 * @throws ConstraintViolationException if a parameter constraint fails through
	 *                                      the Spring proxy
	 */
	@Transactional
	public void deleteEmployee(final @NotBlank @EmployeeNumber String employeeNumber) {
		logger.debug("Employee with employee number {} marked for deletion", employeeNumber);
		if (this.employeeRepository.deleteByEmployeeNumber(employeeNumber) == 0) {
			throw new ResourceNotFoundException("There is no employee with employee number " + employeeNumber);
		}
	}

	/**
	 * Determines whether an employee identified by its internal persistence ID is
	 * assigned to a schedule with the specified business code.
	 * <p>
	 * This method checks for the existence of an {@link Employee} whose internal
	 * identifier ({@code id}) matches the provided value and whose associated
	 * {@link Schedule} has the given {@code code}.
	 * </p>
	 * <p>
	 * The employee id is an internal key and the schedule code is a business
	 * identifier. The method returns {@code true} as soon as a matching assignment
	 * is found.
	 * </p>
	 *
	 * @param  employeeId                   the internal id of the employee; must
	 *                                      not be {@code null}
	 * @param  scheduleCode                 the business code of the schedule; must
	 *                                      not be {@code null}
	 * @return                              {@code true} if the employee is assigned
	 *                                      to the specified schedule; {@code false}
	 *                                      otherwise
	 * @throws ConstraintViolationException if a parameter constraint fails through
	 *                                      the Spring proxy
	 */
	@Transactional(readOnly = true)
	public boolean isAssignedToSchedule(
			final @NotNull Long employeeId,
			final @NotBlank @ScheduleCode String scheduleCode) {
		logger.debug("Checking if the employee {} is assigned to schedule {}", employeeId, scheduleCode);
		return this.employeeRepository.existsByIdAndSchedule_Code(employeeId, scheduleCode);
	}

	/**
	 * Determines whether an employee identified by its internal persistence ID is
	 * assigned to a worksite with the specified business code.
	 * <p>
	 * This method checks for the existence of an {@link Employee} whose internal
	 * identifier ({@code id}) matches the provided value and whose associated
	 * {@link Worksite} has the given {@code code}.
	 * </p>
	 * <p>
	 * The employee id is an internal key and the worksite code is a business
	 * identifier. The method returns {@code true} as soon as a matching assignment
	 * is found.
	 * </p>
	 *
	 * @param  employeeId                   the internal id of the employee; must
	 *                                      not be {@code null}
	 * @param  worksiteCode                 the business code of the worksite; must
	 *                                      not be {@code null}
	 * @return                              {@code true} if the employee is assigned
	 *                                      to the specified worksite; {@code false}
	 *                                      otherwise
	 * @throws ConstraintViolationException if a parameter constraint fails through
	 *                                      the Spring proxy
	 */
	@Transactional(readOnly = true)
	public boolean isAssignedToWorksite(
			final @NotNull Long employeeId,
			final @NotBlank @WorksiteCode String worksiteCode) {
		logger.debug("Checking if the employee {} is assigned to worksite {}", employeeId, worksiteCode);
		return this.employeeRepository.existsByIdAndWorksites_Code(employeeId, worksiteCode);
	}

	/**
	 * Counts distinct employees currently assigned to the exact worksite code.
	 * Worksite code must match {@code [A-Za-z0-9_-]{1,50}}. This operation does not
	 * modify records.
	 *
	 * @param  worksiteCode                 exact worksite business code
	 * @return                              matching count, zero if no records match
	 * @throws ConstraintViolationException if a parameter constraint fails through
	 *                                      the Spring proxy
	 */
	@Transactional(readOnly = true)
	public long countEmployeesAssignedToWorksite(final @NotBlank @WorksiteCode String worksiteCode) {
		return this.employeeRepository.countByWorksiteCode(worksiteCode);
	}

	/**
	 * Counts distinct employees with nondeleted time logs entered in the range.
	 * Range is [start, end); both bounds must be nonnull and start must precede
	 * end. Worksite code must match {@code [A-Za-z0-9_-]{1,50}}. This operation
	 * does not modify records.
	 *
	 * @param  worksiteCode                 exact worksite business code
	 * @param  start                        inclusive entry-time bound
	 * @param  end                          exclusive entry-time bound
	 * @return                              matching count, zero if no records match
	 * @throws ConstraintViolationException if a parameter constraint fails through
	 *                                      the Spring proxy
	 * @throws IllegalArgumentException     if start does not precede end
	 */
	@Transactional(readOnly = true)
	public long countDistinctEmployeesWithTimeLogsInRange(
			final @NotBlank @WorksiteCode String worksiteCode,
			final @NotNull Instant start,
			final @NotNull Instant end) {
		validateRange(start, end);
		return this.employeeRepository.countDistinctEmployeesWithTimeLogsInRange(worksiteCode, start, end);
	}

	/**
	 * Counts nondeleted time logs entered in the range, including open logs. Range
	 * is [start, end); both bounds must be nonnull and start must precede end.
	 * Worksite code must match {@code [A-Za-z0-9_-]{1,50}}. This operation does not
	 * modify records.
	 *
	 * @param  worksiteCode                 exact worksite business code
	 * @param  start                        inclusive entry-time bound
	 * @param  end                          exclusive entry-time bound
	 * @return                              matching count, zero if no records match
	 * @throws ConstraintViolationException if a parameter constraint fails through
	 *                                      the Spring proxy
	 * @throws IllegalArgumentException     if start does not precede end
	 */
	@Transactional(readOnly = true)
	public long countTimeLogsInRange(
			final @NotBlank @WorksiteCode String worksiteCode,
			final @NotNull Instant start,
			final @NotNull Instant end) {
		validateRange(start, end);
		return this.employeeRepository.countTimeLogsInRange(worksiteCode, start, end);
	}

	/**
	 * Counts nondeleted open time logs entered in the range. Range is [start, end);
	 * both bounds must be nonnull and start must precede end. Worksite code must
	 * match {@code [A-Za-z0-9_-]{1,50}}. This operation does not modify records.
	 *
	 * @param  worksiteCode                 exact worksite business code
	 * @param  start                        inclusive entry-time bound
	 * @param  end                          exclusive entry-time bound
	 * @return                              matching count, zero if no records match
	 * @throws ConstraintViolationException if a parameter constraint fails through
	 *                                      the Spring proxy
	 * @throws IllegalArgumentException     if start does not precede end
	 */
	@Transactional(readOnly = true)
	public long countOpenTimeLogsInRange(
			final @NotBlank @WorksiteCode String worksiteCode,
			final @NotNull Instant start,
			final @NotNull Instant end) {
		validateRange(start, end);
		return this.employeeRepository.countOpenTimeLogsInRange(worksiteCode, start, end);
	}

	/**
	 * Counts distinct current employee schedules represented by nondeleted time
	 * logs in the range. Range is [start, end); both bounds must be nonnull and
	 * start must precede end. Worksite code must match {@code [A-Za-z0-9_-]{1,50}}.
	 * This operation does not modify records.
	 *
	 * @param  worksiteCode                 exact worksite business code
	 * @param  start                        inclusive entry-time bound
	 * @param  end                          exclusive entry-time bound
	 * @return                              matching count, zero if no records match
	 * @throws ConstraintViolationException if a parameter constraint fails through
	 *                                      the Spring proxy
	 * @throws IllegalArgumentException     if start does not precede end
	 */
	@Transactional(readOnly = true)
	public long countDistinctSchedulesInRange(
			final @NotBlank @WorksiteCode String worksiteCode,
			final @NotNull Instant start,
			final @NotNull Instant end) {
		validateRange(start, end);
		return this.employeeRepository.countDistinctSchedulesInRange(worksiteCode, start, end);
	}

	/**
	 * Searches employees without changing them. The caller must authorize search
	 * and supply a nonnull paged request. Query is optional single-line text of
	 * 1-100 characters; optional codes match {@code [A-Za-z0-9_-]{1,50}} without
	 * trimming. Text matches employee number, name, surname or email partially,
	 * case-insensitively and literally. Code filters match exactly and combine with
	 * AND; {@code null} filters disable their restriction. Schedules are loaded.
	 * Public sort fields are employeeNumber, name, surname, email and scheduleCode.
	 * Default ordering and the tie-breaker are ascending employee number, unless
	 * explicitly sorted. Page size is capped at
	 * {@code spring.data.rest.max-page-size}.
	 *
	 * @param  query                        optional literal text fragment, used
	 *                                      without trimming
	 * @param  scheduleCode                 optional exact schedule code
	 * @param  worksiteCode                 optional exact worksite code
	 * @param  pageable                     requested page and public ordering
	 * @return                              page of matching employees with readable
	 *                                      schedules, possibly empty
	 * @throws ConstraintViolationException if a parameter constraint fails through
	 *                                      the Spring proxy
	 * @throws IllegalArgumentException     if pageable is unpaged or a sort field
	 *                                      is unsupported
	 */
	@Transactional(readOnly = true)
	public Page<Employee> searchEmployees(
			final @SearchQuery String query,
			final @ScheduleCode String scheduleCode,
			final @WorksiteCode String worksiteCode,
			final @NotNull Pageable pageable) {
		final Pageable normalizedPageable = this.normalizePageable(pageable);
		final String escapedQuery = query == null ? "" : LikePatterns.escape(query);
		logger.atDebug().addKeyValue("query", query).addKeyValue("scheduleCode", scheduleCode)
				.addKeyValue("worksiteCode", worksiteCode).addKeyValue("page", normalizedPageable.getPageNumber())
				.addKeyValue("pageSize", normalizedPageable.getPageSize())
				.addKeyValue("sort", normalizedPageable.getSort().toString()).log("Searching employees");
		return this.employeeRepository.search(escapedQuery, scheduleCode, worksiteCode, normalizedPageable);
	}

	private Pageable normalizePageable(final Pageable pageable) {
		Objects.requireNonNull(pageable, "pageable cannot be null.");
		if (pageable.isUnpaged()) {
			throw new IllegalArgumentException("Must be paged");
		}
		final List<Order> orders = new ArrayList<>();
		for (final Order order : pageable.getSort()) {
			final String property = switch (order.getProperty()) {
			case "employeeNumber", "name", "surname", "email" -> order.getProperty();
			case "scheduleCode" -> "schedule.code";
			default -> throw new IllegalArgumentException("Unsupported Employee sort field: " + order.getProperty());
			};
			orders.add(order.withProperty(property));
		}
		if (orders.stream().noneMatch(order -> "employeeNumber".equals(order.getProperty()))) {
			orders.add(Order.asc("employeeNumber"));
		}
		return PageRequest
				.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), this.maxPageSize), Sort.by(orders));
	}

	private static void validateRange(final Instant start, final Instant end) {
		if (!start.isBefore(end)) {
			throw new IllegalArgumentException("end must be after start");
		}
	}
}
