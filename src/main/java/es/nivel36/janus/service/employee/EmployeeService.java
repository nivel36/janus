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

import es.nivel36.janus.service.ResourceNotFoundException;

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
	public Employee findEmployeeById(final Long id) {
		Objects.requireNonNull(id, "Id can't be null");
		logger.debug("Finding Employee by id: {}", id);
		return this.employeeRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("There is no employee with id " + id));
	}

	/**
	 * Finds an {@link Employee} by email.
	 * 
	 * @param email the email of the employee to find
	 * @return the employee with the specified email, or null if no employee is
	 *         found
	 * @throws NullPointerException if the email is null
	 */
	public Employee findEmployeeByEmail(final String email) {
		Objects.requireNonNull(email, "Email cannot be null.");
		logger.debug("Finding Employee by email: {}", email);
		return this.employeeRepository.findEmployeeByEmail(email);
	}

	/**
	 * Finds the identifiers of employees who have at least one {@link TimeLog}
	 * since the specified instant but do not have any associated {@link WorkShift}.
	 *
	 * @param from the lower bound (inclusive) instant; only time logs with an
	 *             {@code entryTime} greater than or equal to this value are
	 *             considered
	 * @return a list of employee IDs corresponding to employees with time logs
	 *         since the given instant but without any linked work shifts
	 * @throws NullPointerException if {@code from} is {@code null}
	 */
	public List<Long> findEmployeesWithoutWorkshifts(final Instant from) {
		Objects.requireNonNull(from, "From must not be null");
		logger.debug("Finding employees without workshift from date: {}", from);
		return this.employeeRepository.findEmployeesWithoutWorkshifts(from);
	}

	/**
	 * Creates a new {@link Employee}.
	 * 
	 * @param employee the employee to be created
	 * @return the created employee
	 * @throws NullPointerException if the employee is null
	 */
	public Employee createEmployee(final Employee employee) {
		Objects.requireNonNull(employee, "Employee cannot be null.");
		logger.debug("Creating new Employee: {}", employee);
		return this.employeeRepository.save(employee);
	}

	/**
	 * Updates an existing {@link Employee}.
	 * 
	 * @param employee the employee to be updated
	 * @return the updated employee
	 * @throws NullPointerException if the employee is null
	 */
	public Employee updateEmployee(final Employee employee) {
		Objects.requireNonNull(employee, "Employee cannot be null.");
		logger.debug("Updating Employee: {}", employee);
		return this.employeeRepository.save(employee);
	}

	/**
	 * Deletes an existing {@link Employee}.
	 * 
	 * @param employee the employee to be deleted
	 * @throws NullPointerException if the employee is null
	 */
	public void deleteEmployee(final Employee employee) {
		Objects.requireNonNull(employee, "Employee cannot be null.");
		logger.debug("Deleting Employee: {}", employee);
		this.employeeRepository.delete(employee);
	}
}
