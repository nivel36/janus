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
package es.nivel36.janus.service.worksite;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import es.nivel36.janus.service.ResourceAlreadyExistsException;
import es.nivel36.janus.service.ResourceNotFoundException;
import es.nivel36.janus.service.employee.Employee;
import es.nivel36.janus.service.employee.EmployeeService;
import es.nivel36.janus.util.LikePatterns;
import es.nivel36.janus.validation.EmployeeNumber;
import es.nivel36.janus.validation.SearchQuery;
import es.nivel36.janus.validation.WorksiteCode;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Transactional entry point for worksite lookup, search, descriptive changes,
 * logical deletion and employee assignments. Callers must authorize operations.
 */
@Validated
@Service
public class WorksiteService {

	private static final Logger logger = LoggerFactory.getLogger(WorksiteService.class);

	private final WorksiteRepository worksiteRepository;

	private final EmployeeService employeeService;
	private final int maxPageSize;

	/**
	 * Creates a service with nonnull collaborators and a positive page-size limit.
	 * Construction performs no database access.
	 *
	 * @param  worksiteRepository       nonnull worksite repository
	 * @param  employeeService          nonnull employee assignment lookup service
	 * @param  maxPageSize              positive limit from
	 *                                  spring.data.rest.max-page-size
	 * @throws NullPointerException     if a dependency is null
	 * @throws IllegalArgumentException if maxPageSize is not positive
	 */
	public WorksiteService(
		final WorksiteRepository worksiteRepository,
		final EmployeeService employeeService,
		final @Value("${spring.data.rest.max-page-size}") int maxPageSize) {
		this.worksiteRepository = Objects.requireNonNull(worksiteRepository, "worksiteRepository can't be null");
		this.employeeService = Objects.requireNonNull(employeeService, "employeeService can't be null");
		if (maxPageSize < 1) {
			throw new IllegalArgumentException("maxPageSize must be positive");
		}
		this.maxPageSize = maxPageSize;
	}

	/**
	 * Searches visible worksites without changing them.
	 * <p>
	 * Query matches code, name, description or address literally, partially and
	 * case-insensitively, without trimming. Null disables the text restriction. An
	 * employee filter includes GLOBAL worksites and worksites explicitly assigned
	 * to that number, and combines with the text restriction using AND. The caller
	 * must supply the authorized effective employee number.
	 * </p>
	 * <p>
	 * Page size is capped at the configured limit. Sort fields are code, name,
	 * timeZone, scope, description and address. Default ordering is ascending code;
	 * code ascending breaks ties unless code is explicitly ordered. Deleted
	 * worksites are excluded.
	 * </p>
	 *
	 * @param  query                        optional literal single-line fragment of
	 *                                      1-100 characters
	 * @param  employeeNumber               optional exact number matching
	 *                                      [A-Za-z0-9_-]{1,50}
	 * @param  pageable                     nonnull paged request with public sort
	 *                                      fields
	 * @return                              matching page, possibly empty
	 * @throws ConstraintViolationException if query, employeeNumber or pageable
	 *                                      fails validation through the Spring
	 *                                      proxy
	 * @throws IllegalArgumentException     if pageable is unpaged or sorting is
	 *                                      unsupported
	 */
	@Transactional(readOnly = true)
	public Page<Worksite> searchWorksites(
			final @SearchQuery String query,
			final @EmployeeNumber String employeeNumber,
			final @NotNull Pageable pageable) {
		final Pageable normalizedPageable = this.normalizePageable(pageable);
		final String escapedQuery = query == null ? "" : LikePatterns.escape(query);

		logger.atDebug().addKeyValue("query", query).addKeyValue("employeeNumber", employeeNumber)
				.addKeyValue("page", normalizedPageable.getPageNumber())
				.addKeyValue("pageSize", normalizedPageable.getPageSize())
				.addKeyValue("sort", normalizedPageable.getSort().toString()).log("Searching worksites");

		return this.worksiteRepository.search(escapedQuery, employeeNumber, normalizedPageable);
	}

	private Pageable normalizePageable(final Pageable pageable) {
		if (pageable.isUnpaged()) {
			throw new IllegalArgumentException("Must be paged");
		}
		final List<Order> orders = new ArrayList<>();
		for (final Order order : pageable.getSort()) {
			switch (order.getProperty()) {
			case "code", "name", "timeZone", "scope", "description", "address" -> orders.add(order);
			default -> throw new IllegalArgumentException("Unsupported Worksite sort field: " + order.getProperty());
			}
		}
		// The unique business code keeps page boundaries stable when sort values tie.
		if (orders.stream().noneMatch(order -> "code".equals(order.getProperty()))) {
			orders.add(Order.asc("code"));
		}
		final int pageNumber = pageable.getPageNumber();
		final int pageSize = Math.min(pageable.getPageSize(), this.maxPageSize);
		final Sort sort = Sort.by(orders);
		return PageRequest.of(pageNumber, pageSize, sort);
	}

