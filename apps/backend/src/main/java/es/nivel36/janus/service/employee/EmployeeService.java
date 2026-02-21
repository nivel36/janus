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
package es.nivel36.janus.service.employee;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.nivel36.janus.service.ResourceAlreadyExistsException;
import es.nivel36.janus.service.ResourceNotFoundException;
import es.nivel36.janus.service.schedule.Schedule;
import es.nivel36.janus.service.timelog.TimeLog;
import es.nivel36.janus.service.workshift.WorkShift;
import es.nivel36.janus.service.worksite.Worksite;
import es.nivel36.janus.util.Strings;

/**
 * Service responsible for managing {@link Employee} entities.
 *
 * <p>
 * This service provides application-level operations for retrieving, creating,
 * updating and deleting {@link Employee} instances, as well as managing their
 * associations with {@link Worksite}s.
 * </p>
 *
 * <p>
 * Persistence concerns are delegated to the {@link EmployeeRepository}.
 * </p>
 */
@Service
public class EmployeeService {

	private static final Logger logger = LoggerFactory.getLogger(EmployeeService.class);

	/**
	 * Repository used to access {@link Employee} persistence operations.
	 */
	private final EmployeeRepository employeeRepository;

	/**
	 * Creates a new {@code EmployeeService}.
	 *
	 * @param employeeRepository repository used to manage {@link Employee}
	 *                           entities. Can't be {@code null}.
	 *
	 * @throws NullPointerException if {@code employeeRepository} is {@code null}
	 */
	public EmployeeService(final EmployeeRepository employeeRepository) {
		this.employeeRepository = Objects.requireNonNull(employeeRepository, "employeeRepository cannot be null.");
	}

