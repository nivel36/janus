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
import es.nivel36.janus.policy.worksite.WorksiteAuthorizationAdapter;
import es.nivel36.janus.service.employee.EmployeeService;
import es.nivel36.janus.service.worksite.Worksite;
import es.nivel36.janus.service.worksite.WorksiteScope;
import es.nivel36.janus.service.worksite.WorksiteService;

/**
 * Spring MVC implementation of {@link WorksiteResource}.
 */
@RestController
public class WorksiteController implements WorksiteResource {

	private static final Logger logger = LoggerFactory.getLogger(WorksiteController.class);

	private final WorksiteService worksiteService;
	private final EmployeeService employeeService;
	private final WorksiteAuthorizationAdapter authorization;
	private final Mapper<Worksite, WorksiteResponse> worksiteResponseMapper;

	/**
	 * Creates a controller ready to delegate resource operations without accessing
	 * persistence during construction.
	 *
	 * @param  worksiteService        nonnull worksite service
	 * @param  employeeService        nonnull employee lookup and statistics service
	 * @param  authorization          nonnull employee-scope resolver
	 * @param  worksiteResponseMapper nonnull response mapper
	 * @throws NullPointerException   if any dependency is null
	 */
	public WorksiteController(
		final WorksiteService worksiteService,
		final EmployeeService employeeService,
		final WorksiteAuthorizationAdapter authorization,
		final @Qualifier("worksiteResponseMapper") Mapper<Worksite, WorksiteResponse> worksiteResponseMapper) {
		this.worksiteService = Objects.requireNonNull(worksiteService, "worksiteService can't be null");
		this.employeeService = Objects.requireNonNull(employeeService, "employeeService can't be null");
		this.authorization = Objects.requireNonNull(authorization, "authorization can't be null");
		this.worksiteResponseMapper = Objects
				.requireNonNull(worksiteResponseMapper, "worksiteResponseMapper can't be null");
	}

	@Override
	public ResponseEntity<Page<WorksiteResponse>> searchWorksites(
			final String query,
			final String employeeNumber,
			final Pageable pageable,
			final Authentication authentication) {
		logger.debug("Search worksites ACTION performed");

		final String effectiveEmployeeNumber = this.authorization
				.effectiveEmployeeNumber(authentication, employeeNumber);

		final Page<WorksiteResponse> worksites = this.worksiteService
				.searchWorksites(query, effectiveEmployeeNumber, pageable).map(this.worksiteResponseMapper::map);
		return ResponseEntity.ok(worksites);
	}

	@Override
	public ResponseEntity<WorksiteResponse> findWorksite(final String worksiteCode) {
		logger.debug("Find worksite ACTION performed");

		final Worksite worksite = this.worksiteService.findWorksiteByCode(worksiteCode);
		final WorksiteResponse response = this.worksiteResponseMapper.map(worksite);
		return ResponseEntity.ok(response);
	}

	@Override
	public ResponseEntity<WorksiteStatsResponse> stats(
			final String worksiteCode,
			final Instant start,
			final Instant end) {
		logger.debug("Worksite statistics ACTION performed");

		if (!start.isBefore(end)) {
			throw new IllegalArgumentException("end must be after start");
		}

		this.worksiteService.findWorksiteByCode(worksiteCode);
		final long employeesWhoClockedIn = this.employeeService
				.countDistinctEmployeesWithTimeLogsInRange(worksiteCode, start, end);
		final long erroneousTimeLogs = this.employeeService.countOpenTimeLogsInRange(worksiteCode, start, end);
		final long totalTimeLogs = this.employeeService.countTimeLogsInRange(worksiteCode, start, end);
		final long employeesAllowedToClockIn = this.employeeService.countEmployeesAssignedToWorksite(worksiteCode);
		final long distinctSchedules = this.employeeService.countDistinctSchedulesInRange(worksiteCode, start, end);
		return ResponseEntity.ok(
				new WorksiteStatsResponse(
						worksiteCode,
						start,
						end,
						employeesWhoClockedIn,
						erroneousTimeLogs,
						totalTimeLogs,
						employeesAllowedToClockIn,
						distinctSchedules));
	}

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

	@Override
	public ResponseEntity<WorksiteResponse> updateWorksite(
			final String worksiteCode,
			final UpdateWorksiteRequest request) {
		logger.debug("Update worksite ACTION performed");

		final String name = request.name().trim();
		final ZoneId zoneId = ZoneId.of(request.timeZone().trim());
		final WorksiteScope scope = request.scope();
		final String description = request.description() == null ? null : request.description().trim();
		final String address = request.address() == null ? null : request.address().trim();
		final Worksite worksite = this.worksiteService
				.updateWorksite(worksiteCode, name, zoneId, scope, description, address);

		final WorksiteResponse response = this.worksiteResponseMapper.map(worksite);
		return ResponseEntity.ok(response);
	}

	@Override
	public ResponseEntity<Void> deleteWorksite(final String worksiteCode) {
		logger.debug("Delete worksite ACTION performed");

		this.worksiteService.deleteWorksite(worksiteCode);
		return ResponseEntity.noContent().build();
	}

	@Override
	public ResponseEntity<Void> assignEmployeeToWorksite(final String worksiteCode, final String employeeNumber) {
		logger.debug("Add worksite to employee ACTION performed");

		this.worksiteService.addEmployeeToWorksite(worksiteCode, employeeNumber);
		return ResponseEntity.noContent().build();
	}

	@Override
	public ResponseEntity<Void> removeEmployeeFromWorksite(final String worksiteCode, final String employeeNumber) {
		logger.debug("Remove worksite from employee ACTION performed");

		this.worksiteService.removeEmployeeFromWorksite(worksiteCode, employeeNumber);

		return ResponseEntity.noContent().build();
	}

}
