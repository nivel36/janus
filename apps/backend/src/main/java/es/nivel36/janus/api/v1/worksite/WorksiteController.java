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

import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;

import es.nivel36.janus.api.Mapper;
import es.nivel36.janus.api.v1.employee.EmployeeResponse;
import es.nivel36.janus.policy.worksite.WorksiteAuthorizationAdapter;
import es.nivel36.janus.service.employee.Employee;
import es.nivel36.janus.service.employee.EmployeeService;
import es.nivel36.janus.service.worksite.Worksite;
import es.nivel36.janus.service.worksite.WorksiteScope;
import es.nivel36.janus.service.worksite.WorksiteService;

/**
 * REST controller responsible for exposing worksite operations.
 */
@RestController
public class WorksiteController implements WorksiteResource {

	private static final Logger logger = LoggerFactory.getLogger(WorksiteController.class);

	private final WorksiteService worksiteService;
	private final EmployeeService employeeService;
	private final WorksiteAuthorizationAdapter authorization;
	private final Mapper<Worksite, WorksiteResponse> worksiteResponseMapper;

	/**
	 * Builds a controller for managing {@link Worksite} resources.
	 *
	 * @param worksiteService        application service that provides worksite
	 *                               operations; must not be {@code null}
	 * @param employeeService        service used to resolve employees and calculate
	 *                               worksite statistics; must not be {@code null}
	 * @param authorization          component that determines the authenticated
	 *                               user's effective employee scope; must not be
	 *                               {@code null}
	 * @param worksiteResponseMapper mapper translating {@link Worksite} entities
	 *                               into {@link WorksiteResponse} DTOs; must not be
	 *                               {@code null}
	 */
	public WorksiteController(final WorksiteService worksiteService, final EmployeeService employeeService,
			final WorksiteAuthorizationAdapter authorization,
			final @Qualifier("worksiteResponseMapper") Mapper<Worksite, WorksiteResponse> worksiteResponseMapper) {
		this.worksiteService = //
				Objects.requireNonNull(worksiteService, "WorksiteService can't be null");
		this.employeeService = //
				Objects.requireNonNull(employeeService, "EmployeeService can't be null");
		this.authorization = Objects.requireNonNull(authorization, "authorization can't be null");
		this.worksiteResponseMapper = //
				Objects.requireNonNull(worksiteResponseMapper, "WorksiteResponseMapper can't be null");
	}