	/**
	 * Creates a worksite without assigning employees. The caller supplies
	 * validated, already normalized data. Code must match
	 * {@code [A-Za-z0-9_-]{1,50}}; name must be nonblank. Zone and scope are
	 * required. Description and address may be null and are stored as supplied.
	 *
	 * @param  code                           exact immutable business code
	 * @param  name                           nonblank display name
	 * @param  timeZone                       nonnull worksite timezone
	 * @param  scope                          nonnull visibility scope
	 * @param  description                    optional descriptive text
	 * @param  address                        optional address
	 * @return                                persisted worksite whose insert
	 *                                        commits with the transaction
	 * @throws ConstraintViolationException   if a parameter constraint fails
	 *                                        through the Spring proxy
	 * @throws ResourceAlreadyExistsException if a visible worksite uses the code
	 */
	@Transactional
	public Worksite createWorksite(
			final @NotBlank @WorksiteCode String code,
			final @NotBlank String name,
			final @NotNull ZoneId timeZone,
			final @NotNull WorksiteScope scope,
			final String description,
			final String address) {
		logger.atDebug().addKeyValue("worksiteCode", code).addKeyValue("scope", scope).log("Creating worksite");

		final boolean existsByCode = this.worksiteRepository.existsByCode(code);
		if (existsByCode) {
			logger.warn("Unable to create worksite. Code {} already exists", code);
			throw new ResourceAlreadyExistsException("Worksite already exists with code " + code);
		}

		final Worksite worksite = new Worksite(code, name, timeZone, scope);
		worksite.setDescription(description);
		worksite.setAddress(address);
		final Worksite savedWorksite = this.worksiteRepository.save(worksite);
		logger.info("Worksite created: worksiteCode={}, scope={}", code, scope);
		return savedWorksite;
	}

	/**
	 * Looks up an exact business code without changing the worksite.
	 *
	 * @param  code                         nonblank code matching
	 *                                      [A-Za-z0-9_-]{1,50} without trimming
	 * @return                              existing nondeleted worksite
	 * @throws ConstraintViolationException if code fails validation through the
	 *                                      Spring proxy
	 * @throws ResourceNotFoundException    if no visible worksite has that code
	 */
	@Transactional(readOnly = true)
	public Worksite findWorksiteByCode(final @NotBlank @WorksiteCode String code) {
		return this.findWorksite(code);
	}

	private Worksite findWorksite(final String code) {
		final Worksite worksite = this.worksiteRepository.findByCode(code);
		if (worksite == null) {
			logger.warn("No worksite found with code {}", code);
			throw new ResourceNotFoundException("No worksite found with code " + code);
		}
		return worksite;
	}

	/**
	 * Checks whether an existing employee may use an existing worksite for time
	 * logging. Both resources are loaded within this transaction. GLOBAL requires
	 * no assignment; ASSIGNED requires an explicit persistent employee-worksite
	 * association. Neither resource is modified.
	 *
	 * @param  employeeNumber                exact nonblank number matching
	 *                                       [A-Za-z0-9_-]{1,50}
	 * @param  worksiteCode                  exact nonblank code matching
	 *                                       [A-Za-z0-9_-]{1,50}
	 * @throws ConstraintViolationException  if an identifier fails validation
	 *                                       through the Spring proxy
	 * @throws ResourceNotFoundException     if either resource is absent
	 * @throws WorksiteAccessDeniedException if an ASSIGNED worksite has no matching
	 *                                       assignment
	 */
	@Transactional(readOnly = true)
	public void assertEmployeeCanUseWorksite(
			final @NotBlank @EmployeeNumber String employeeNumber,
			final @NotBlank @WorksiteCode String worksiteCode) throws WorksiteAccessDeniedException {
		logger.debug("Checking whether employee with number {} can use worksite {}", employeeNumber, worksiteCode);
		final Employee employee = this.employeeService.findEmployeeByEmployeeNumber(employeeNumber);
		final Worksite worksite = this.findWorksite(worksiteCode);
		if (worksite.getScope() == WorksiteScope.GLOBAL) {
			return;
		}
		final boolean assigned = this.employeeService.isAssignedToWorksite(employee.getId(), worksiteCode);
		if (!assigned) {
			logger.warn("Employee with number {} is not assigned to worksite {}", employeeNumber, worksiteCode);
			throw new WorksiteAccessDeniedException(
					"Employee %s cannot use assigned worksite %s because it is not assigned"
							.formatted(employeeNumber, worksiteCode));
		}
	}

