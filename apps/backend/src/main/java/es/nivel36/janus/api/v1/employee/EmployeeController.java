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
package es.nivel36.janus.api.v1.employee;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import es.nivel36.janus.api.Mapper;
import es.nivel36.janus.service.employee.Employee;
import es.nivel36.janus.service.employee.EmployeeService;
import es.nivel36.janus.service.schedule.Schedule;
import es.nivel36.janus.service.schedule.ScheduleService;
import es.nivel36.janus.util.EmailAddresses;

/**
 * REST controller exposing CRUD operations and ancillary actions for
 * {@link Employee} entities.
 */
@RestController
public class EmployeeController implements EmployeeResource {

	private static final Logger logger = LoggerFactory.getLogger(EmployeeController.class);

	private final EmployeeService employeeService;
	private final ScheduleService scheduleService;
	private final Mapper<Employee, EmployeeResponse> employeeResponseMapper;

	/**
	 * Creates a controller that exposes employee management endpoints.
	 *
	 * @param employeeService        service handling {@link Employee} domain
	 *                               operations; must not be {@code null}
	 * @param scheduleService        service retrieving {@link Schedule}
	 *                               information; must not be {@code null}
	 * @param employeeResponseMapper mapper converting {@link Employee} entities to
	 *                               {@link EmployeeResponse} DTOs; must not be
	 *                               {@code null}
	 */
	public EmployeeController(final EmployeeService employeeService, final ScheduleService scheduleService,
			final @Qualifier("employeeResponseMapper") Mapper<Employee, EmployeeResponse> employeeResponseMapper) {
		this.employeeService = Objects.requireNonNull(employeeService, "employeeService can't be null");
		this.scheduleService = Objects.requireNonNull(scheduleService, "scheduleService can't be null");
		this.employeeResponseMapper = Objects.requireNonNull(employeeResponseMapper,
				"employeeResponseMapper can't be null");
	}

	/**
	 * Retrieves an {@link Employee} by its employee number.
	 *
	 * @param employeeNumber the stable employee number of the employee; must not be
	 *                      {@code null}
	 * @return the {@link EmployeeResponse} matching the employee number
	 */
	@Override
	public ResponseEntity<EmployeeResponse> findEmployee(final String employeeNumber) {
		logger.debug("Find employee by number ACTION performed");
		final Employee employee = this.employeeService.findEmployeeByEmployeeNumber(employeeNumber);
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
	@Override
	public ResponseEntity<EmployeeResponse> createEmployee(final CreateEmployeeRequest request) {
		logger.debug("Create employee ACTION performed");

		final String scheduleCode = request.scheduleCode().trim();
		final Schedule schedule = this.scheduleService.findScheduleByCode(scheduleCode);
		final String name = request.name().trim();
		final String surname = request.surname().trim();
		final String email = EmailAddresses.canonicalize(request.email());
		final String employeeNumber = request.employeeNumber().trim();
		final Employee createdEmployee = this.employeeService.createEmployee(employeeNumber, name, surname, email, schedule);
		final EmployeeResponse response = this.employeeResponseMapper.map(createdEmployee);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	/**
	 * Updates an existing {@link Employee} identified by its identifier.
	 *
	 * @param employeeNumber the stable number of the employee to update; must not be
	 *                      {@code null}
	 * @param request       the payload containing the new employee data; must not
	 *                      be {@code null}
	 * @return the updated {@link EmployeeResponse}
	 */
	@Override
	public ResponseEntity<EmployeeResponse> updateEmployee(//
			final String employeeNumber, //
			final UpdateEmployeeRequest request) {
		logger.debug("Update employee ACTION performed");
		final String email = EmailAddresses.canonicalize(request.email());
		final String name = request.name().trim();
		final String surname = request.surname().trim();
		final String scheduleCode = request.scheduleCode().trim();
		final Employee updatedEmployee = this.employeeService.updateEmployee(employeeNumber, name, surname, email, scheduleCode);
		final EmployeeResponse response = this.employeeResponseMapper.map(updatedEmployee);
		return ResponseEntity.ok(response);
	}

	/**
	 * Deletes an existing {@link Employee}.
	 *
	 * @param employeeNumber the stable number of the employee; must not be {@code null}
	 * @return an empty response with status {@link HttpStatus#NO_CONTENT}
	 */
	@Override
	public ResponseEntity<Void> deleteEmployee(//
			final String employeeNumber) {
		logger.debug("Delete employee ACTION performed");
		final Employee employee = this.employeeService.findEmployeeByEmployeeNumber(employeeNumber);
		this.employeeService.deleteEmployee(employee);
		return ResponseEntity.noContent().build();
	}
}