	/**
	 * Retrieves all worksites registered in the system.
	 *
	 * @param query          optional worksite search query
	 * @param employeeEmail  optional employee email filter
	 * @param pageable       pagination and sorting information; must not be
	 *                       {@code null}
	 * @param authentication current authentication; must not be {@code null}
	 * @return a {@link ResponseEntity} containing the list of worksites
	 */
	@Override
	public ResponseEntity<Page<WorksiteResponse>> searchWorksites( //
			final String query, //
			final String employeeEmail, //
			final Pageable pageable, //
			final Authentication authentication) {
		logger.debug("Search worksites ACTION performed");
		final String effectiveEmployeeEmail = this.authorization.effectiveEmployeeEmail(authentication, employeeEmail);

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
	@Override
	public ResponseEntity<WorksiteResponse> findWorksite(final String worksiteCode) {
		logger.debug("Find worksite ACTION performed");

		final Worksite worksite = this.worksiteService.findWorksiteByCode(worksiteCode);
		final WorksiteResponse response = this.worksiteResponseMapper.map(worksite);
		return ResponseEntity.ok(response);
	}

	/**
	 * Retrieves statistics for a worksite over the requested time range.
	 *
	 * @param worksiteCode the unique code of the worksite; must not be {@code null}
	 * @param start        start of the time range; must not be {@code null}
	 * @param end          end of the time range; must not be {@code null} and must
	 *                     not precede {@code start}
	 * @return the requested worksite statistics
	 * @throws IllegalArgumentException if {@code end} precedes {@code start}
	 */
	@Override
	public ResponseEntity<WorksiteStatsResponse> stats( //
			final String worksiteCode, //
			final Instant start, //
			final Instant end) {
		if (end.isBefore(start)) {
			throw new IllegalArgumentException("end must be greater than or equal to start");
		}

		this.worksiteService.findWorksiteByCode(worksiteCode);
		final long employeesWhoClockedIn = this.employeeService.countDistinctEmployeesWithTimeLogsInRange(worksiteCode,
				start, end);
		final long erroneousTimeLogs = this.employeeService.countOpenTimeLogsInRange(worksiteCode, start, end);
		final long totalTimeLogs = this.employeeService.countTimeLogsInRange(worksiteCode, start, end);
		final long employeesAllowedToClockIn = this.employeeService.countEmployeesAssignedToWorksite(worksiteCode);
		final long distinctSchedules = this.employeeService.countDistinctSchedulesInRange(worksiteCode, start, end);
		return ResponseEntity.ok(new WorksiteStatsResponse(worksiteCode, start, end, employeesWhoClockedIn,
				erroneousTimeLogs, totalTimeLogs, employeesAllowedToClockIn, distinctSchedules));
	}

	/**
	 * Creates a new worksite.
	 *
	 * <p>
	 * The request defines the worksite scope together with its identifying and
	 * descriptive data.
	 * </p>
	 *
	 * @param request the payload describing the worksite to create; must not be
	 *                {@code null}
	 * @return a {@link ResponseEntity} containing the created worksite
	 */
	@Override
	public ResponseEntity<WorksiteResponse> createWorksite(final CreateWorksiteRequest request) {
		logger.debug("Create worksite ACTION performed");

		final String code = request.code().trim();
		final String name = request.name().trim();
		final ZoneId zoneId = ZoneId.of(request.timeZone().trim());
		final WorksiteScope scope = request.scope();
		final String description = request.description() == null ? null : request.description().trim();
		final String address = request.address() == null ? null : request.address().trim();
		final Worksite worksite = this.worksiteService.createWorksite(code, name, zoneId, scope, description, address);

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
	@Override
	public ResponseEntity<WorksiteResponse> updateWorksite(final String worksiteCode,
			final UpdateWorksiteRequest request) {
		logger.debug("Update worksite ACTION performed");

		final String name = request.name().trim();
		final ZoneId zoneId = ZoneId.of(request.timeZone().trim());
		final WorksiteScope scope = request.scope();
		final String description = request.description() == null ? null : request.description().trim();
		final String address = request.address() == null ? null : request.address().trim();
		final Worksite worksite = this.worksiteService.updateWorksite(worksiteCode, name, zoneId, scope, description,
				address);

		final WorksiteResponse response = this.worksiteResponseMapper.map(worksite);
		return ResponseEntity.ok(response);
	}

	/**
	 * Deletes the worksite identified by the given code.
	 *
	 * @param worksiteCode the unique code of the worksite; must not be {@code null}
	 * @return a {@link ResponseEntity} with an empty body and HTTP 204 status
	 */
	@Override
	public ResponseEntity<Void> deleteWorksite(final String worksiteCode) {
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
	 * @return an empty response with HTTP 204 status
	 */
	@Override
	public ResponseEntity<Void> assignEmployeeToWorksite(final String worksiteCode, final String employeeEmail) {
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
	 * @return an empty response with HTTP 204 status
	 */
	@Override
	public ResponseEntity<EmployeeResponse> removeEmployeeFromWorksite( //
			final String worksiteCode, //
			final String employeeEmail) {
		logger.debug("Remove worksite from employee ACTION performed");

		final Employee employee = this.employeeService.findEmployeeByEmail(employeeEmail);
		final Worksite worksite = this.worksiteService.findWorksiteByCode(worksiteCode);
		this.worksiteService.removeEmployeeFromWorksite(worksite, employee);

		return ResponseEntity.noContent().build();
	}
}
