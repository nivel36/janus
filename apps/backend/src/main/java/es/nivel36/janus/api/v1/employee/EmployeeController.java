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

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
 * Spring MVC implementation of {@link EmployeeResource}.
 */
@RestController
public class EmployeeController implements EmployeeResource {

	private static final Logger logger = LoggerFactory.getLogger(EmployeeController.class);

	private final EmployeeService employeeService;
	private final ScheduleService scheduleService;
	private final Mapper<Employee, EmployeeResponse> employeeResponseMapper;

	/**
	 * Creates a controller without accessing persistence. All dependencies must be
	 * nonnull.
	 *
	 * @param  employeeService        service handling {@link Employee} domain
	 *                                operations; must not be {@code null}
	 * @param  scheduleService        service retrieving {@link Schedule}
	 *                                information; must not be {@code null}
	 * @param  employeeResponseMapper mapper converting {@link Employee} entities to
	 *                                {@link EmployeeResponse} DTOs; must not be
	 *                                {@code null}
	 * @throws NullPointerException   if any dependency is {@code null}
	 */
	public EmployeeController(
		final EmployeeService employeeService,
		final ScheduleService scheduleService,
		final @Qualifier("employeeResponseMapper") Mapper<Employee, EmployeeResponse> employeeResponseMapper) {
		this.employeeService = Objects.requireNonNull(employeeService, "employeeService can't be null");
		this.scheduleService = Objects.requireNonNull(scheduleService, "scheduleService can't be null");
		this.employeeResponseMapper = Objects
				.requireNonNull(employeeResponseMapper, "employeeResponseMapper can't be null");
	}

	@Override
	public ResponseEntity<Page<EmployeeResponse>> searchEmployees(
			final String query,
			final String scheduleCode,
			final String worksiteCode,
			final Pageable pageable) {
		logger.debug("Search employees ACTION performed");
		final Page<EmployeeResponse> employees = this.employeeService
				.searchEmployees(query, scheduleCode, worksiteCode, pageable).map(this.employeeResponseMapper::map);
		return ResponseEntity.ok(employees);
	}

	@Override
	public ResponseEntity<EmployeeResponse> findEmployee(final String employeeNumber) {
		logger.debug("Find employee by number ACTION performed");
		final Employee employee = this.employeeService.findEmployeeByEmployeeNumber(employeeNumber);
		final EmployeeResponse response = this.employeeResponseMapper.map(employee);
		return ResponseEntity.ok(response);
	}

	@Override
	public ResponseEntity<EmployeeResponse> createEmployee(final CreateEmployeeRequest request) {
		logger.debug("Create employee ACTION performed");
		final String scheduleCode = request.scheduleCode();
		final Schedule schedule = this.scheduleService.findScheduleByCode(scheduleCode);
		final String name = request.name().trim();
		final String surname = request.surname().trim();
		final String email = EmailAddresses.canonicalize(request.email());
		final String employeeNumber = request.employeeNumber();
		final Employee createdEmployee = this.employeeService
				.createEmployee(employeeNumber, name, surname, email, schedule);
		final EmployeeResponse response = this.employeeResponseMapper.map(createdEmployee);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@Override
	public ResponseEntity<EmployeeResponse> updateEmployee(
			final String employeeNumber,
			final UpdateEmployeeRequest request) {
		logger.debug("Update employee ACTION performed");
		final String email = EmailAddresses.canonicalize(request.email());
		final String name = request.name().trim();
		final String surname = request.surname().trim();
		final String scheduleCode = request.scheduleCode();
		final Employee updatedEmployee = this.employeeService
				.updateEmployee(employeeNumber, name, surname, email, scheduleCode);
		final EmployeeResponse response = this.employeeResponseMapper.map(updatedEmployee);
		return ResponseEntity.ok(response);
	}

	@Override
	public ResponseEntity<Void> deleteEmployee(final String employeeNumber) {
		logger.debug("Delete employee ACTION performed");
		this.employeeService.deleteEmployee(employeeNumber);
		return ResponseEntity.noContent().build();
	}
}
