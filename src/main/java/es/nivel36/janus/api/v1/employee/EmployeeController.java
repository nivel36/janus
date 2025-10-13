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
import es.nivel36.janus.service.schedule.Schedule;
import es.nivel36.janus.service.schedule.ScheduleService;
import es.nivel36.janus.service.worksite.Worksite;
import es.nivel36.janus.service.worksite.WorksiteService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;

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
	private final ScheduleService scheduleService;
	private final Mapper<Employee, EmployeeResponse> employeeResponseMapper;

        /**
         * Creates a controller that exposes employee management endpoints.
         *
         * @param employeeService          service handling {@link Employee} domain
         *                                 operations; must not be {@code null}
         * @param worksiteService          service managing {@link Worksite}
         *                                 associations; must not be {@code null}
         * @param scheduleService          service retrieving {@link Schedule}
         *                                 information; must not be {@code null}
         * @param employeeResponseMapper   mapper converting {@link Employee} entities to
         *                                 {@link EmployeeResponse} DTOs; must not be
         *                                 {@code null}
         */
        public EmployeeController(final EmployeeService employeeService, final WorksiteService worksiteService,
                        final ScheduleService scheduleService, final Mapper<Employee, EmployeeResponse> employeeResponseMapper) {
                this.employeeService = Objects.requireNonNull(employeeService, "employeeService can't be null");
                this.worksiteService = Objects.requireNonNull(worksiteService, "worksiteService can't be null");
                this.scheduleService = Objects.requireNonNull(scheduleService, "scheduleService can't be null");
                this.employeeResponseMapper = Objects.requireNonNull(employeeResponseMapper,
                                "employeeResponseMapper can't be null");
	}

	/**
	 * Retrieves an {@link Employee} by its email address.
	 *
	 * @param employeeEmail the unique email address of the employee; must not be
	 *                      {@code null}
	 * @return the {@link EmployeeResponse} matching the email
	 */
	@GetMapping("/by-email/{employeeEmail}")
	public ResponseEntity<EmployeeResponse> findEmployeeByEmail( //
			final @PathVariable("employeeEmail") //
			@Pattern( //
					regexp = "^(?=.{1,254}$)[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", //
					message = "must be a valid and safe email address (max 254)" //
			) //
			String employeeEmail) {
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
		logger.debug("Create employee ACTION performed");

		final Schedule schedule = this.scheduleService.findScheduleById(request.scheduleId());
		final Employee createdEmployee = this.employeeService.createEmployee(request.name(), request.surname(),
				request.email(), schedule);
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
	public ResponseEntity<EmployeeResponse> updateEmployee(//
			final @PathVariable("employeeEmail") //
			@Pattern( //
					regexp = "^(?=.{1,254}$)[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", //
					message = "must be a valid and safe email address (max 254)" //
			) //
			String employeeEmail, //
			@Valid @RequestBody final UpdateEmployeeRequest request) {
		logger.debug("Update employee ACTION performed");

		final Schedule schedule = this.scheduleService.findScheduleById(request.scheduleId());
		final Employee updatedEmployee = this.employeeService.updateEmployee(employeeEmail, request.name(),
				request.surname(), request.email(), schedule);
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
	public ResponseEntity<EmployeeResponse> addWorksiteToEmployee( //
			final @PathVariable("employeeEmail") //
			@Pattern( //
					regexp = "^(?=.{1,254}$)[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", //
					message = "must be a valid and safe email address (max 254)" //
			) //
			String employeeEmail, final @PathVariable("worksiteCode") //
			@Pattern( //
					regexp = "[A-Za-z0-9_-]{1,50}", //
					message = "code must contain only letters, digits, underscores or hyphens (max 50)" //
			) //
			String worksiteCode) {
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
	public ResponseEntity<EmployeeResponse> removeWorksiteFromEmployee( //
			final @PathVariable("employeeEmail") //
			@Pattern( //
					regexp = "^(?=.{1,254}$)[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", //
					message = "must be a valid and safe email address (max 254)" //
			) //
			String employeeEmail, final @PathVariable("worksiteCode") //
			@Pattern( //
					regexp = "[A-Za-z0-9_-]{1,50}", //
					message = "code must contain only letters, digits, underscores or hyphens (max 50)" //
			) //
			String worksiteCode) {
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
	public ResponseEntity<Void> deleteEmployee(//
			final @PathVariable("employeeEmail") //
			@Pattern( //
					regexp = "^(?=.{1,254}$)[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", //
					message = "must be a valid and safe email address (max 254)" //
			) //
			String employeeEmail) {
		logger.debug("Delete employee ACTION performed");

		final Employee employee = this.employeeService.findEmployeeByEmail(employeeEmail);
		this.employeeService.deleteEmployee(employee);
		return ResponseEntity.noContent().build();
	}
}
