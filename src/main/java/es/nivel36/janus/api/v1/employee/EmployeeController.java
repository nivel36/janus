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
package es.nivel36.janus.api.v1.employee;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.nivel36.janus.api.Mapper;
import es.nivel36.janus.service.employee.Employee;
import es.nivel36.janus.service.employee.EmployeeService;
import es.nivel36.janus.service.worksite.Worksite;
import es.nivel36.janus.service.worksite.WorksiteService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * REST controller exposing CRUD operations and ancillary actions for
 * {@link Employee} entities.
 */
@RestController
@RequestMapping("/api/v1/employees")
public class EmployeeController {

	private static final Logger logger = LoggerFactory.getLogger(EmployeeController.class);

	private final EmployeeService employeeService;
	private final WorksiteService worksiteService;
	private final Mapper<Employee, EmployeeResponse> employeeResponseMapper;

	public EmployeeController(final EmployeeService employeeService, final WorksiteService worksiteService,
			final Mapper<Employee, EmployeeResponse> employeeResponseMapper) {
		this.employeeService = Objects.requireNonNull(employeeService, "EmployeeService can't be null");
		this.worksiteService = Objects.requireNonNull(worksiteService, "WorksiteService can't be null");
		this.employeeResponseMapper = Objects.requireNonNull(employeeResponseMapper,
				"EmployeeResponseMapper can't be null");
	}

	/**
	 * Retrieves an {@link Employee} by its email address.
	 *
	 * @param employeeEmail the unique email address of the employee; must not be
	 *                      {@code null}
	 * @return the {@link EmployeeResponse} matching the email
	 */
	@GetMapping("/by-email/{employeeEmail}")
	public ResponseEntity<EmployeeResponse> findEmployeeByEmail(
			final @PathVariable("employeeEmail") @Email @Size(max = 254) String employeeEmail) {
		Objects.requireNonNull(employeeEmail, "EmployeeEmail can't be null");
		logger.debug("Find employee by email ACTION performed");

		final Employee employee = this.employeeService.findEmployeeByEmail(employeeEmail);
		final EmployeeResponse response = this.employeeResponseMapper.map(employee);
		return ResponseEntity.ok(response);
	}

