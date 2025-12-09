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

/**
 * Service class responsible for managing {@link Employee} entities and
 * interacting with the {@link EmployeeRepository}.
 */
@Service
public class EmployeeService {

	private static final Logger logger = LoggerFactory.getLogger(EmployeeService.class);

	private final EmployeeRepository employeeRepository;

	public EmployeeService(final EmployeeRepository employeeRepository) {
		this.employeeRepository = Objects.requireNonNull(employeeRepository, "EmployeeRepository cannot be null.");
	}

	/**
	 * Finds an {@link Employee} by its primary key (Id).
	 * 
	 * @param id the ID of the employee to find
	 * @return the employee with the specified Id
	 * @throws IllegalArgumentException  if the Id is negative
	 * @throws ResourceNotFoundException if the employee is not found
	 */
	@Transactional(readOnly = true)
	public Employee findEmployeeById(final Long id) {
		Objects.requireNonNull(id, "id can't be null");
		logger.debug("Finding Employee by id {}", id);
		return this.employeeRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("There is no employee with id " + id));
	}

	/**
	 * Finds an {@link Employee} by email.
	 * 
	 * @param email the email of the employee to find
	 * @return the employee with the specified email, or null if no employee is
	 *         found
	 * @throws NullPointerException      if the email is null
	 * @throws ResourceNotFoundException if the employee is not found
	 */
	@Transactional(readOnly = true)
	public Employee findEmployeeByEmail(final String email) {
		Objects.requireNonNull(email, "email cannot be null.");
		logger.debug("Finding Employee by email {}", email);

		return this.findEmployee(email);
	}

	private Employee findEmployee(final String email) {
		final Employee employee = this.employeeRepository.findByEmail(email);
		if (employee == null) {
			logger.warn("No employee found with email {}", email);
			throw new ResourceNotFoundException("There is no employee with email " + email);
		}
		return employee;
	}

	/**
	 * Finds the identifiers of employees who have at least one {@link TimeLog}
	 * since the specified instant but do not have any associated {@link WorkShift}.
	 *
	 * @param fromInclusive the lower bound instant; only time logs with an
	 *                      {@code entryTime} greater than or equal to this value
	 *                      are considered
	 * @return a list of employee IDs corresponding to employees with time logs
	 *         since the given instant but without any linked work shifts
	 * @throws NullPointerException if {@code from} is {@code null}
	 */
	@Transactional(readOnly = true)
	public List<Long> findEmployeesWithoutWorkshiftsSince(final Instant fromInclusive) {
		Objects.requireNonNull(fromInclusive, "From must not be null");
		logger.debug("Finding employees without workshift from date: {}", fromInclusive);

		final List<Long> employeesWithoutWorkshift = this.employeeRepository.findWithoutWorkshiftsSince(fromInclusive);
		logger.trace("Found {} employees without workshift", employeesWithoutWorkshift.size());
		return employeesWithoutWorkshift;
	}

	/**
	 * Creates a new {@link Employee}.
	 * 
	 * @param employee the employee to be created
	 * @return the created employee
	 * @throws NullPointerException           if the employee is {@code null}.
	 * @throws ResourceAlreadyExistsException if the employee is changing the email
	 *                                        and the new email already exists.
	 */
	@Transactional
	public Employee createEmployee(final String name, final String surname, final String email,
			final Schedule schedule) {
		Objects.requireNonNull(name, "name cannot be null.");
		Objects.requireNonNull(surname, "surname cannot be null.");
		Objects.requireNonNull(email, "email cannot be null.");
		Objects.requireNonNull(schedule, "schedule cannot be null.");

		logger.debug("Creating new employee {}", email);
		final boolean emailInUse = this.employeeRepository.existsByEmail(email);
		if (emailInUse) {
			logger.warn("Employee with email {} already exists", email);
			throw new ResourceAlreadyExistsException("Employee with email " + email + " already exists");
		}
		final Employee employee = new Employee(name.trim(), surname.trim(), email.trim(), schedule);
		final Employee savedEmployee = this.employeeRepository.save(employee);
		logger.trace("Employee {} created successfully", savedEmployee);
		return savedEmployee;
	}

	/**
	 * Updates an existing {@link Employee} identified by its email.
	 *
	 * <p>
	 * Validates and normalizes inputs (trimming and basic checks). Replaces the
	 * current name, surname and schedule atomically.
	 * </p>
	 *
	 * @param email       unique email that identifies the employee; must be
	 *                    non-blank
	 * @param newName     new first name; must be non-blank
	 * @param newSurname  new surname; must be non-blank
	 * @param newSchedule new schedule; must be non-null
	 * @return the updated employee (managed instance)
	 *
	 * @throws IllegalArgumentException  if any string parameter is blank or email
	 *                                   is invalid
	 * @throws ResourceNotFoundException if no employee exists with the given email
	 */
	@Transactional
	public Employee updateEmployee(final String email, final String newName, final String newSurname,
			final Schedule newSchedule) {
		Objects.requireNonNull(email, "email cannot be null.");
		Objects.requireNonNull(newName, "newName cannot be null.");
		Objects.requireNonNull(newSurname, "newSurname cannot be null.");
		Objects.requireNonNull(newSchedule, "newSchedule cannot be null.");
		logger.debug("Updating employee {}", email);

		final Employee employee = this.findEmployeeByEmail(email);
		employee.setName(newName.trim());
		employee.setSurname(newSurname.trim());
		employee.setSchedule(newSchedule);
		final Employee savedEmployee = this.employeeRepository.save(employee);
		logger.trace("Employee {} updated successfully", savedEmployee);
		return savedEmployee;
	}

	/**
	 * Adds the {@link Worksite} to the {@link Employee}. This method is idempotent:
	 * does not duplicate the relationship if it already exists.
	 *
	 * @param worksite the worksite to add; must not be {@code null}
	 * @param employee the employee; must not be {@code null}
	 * @throws NullPointerException if any parameter is {@code null}
	 */
	@Transactional
	public void addWorksiteToEmployee(final Worksite worksite, final Employee employee) {
		Objects.requireNonNull(worksite, "worksite can't be null");
		Objects.requireNonNull(employee, "employee can't be null");

		logger.debug("Adding worksite {} to employee {}", worksite, employee);
		boolean added = employee.getWorksites().add(worksite);
		if (added) {
			worksite.getEmployees().add(employee);
			this.employeeRepository.save(employee);
			logger.trace("Worksite {} added to employee {}", worksite, employee);
		} else {
			logger.trace("Relation already existed: worksite {} ⇢ employee {}", worksite, employee);
		}
	}

	/**
	 * Removes the {@link Worksite} to the {@link Employee}.
	 *
	 * @param worksite the worksite to remove; must not be {@code null}
	 * @param employee the employee; must not be {@code null}
	 * @throws NullPointerException if any parameter is {@code null}
	 */
	@Transactional
	public void removeWorksiteFromEmployee(final Worksite worksite, final Employee employee) {
		Objects.requireNonNull(worksite, "worksite can't be null");
		Objects.requireNonNull(employee, "employee can't be null");

		logger.debug("Removing worksite {} from employee {}", worksite, employee);

		boolean removed = employee.getWorksites().remove(worksite);
		if (removed) {
			worksite.getEmployees().remove(employee);
			this.employeeRepository.save(employee);
			logger.trace("Worksite {} removed from employee {}", worksite, employee);
		} else {
			logger.trace("Relation did not exist: worksite {} ⇢ employee {}", worksite, employee);
		}
	}

	/**
	 * Deletes an existing {@link Employee}.
	 * 
	 * @param employee the employee to be deleted
	 * @throws NullPointerException if the employee is null
	 */
	@Transactional
	public void deleteEmployee(final Employee employee) {
		Objects.requireNonNull(employee, "employee cannot be null.");
		logger.debug("Deleting employee {}", employee);

		this.employeeRepository.delete(employee);
		logger.trace("Employee {} deleted successfully", employee);
	}
}
