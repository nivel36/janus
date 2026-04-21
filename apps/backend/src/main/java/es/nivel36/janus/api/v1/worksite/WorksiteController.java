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
package es.nivel36.janus.api.v1.worksite;

import java.time.ZoneId;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.bind.annotation.RestController;

import es.nivel36.janus.api.Mapper;
import es.nivel36.janus.api.v1.employee.EmployeeResponse;
import es.nivel36.janus.service.applicationsettings.ApplicationSettingsService;
import es.nivel36.janus.service.employee.Employee;
import es.nivel36.janus.service.employee.EmployeeService;
import es.nivel36.janus.service.worksite.Worksite;
import es.nivel36.janus.service.worksite.WorksiteScope;
import es.nivel36.janus.service.worksite.WorksiteService;
import es.nivel36.janus.util.Roles;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;

/**
 * REST controller responsible for exposing worksite operations.
 */
@RestController
@RequestMapping("/api/v1/worksites")
public class WorksiteController {

	private static final Logger logger = LoggerFactory.getLogger(WorksiteController.class);

	private final WorksiteService worksiteService;
	private final EmployeeService employeeService;
	private final ApplicationSettingsService applicationSettingsService;
	private final Mapper<Worksite, WorksiteResponse> worksiteResponseMapper;

	/**
	 * Builds a controller for managing {@link Worksite} resources.
	 *
	 * @param worksiteService        application service that provides worksite
	 *                               operations; must not be {@code null}
	 * @param worksiteResponseMapper mapper translating {@link Worksite} entities
	 *                               into {@link WorksiteResponse} DTOs; must not be
	 *                               {@code null}
	 */
	public WorksiteController(final WorksiteService worksiteService,
			final ApplicationSettingsService applicationSettingsService, final EmployeeService employeeService,
			final Mapper<Worksite, WorksiteResponse> worksiteResponseMapper) {
		this.worksiteService = //
				Objects.requireNonNull(worksiteService, "WorksiteService can't be null");
		this.applicationSettingsService = //
				Objects.requireNonNull(applicationSettingsService, "applicationSettingsService can't be null");
		this.employeeService = //
				Objects.requireNonNull(employeeService, "EmployeeService can't be null");
		this.worksiteResponseMapper = //
				Objects.requireNonNull(worksiteResponseMapper, "WorksiteResponseMapper can't be null");
	}