	/**
	 * Retrieves an {@link Employee} by its primary identifier.
	 *
	 * @param id the unique identifier of the employee. Can't be {@code null}.
	 *
	 * @return the {@link Employee} with the given identifier
	 *
	 * @throws NullPointerException      if {@code id} is {@code null}
	 * @throws ResourceNotFoundException if no employee exists with the given id
	 */
	@Transactional(readOnly = true)
	public Employee findEmployeeById(final Long id) {
		Objects.requireNonNull(id, "id can't be null");
		logger.debug("Finding employee by id {}", id);

		return this.employeeRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("There is no employee with id " + id));
	}

	/**
	 * Retrieves an {@link Employee} identified by its email address.
	 *
	 * <p>
	 * The email acts as a natural identifier for the employee.
	 * </p>
	 *
	 * @param email the email of the employee to retrieve. Can't be {@code null} or
	 *              blank.
	 *
	 * @return the {@link Employee} associated with the given email
	 *
	 * @throws NullPointerException      if {@code email} is {@code null}
	 * @throws IllegalArgumentException  if {@code email} is blank
	 * @throws ResourceNotFoundException if no employee exists with the given email
	 */
	@Transactional(readOnly = true)
	public Employee findEmployeeByEmail(final String email) {
		Strings.requireNonBlank(email, "email cannot be null or blank.");
		logger.debug("Finding Employee by email {}", email);

		return this.findEmployee(email);
	}

	/**
	 * Finds the identifiers of employees who have at least one {@link TimeLog}
	 * since the specified instant but have no associated {@link WorkShift}.
	 *
	 * @param fromInclusive the lower bound instant (inclusive). Can't be
	 *                      {@code null}.
	 *
	 * @return a list of employee identifiers matching the criteria
	 *
	 * @throws NullPointerException if {@code fromInclusive} is {@code null}
	 */
	@Transactional(readOnly = true)
	public List<Long> findEmployeesWithoutWorkshiftsSince(final Instant fromInclusive) {
		Objects.requireNonNull(fromInclusive, "fromInclusive must not be null");
		logger.debug("Finding employees without workshift from date: {}", fromInclusive);

		final List<Long> employeesWithoutWorkshift = this.employeeRepository.findWithoutWorkshiftsSince(fromInclusive);

		logger.trace("Found {} employees without workshift", employeesWithoutWorkshift.size());
		return employeesWithoutWorkshift;
	}

	/**
	 * Creates and persists a new {@link Employee}.
	 *
	 * <p>
	 * The employee email must be unique across the system.
	 * </p>
	 *
	 * @param name     the first name of the employee. Can't be {@code null} or
	 *                 blank.
	 * @param surname  the surname of the employee. Can't be {@code null} or blank.
	 * @param email    the unique email of the employee. Can't be {@code null} or
	 *                 blank.
	 * @param schedule the {@link Schedule} assigned to the employee. Can't be
	 *                 {@code null}.
	 *
	 * @return the newly created {@link Employee}
	 *
	 * @throws NullPointerException           if any parameter is {@code null}
	 * @throws IllegalArgumentException       if any string parameter is blank
	 * @throws ResourceAlreadyExistsException if an employee with the given email
	 *                                        already exists
	 */
	@Transactional
	public Employee createEmployee(final String name, final String surname, final String email,
			final Schedule schedule) {

		Strings.requireNonBlank(name, "name cannot be null or blank.");
		Strings.requireNonBlank(surname, "surname cannot be null or blank.");
		Strings.requireNonBlank(email, "email cannot be null or blank.");
		Objects.requireNonNull(schedule, "schedule cannot be null.");

		logger.debug("Creating new employee {}", email);

		final boolean emailInUse = this.employeeRepository.existsByEmail(email);
		if (emailInUse) {
			logger.warn("Employee with email {} already exists", email);
			throw new ResourceAlreadyExistsException("Employee with email " + email + " already exists");
		}

		final Employee employee = new Employee(name.trim(), surname.trim(), email.trim(), schedule);

		return this.employeeRepository.save(employee);
	}

	/**
	 * Updates an existing {@link Employee} identified by its email.
	 *
	 * <p>
	 * Replaces the employee's personal information and schedule atomically.
	 * </p>
	 *
	 * @param email       the unique email of the employee to update. Can't be
	 *                    {@code null} or blank.
	 * @param newName     the new first name. Can't be {@code null} or blank.
	 * @param newSurname  the new surname. Can't be {@code null} or blank.
	 * @param newSchedule the new {@link Schedule}. Can't be {@code null}.
	 *
	 * @return the updated {@link Employee}
	 *
	 * @throws NullPointerException      if any parameter is {@code null}
	 * @throws IllegalArgumentException  if any string parameter is blank
	 * @throws ResourceNotFoundException if no employee exists with the given email
	 */
	@Transactional
	public Employee updateEmployee(final String email, final String newName, final String newSurname,
			final Schedule newSchedule) {

		Strings.requireNonBlank(email, "email cannot be null or blank.");
		Strings.requireNonBlank(newName, "newName cannot be null or blank.");
		Strings.requireNonBlank(newSurname, "newSurname cannot be null or blank.");
		Objects.requireNonNull(newSchedule, "newSchedule cannot be null.");

		logger.debug("Updating employee {}", email);

		final Employee employee = this.findEmployee(email);
		employee.setFullName(newName.trim(), newSurname.trim());
		employee.setSchedule(newSchedule);

		return employee;
	}

	/**
	 * Associates the given {@link Worksite} with the specified {@link Employee}.
	 *
	 * <p>
	 * This operation is idempotent: if the association already exists, no changes
	 * are applied.
	 * </p>
	 *
	 * @param worksite the worksite to associate. Can't be {@code null}.
	 * @param employee the employee to associate. Can't be {@code null}.
	 *
	 * @throws NullPointerException if any parameter is {@code null}
	 */
	@Transactional
	public void addWorksiteToEmployee(final Worksite worksite, final Employee employee) {
		Objects.requireNonNull(worksite, "worksite can't be null");
		Objects.requireNonNull(employee, "employee can't be null");

		logger.debug("Adding worksite {} to employee {}", worksite, employee);

		final boolean added = employee.assignToWorksite(worksite);
		if (added) {
			this.employeeRepository.save(employee);
		}
	}

	/**
	 * Removes the association between the given {@link Worksite} and
	 * {@link Employee}.
	 *
	 * @param worksite the worksite to remove. Can't be {@code null}.
	 * @param employee the employee. Can't be {@code null}.
	 *
	 * @throws NullPointerException if any parameter is {@code null}
	 */
	@Transactional
	public void removeWorksiteFromEmployee(final Worksite worksite, final Employee employee) {
		Objects.requireNonNull(worksite, "worksite can't be null");
		Objects.requireNonNull(employee, "employee can't be null");

		logger.debug("Removing worksite {} from employee {}", worksite, employee);

		final boolean removed = employee.removeFromWorksite(worksite);
		if (removed) {
			this.employeeRepository.save(employee);
		}
	}

	/**
	 * Deletes the given {@link Employee}.
	 *
	 * <p>
	 * After deletion, the employee will no longer be available in the system.
	 * </p>
	 *
	 * @param employee the employee to delete. Can't be {@code null}.
	 *
	 * @throws NullPointerException if {@code employee} is {@code null}
	 */
	@Transactional
	public void deleteEmployee(final Employee employee) {
		Objects.requireNonNull(employee, "employee cannot be null.");
		logger.debug("Deleting employee {}", employee);

		this.employeeRepository.delete(employee);
	}

	/**
	 * Retrieves an {@link Employee} by email or fails if it does not exist.
	 *
	 * @param email the employee email. Can't be {@code null} or blank.
	 * @return the corresponding {@link Employee}
	 *
	 * @throws ResourceNotFoundException if no employee exists with the given email
	 */
	private Employee findEmployee(final String email) {
		final Employee employee = this.employeeRepository.findByEmail(email);
		if (employee == null) {
			logger.warn("No employee found with email {}", email);
			throw new ResourceNotFoundException("There is no employee with email " + email);
		}
		return employee;
	}
}