	/**
	 * Creates a new {@link Employee} using the provided payload.
	 *
	 * @param request the data describing the employee to create; must not be
	 *                {@code null}
	 * @return the created {@link EmployeeResponse}
	 */
	@PostMapping
	public ResponseEntity<EmployeeResponse> createEmployee(@Valid @RequestBody final CreateEmployeeRequest request) {
		Objects.requireNonNull(request, "CreateEmployeeRequest can't be null");
		logger.debug("Create employee ACTION performed");

		final Employee employee = this.buildEmployee(request);
		final Employee createdEmployee = this.employeeService.createEmployee(employee);
		final EmployeeResponse response = this.employeeResponseMapper.map(createdEmployee);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	/**
	 * Updates an existing {@link Employee} identified by its identifier.
	 *
	 * @param employeeEmail the email of the employee to update; must not be
	 *                      {@code null}
	 * @param request       the payload containing the new employee data; must not
	 *                      be {@code null}
	 * @return the updated {@link EmployeeResponse}
	 */
	@PutMapping("/{employeeId}")
	public ResponseEntity<EmployeeResponse> updateEmployee(
			final @PathVariable("employeeEmail") @Email @Size(max = 254) String employeeEmail,
			@Valid @RequestBody final UpdateEmployeeRequest request) {
		Objects.requireNonNull(employeeEmail, "EmployeeEmail can't be null");
		Objects.requireNonNull(request, "UpdateEmployeeRequest can't be null");
		logger.debug("Update employee ACTION performed");

		final Employee existingEmployee = this.employeeService.findEmployeeByEmail(employeeEmail);
		this.merge(existingEmployee, request);

		final Employee updatedEmployee = this.employeeService.updateEmployee(employeeEmail, existingEmployee);
		final EmployeeResponse response = this.employeeResponseMapper.map(updatedEmployee);
		return ResponseEntity.ok(response);
	}

	/**
	 * Adds a {@link Worksite} to an {@link Employee}.
	 *
	 * @param employeeEmail the email of the employee; must not be {@code null}
	 * @param worksiteCode  the worksite business code; must not be {@code null}
	 * @return the updated {@link EmployeeResponse}
	 */
	@PostMapping("/{employeeId}/worksites/{worksiteCode}")
	public ResponseEntity<EmployeeResponse> addWorksiteToEmployee(
			final @PathVariable("employeeEmail") @Email @Size(max = 254) String employeeEmail,
			final @PathVariable("worksiteCode") @Pattern(regexp = "[A-Zea-z0-9_-]{1,50}") String worksiteCode) {
		Objects.requireNonNull(employeeEmail, "EmployeeEmail can't be null");
		Objects.requireNonNull(worksiteCode, "WorksiteCode can't be null");
		logger.debug("Add worksite to employee ACTION performed");

		final Employee employee = this.employeeService.findEmployeeByEmail(employeeEmail);
		final Worksite worksite = this.worksiteService.findWorksiteByCode(worksiteCode);
		this.employeeService.addWorksiteToEmployee(worksite, employee);

		final EmployeeResponse response = this.employeeResponseMapper.map(employee);
		return ResponseEntity.ok(response);
	}

	/**
	 * Removes a {@link Worksite} from an {@link Employee}.
	 *
	 * @param employeeEmail the email of the employee; must not be {@code null}
	 * @param worksiteCode  the worksite business code; must not be {@code null}
	 * @return the updated {@link EmployeeResponse}
	 */
	@DeleteMapping("/{employeeId}/worksites/{worksiteCode}")
	public ResponseEntity<EmployeeResponse> removeWorksiteFromEmployee(
			final @PathVariable("employeeEmail") @Email @Size(max = 254) String employeeEmail,
			final @PathVariable("worksiteCode") @Pattern(regexp = "[A-Za-z0-9_-]{1,50}") String worksiteCode) {
		Objects.requireNonNull(employeeEmail, "EmployeeEmail can't be null");
		Objects.requireNonNull(worksiteCode, "WorksiteCode can't be null");
		logger.debug("Remove worksite from employee ACTION performed");

		final Employee employee = this.employeeService.findEmployeeByEmail(employeeEmail);
		final Worksite worksite = this.worksiteService.findWorksiteByCode(worksiteCode);
		this.employeeService.removeWorksiteFromEmployee(worksite, employee);

		final EmployeeResponse response = this.employeeResponseMapper.map(employee);
		return ResponseEntity.ok(response);
	}

	/**
	 * Deletes an existing {@link Employee}.
	 *
	 * @param employeeEmail the email of the employee; must not be {@code null}
	 * @return an empty response with status {@link HttpStatus#NO_CONTENT}
	 */
	@DeleteMapping("/{employeeId}")
	public ResponseEntity<Void> deleteEmployee(
			final @PathVariable("employeeEmail") @Email @Size(max = 254) String employeeEmail) {
		Objects.requireNonNull(employeeEmail, "EmployeeEmail can't be null");
		logger.debug("Delete employee ACTION performed");

		final Employee employee = this.employeeService.findEmployeeByEmail(employeeEmail);
		this.employeeService.deleteEmployee(employee);
		return ResponseEntity.noContent().build();
	}

	private Employee buildEmployee(final CreateEmployeeRequest request) {
		final Employee employee = new Employee();
		employee.setName(request.name());
		employee.setSurname(request.surname());
		employee.setEmail(request.email());
		return employee;
	}

	private void merge(final Employee employee, final UpdateEmployeeRequest request) {
		employee.setName(request.name());
		employee.setSurname(request.surname());
		employee.setEmail(request.email());
	}
}