	/**
	 * Retrieves all worksites registered in the system.
	 *
	 * @return a {@link ResponseEntity} containing the list of worksites
	 */
	@GetMapping
	@PreAuthorize("hasAnyRole('JANUS_EMPLOYEE', 'JANUS_USER', 'JANUS_ADMIN')")
	public ResponseEntity<Page<WorksiteResponse>> searchWorksites(
			final @RequestParam(required = false) @Pattern(regexp = "[A-Za-z0-9_-]{1,50}", message = "query must contain only letters, digits, underscores or hyphens (max 50)") String query,
			final @RequestParam(required = false) @Pattern(regexp = "^(?=.{1,254}$)[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "employeeEmail must be a valid and safe email address (max 254)") String employeeEmail,
			final Pageable pageable, final Authentication authentication) {
		logger.debug("Search worksites ACTION performed");
		final String authenticatedEmail = authentication.getName();
		final boolean hasOnlyEmployeeRole = Roles.hasOnlyEmployeeRole(authentication.getAuthorities());
		final String effectiveEmployeeEmail;
		if (hasOnlyEmployeeRole) {
			if (authenticatedEmail == null || authenticatedEmail.isBlank()) {
				throw new AccessDeniedException("Employee email claim is required");
			}
			if (employeeEmail == null) {
				throw new AccessDeniedException("Employees can only search worksites for themselves");
			}
			if (employeeEmail != null && !employeeEmail.equalsIgnoreCase(authenticatedEmail)) {
				throw new AccessDeniedException("Employees can only search worksites for themselves");
			}
			effectiveEmployeeEmail = authenticatedEmail;
		} else {
			effectiveEmployeeEmail = employeeEmail;
		}

		final Page<WorksiteResponse> worksites = this.worksiteService
				.searchWorksites(query, effectiveEmployeeEmail, pageable).map(this.worksiteResponseMapper::map);
		return ResponseEntity.ok(worksites);
	}

	/**
	 * Retrieves a specific worksite by its unique code.
	 *
	 * @param worksiteCode the unique code of the worksite; must not be {@code null}
	 * @return a {@link ResponseEntity} containing the requested worksite
	 */
	@GetMapping("/{worksiteCode}")
	@PreAuthorize("hasAnyRole('JANUS_EMPLOYEE', 'JANUS_USER', 'JANUS_ADMIN')")
	public ResponseEntity<WorksiteResponse> findWorksite(
			final @PathVariable("worksiteCode") @Pattern(regexp = "[A-Za-z0-9_-]{1,50}", message = "code must contain only letters, digits, underscores or hyphens (max 50)") String worksiteCode) {
		logger.debug("Find worksite ACTION performed");

		final Worksite worksite = this.worksiteService.findWorksiteByCode(worksiteCode);
		final WorksiteResponse response = this.worksiteResponseMapper.map(worksite);
		return ResponseEntity.ok(response);
	}

	/**
	 * Creates a new worksite.
	 *
	 * <p>
	 * The request may describe either a global worksite or a personal one. For
	 * personal worksites, the owner employee id is forwarded to the service layer
	 * so it can resolve and validate the owner relation.
	 * </p>
	 *
	 * @param request the payload describing the worksite to create; must not be
	 *                {@code null}
	 * @return a {@link ResponseEntity} containing the created worksite
	 */
	@PostMapping
	@PreAuthorize("hasAnyRole('JANUS_EMPLOYEE', 'JANUS_USER', 'JANUS_ADMIN')")
	public ResponseEntity<WorksiteResponse> createWorksite(@Valid @RequestBody final CreateWorksiteRequest request,
			final Authentication authentication) {
		logger.debug("Create worksite ACTION performed");

		final boolean hasOnlyEmployeeRole = Roles.hasOnlyEmployeeRole(authentication.getAuthorities());

		if (hasOnlyEmployeeRole) {
			if (!this.applicationSettingsService.isEmployeeWorkplaceCreationAllowed()) {
				throw new AccessDeniedException("Employee workplace creation is disabled");
			}
			if (request.scope() != WorksiteScope.ASSIGNED) {
				throw new AccessDeniedException("Employees can only create assigned worksites");
			}
		}

		final String code = request.code();
		final String name = request.name();
		final ZoneId zoneId = ZoneId.of(request.timeZone());
		final WorksiteScope scope = request.scope();
		final Worksite worksite = this.worksiteService.createWorksite(code, name, zoneId, scope);

		final WorksiteResponse response = this.worksiteResponseMapper.map(worksite);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	/**
	 * Updates an existing worksite identified by its code.
	 *
	 * @param worksiteCode the code of the worksite to update; must not be
	 *                     {@code null}
	 * @param request      the payload describing the new worksite data; must not be
	 *                     {@code null}
	 * @return a {@link ResponseEntity} containing the updated worksite
	 */
	@PreAuthorize("hasAnyRole('JANUS_EMPLOYEE', 'JANUS_USER', 'JANUS_ADMIN')")
	@PutMapping("/{worksiteCode}")
	public ResponseEntity<WorksiteResponse> updateWorksite(
			@PathVariable("worksiteCode") @Pattern(regexp = "[A-Za-z0-9_-]{1,50}", message = "code must contain only letters, digits, underscores or hyphens (max 50)") final String worksiteCode,
			@Valid @RequestBody final UpdateWorksiteRequest request, //
			final Authentication authentication) {
		logger.debug("Update worksite ACTION performed");

		final String authenticatedEmail = authentication.getName();
		final boolean hasOnlyEmployeeRole = Roles.hasOnlyEmployeeRole(authentication.getAuthorities());
		if (hasOnlyEmployeeRole) {
			if (request.scope() != WorksiteScope.ASSIGNED) {
				throw new AccessDeniedException("Employees can only update assigned worksites");
			}

			if (!this.applicationSettingsService.isEmployeeWorkplaceCreationAllowed()) {
				throw new AccessDeniedException("Employee workplace creation is disabled");
			}

			if (!this.employeeService.isAssignedToWorksite(worksiteCode, authenticatedEmail)) {
				throw new AccessDeniedException("Employees can only update their personal worksites");
			}
		}
		final String name = request.name();
		final ZoneId zoneId = ZoneId.of(request.timeZone());
		final WorksiteScope scope = request.scope();
		final Worksite worksite = this.worksiteService.updateWorksite(worksiteCode, name, zoneId, scope);

		final WorksiteResponse response = this.worksiteResponseMapper.map(worksite);
		return ResponseEntity.ok(response);
	}

	/**
	 * Deletes the worksite identified by the given code.
	 *
	 * @param worksiteCode the unique code of the worksite; must not be {@code null}
	 * @return a {@link ResponseEntity} with an empty body and HTTP 204 status
	 */
	@PreAuthorize("hasAnyRole('JANUS_USER', 'JANUS_ADMIN')")
	@DeleteMapping("/{worksiteCode}")
	public ResponseEntity<Void> deleteWorksite(
			final @PathVariable("worksiteCode") @Pattern(regexp = "[A-Za-z0-9_-]{1,50}", message = "code must contain only letters, digits, underscores or hyphens (max 50)") String worksiteCode) {
		logger.debug("Delete worksite ACTION performed");

		final Worksite workiste = this.worksiteService.findWorksiteByCode(worksiteCode);
		this.worksiteService.deleteWorksite(workiste);
		return ResponseEntity.noContent().build();
	}

	/**
	 * Adds a {@link Worksite} to an {@link Employee}.
	 *
	 * @param worksiteCode  the worksite business code; must not be {@code null}
	 * @param employeeEmail the email of the employee; must not be {@code null}
	 * 
	 * @return the updated {@link EmployeeResponse}
	 */
	@PreAuthorize("hasAnyRole('JANUS_USER', 'JANUS_ADMIN')")
	@PutMapping("/{worksiteCode}/employees/{employeeEmail}")
	public ResponseEntity<Void> assignEmployeeToWorksite(
			final @PathVariable("worksiteCode") @Pattern(regexp = "[A-Za-z0-9_-]{1,50}", message = "code must contain only letters, digits, underscores or hyphens (max 50)") String worksiteCode,
			final @PathVariable("employeeEmail") @Pattern(regexp = "^(?=.{1,254}$)[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "must be a valid and safe email address (max 254)") String employeeEmail) {
		logger.debug("Add worksite to employee ACTION performed");

		final Employee employee = this.employeeService.findEmployeeByEmail(employeeEmail);
		final Worksite worksite = this.worksiteService.findWorksiteByCode(worksiteCode);

		if (worksite.getScope() != WorksiteScope.ASSIGNED) {
			this.worksiteService.assertEmployeeCanUseWorksite(employeeEmail, worksite);
		}

		this.worksiteService.addEmployeeToWorksite(worksite, employee);
		return ResponseEntity.noContent().build();
	}

	/**
	 * Removes a {@link Worksite} from an {@link Employee}.
	 *
	 * @param employeeEmail the email of the employee; must not be {@code null}
	 * @param worksiteCode  the worksite business code; must not be {@code null}
	 * @return the updated {@link EmployeeResponse}
	 */
	@PreAuthorize("hasAnyRole('JANUS_USER', 'JANUS_ADMIN')")
	@DeleteMapping("/{worksiteCode}/employees/{employeeEmail}")
	public ResponseEntity<EmployeeResponse> removeEmployeeFromWorksite( //
			final @PathVariable("worksiteCode") //
			@Pattern( //
					regexp = "[A-Za-z0-9_-]{1,50}", //
					message = "code must contain only letters, digits, underscores or hyphens (max 50)" //
			) //
			String worksiteCode, final @PathVariable("employeeEmail") //
			@Pattern( //
					regexp = "^(?=.{1,254}$)[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", //
					message = "must be a valid and safe email address (max 254)" //
			) //
			String employeeEmail) {
		logger.debug("Remove worksite from employee ACTION performed");

		final Employee employee = this.employeeService.findEmployeeByEmail(employeeEmail);
		final Worksite worksite = this.worksiteService.findWorksiteByCode(worksiteCode);
		this.worksiteService.removeEmployeeFromWorksite(worksite, employee);

		return ResponseEntity.noContent().build();
	}
}