	/**
	 * Replaces descriptive data and scope of an existing worksite. Values are
	 * supplied already normalized; null description and address clear those fields.
	 * Code, employee assignments and time logs remain unchanged. Scope may remain
	 * unchanged or expand from ASSIGNED to GLOBAL. Changes commit with the
	 * transaction; a rejected transition rolls it back.
	 *
	 * @param  code                         exact nonblank business code matching
	 *                                      [A-Za-z0-9_-]{1,50}
	 * @param  newName                      replacement nonblank display name
	 * @param  newTimeZone                  nonnull replacement timezone
	 * @param  newScope                     nonnull replacement scope
	 * @param  newDescription               optional replacement description
	 * @param  newAddress                   optional replacement address
	 * @return                              updated persisted worksite
	 * @throws ConstraintViolationException if a parameter constraint fails through
	 *                                      the Spring proxy
	 * @throws ResourceNotFoundException    if the target is absent
	 * @throws IllegalArgumentException     if the scope transition is not allowed
	 */
	@Transactional
	public Worksite updateWorksite(
			final @NotBlank @WorksiteCode String code,
			final @NotBlank String newName,
			final @NotNull ZoneId newTimeZone,
			final @NotNull WorksiteScope newScope,
			final String newDescription,
			final String newAddress) {
		logger.atDebug().addKeyValue("worksiteCode", code).addKeyValue("name", newName)
				.addKeyValue("timeZone", newTimeZone).addKeyValue("scope", newScope).log("Updating worksite");

		final Worksite worksite = this.findWorksite(code);
		worksite.updateScope(newScope);
		worksite.setName(newName);
		worksite.setTimeZone(newTimeZone);
		worksite.setDescription(newDescription);
		worksite.setAddress(newAddress);

		final Worksite updatedWorksite = this.worksiteRepository.save(worksite);
		return updatedWorksite;
	}

	/**
	 * Logically deletes an existing worksite by its exact business code. The
	 * worksite is loaded within this transaction and must have no employee
	 * assignments. After commit, lookup and search exclude it while historical time
	 * logs remain stored.
	 *
	 * @param  code                         nonblank code matching
	 *                                      [A-Za-z0-9_-]{1,50} without trimming
	 * @throws ConstraintViolationException if code fails validation through the
	 *                                      Spring proxy
	 * @throws ResourceNotFoundException    if the target is absent
	 * @throws IllegalStateException        if employees are still assigned
	 */
	@Transactional
	public void deleteWorksite(final @NotBlank @WorksiteCode String code) {
		logger.debug("Worksite with code {} marked for deletion", code);
		final Worksite worksite = this.findWorksite(code);

		final boolean inUse = this.worksiteRepository.hasEmployees(code);
		if (inUse) {
			throw new IllegalStateException(
					"The worksite " + worksite + " can't be deleted because it has assigned employees");
		}

		this.worksiteRepository.delete(worksite);
	}

	/**
	 * Idempotently assigns an existing employee to an existing worksite. Both
	 * resources are loaded within this transaction and both sides of the
	 * association are updated. Existing assignments do not trigger a save; neither
	 * resource is created or deleted.
	 *
	 * @param  worksiteCode                 exact code matching [A-Za-z0-9_-]{1,50}
	 * @param  employeeNumber               exact number matching
	 *                                      [A-Za-z0-9_-]{1,50}
	 * @return                              true when an assignment was added, false
	 *                                      when it already existed
	 * @throws ConstraintViolationException if an identifier fails validation
	 *                                      through the Spring proxy
	 * @throws ResourceNotFoundException    if either resource is absent
	 */
	@Transactional
	public boolean addEmployeeToWorksite(
			final @NotBlank @WorksiteCode String worksiteCode,
			final @NotBlank @EmployeeNumber String employeeNumber) {
		final Employee employee = this.employeeService.findEmployeeByEmployeeNumber(employeeNumber);
		final Worksite worksite = this.findWorksite(worksiteCode);

		logger.debug(
				"Assigning employee with number {} to worksite {}",
				employee.getEmployeeNumber(),
				worksite.getCode());

		final boolean added = worksite.assignEmployee(employee);
		if (added) {
			this.worksiteRepository.save(worksite);
		}
		return added;
	}

	/**
	 * Idempotently removes an assignment between existing resources. Both resources
	 * are loaded within this transaction and both sides of the association are
	 * updated. Missing assignments do not trigger a save; neither resource is
	 * deleted.
	 *
	 * @param  worksiteCode                 exact code matching [A-Za-z0-9_-]{1,50}
	 * @param  employeeNumber               exact number matching
	 *                                      [A-Za-z0-9_-]{1,50}
	 * @return                              true when an assignment was removed,
	 *                                      false when it was absent
	 * @throws ConstraintViolationException if an identifier fails validation
	 *                                      through the Spring proxy
	 * @throws ResourceNotFoundException    if either resource is absent
	 */
	@Transactional
	public boolean removeEmployeeFromWorksite(
			final @NotBlank @WorksiteCode String worksiteCode,
			final @NotBlank @EmployeeNumber String employeeNumber) {
		final Employee employee = this.employeeService.findEmployeeByEmployeeNumber(employeeNumber);
		final Worksite worksite = this.findWorksite(worksiteCode);

		logger.debug(
				"Removing employee with number {} from worksite {}",
				employee.getEmployeeNumber(),
				worksite.getCode());

		final boolean removed = worksite.removeEmployee(employee);
		if (removed) {
			this.worksiteRepository.save(worksite);
		}
		return removed;
	}
}
