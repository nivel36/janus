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
package es.nivel36.janus.service.worksite;

import java.time.ZoneId;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.nivel36.janus.service.ResourceAlreadyExistsException;
import es.nivel36.janus.service.ResourceNotFoundException;
import es.nivel36.janus.service.employee.Employee;
import es.nivel36.janus.service.employee.EmployeeService;

/**
 * Service class responsible for managing {@link Worksite} entities.
 *
 * <p>
 * This service provides CRUD operations and enforces business rules related to
 * {@link WorksiteScope}. Depending on the scope, access and visibility rules
 * vary:
 * <ul>
 * <li>{@code GLOBAL}: accessible by all employees</li>
 * <li>{@code ASSIGNED}: accessible only by explicitly assigned employees</li>
 * <li>{@code PERSONAL}: restricted to its owner</li>
 * </ul>
 *
 * <p>
 * It also coordinates with {@link WorksiteRepository} for persistence and
 * {@link EmployeeService} for employee-related validations.
 */
@Service
public class WorksiteService {

	private static final Logger logger = LoggerFactory.getLogger(WorksiteService.class);

	/**
	 * Repository used to perform persistence operations on {@link Worksite}.
	 */
	private final WorksiteRepository worksiteRepository;

	/**
	 * Service used to manage and validate {@link Employee} associations.
	 */
	private final EmployeeService employeeService;

	/**
	 * Constructs a new {@code WorksiteService} with its required collaborators.
	 *
	 * @param worksiteRepository the repository used to access worksite data; must
	 *                           not be {@code null}
	 * @param employeeService    the employee service used for employee-related
	 *                           operations; must not be {@code null}
	 * @throws NullPointerException if any argument is {@code null}
	 */
	public WorksiteService(final WorksiteRepository worksiteRepository, final EmployeeService employeeService) {
		this.worksiteRepository = Objects.requireNonNull(worksiteRepository, "worksiteRepository can't be null");
		this.employeeService = Objects.requireNonNull(employeeService, "employeeService can't be null");
	}

	/**
	 * Searches {@link Worksite} entities using an optional query and employee
	 * filter.
	 *
	 * <p>
	 * If both parameters are empty, all worksites are returned. Otherwise, a
	 * filtered search is performed.
	 *
	 * @param query         a text query to filter worksites; may be {@code null} or
	 *                      blank
	 * @param employeeEmail the employee email used to filter assigned worksites;
	 *                      may be {@code null}
	 * @param pageable      pagination information; must not be {@code null}
	 * @return a {@link Page} of matching {@link Worksite} instances; never
	 *         {@code null}
	 */
	@Transactional(readOnly = true)
	public Page<Worksite> searchWorksites(final String query, final String employeeEmail, final Pageable pageable) {
		logger.debug("Retrieving all worksites");
		final String sanitizedQuery = query == null ? "" : query.strip();
		final String sanitizedEmployeeEmail = employeeEmail == null || employeeEmail.isBlank() ? null
				: employeeEmail.strip();
		final Page<Worksite> worksites;
		if (sanitizedQuery.isEmpty() && sanitizedEmployeeEmail == null) {
			worksites = this.worksiteRepository.findAll(pageable);
		} else {
			worksites = this.worksiteRepository.search(sanitizedQuery, sanitizedEmployeeEmail, pageable);
		}

		logger.trace("Found {} worksites", worksites.getTotalElements());
		return worksites;
	}

	/**
	 * Creates a new {@link Worksite}.
	 *
	 * @param code     the unique worksite identifier; must not be {@code null} and
	 *                 must be unique
	 * @param name     the human-readable name; must not be {@code null}
	 * @param timeZone the {@link ZoneId} of the worksite; must not be {@code null}
	 * @param scope    the {@link WorksiteScope} defining visibility; must not be
	 *                 {@code null}
	 * @return the persisted {@link Worksite}
	 * @throws NullPointerException           if any parameter is {@code null}
	 * @throws ResourceAlreadyExistsException if a worksite with the same code
	 *                                        already exists
	 */
	@Transactional
	public Worksite createWorksite(final String code, final String name, final ZoneId timeZone,
			final WorksiteScope scope) {
		Objects.requireNonNull(code, "code can't be null");
		Objects.requireNonNull(name, "name can't be null");
		Objects.requireNonNull(timeZone, "timeZone can't be null");
		Objects.requireNonNull(scope, "scope can't be null");
		logger.debug("Creating {} worksite with code {}", scope, code);

		final boolean existsByCode = this.worksiteRepository.existsByCode(code);
		if (existsByCode) {
			logger.warn("Unable to create worksite. Code {} already exists", code);
			throw new ResourceAlreadyExistsException("Worksite already exists with code " + code);
		}

		final Worksite worksite = new Worksite(code, name, timeZone, scope);
		final Worksite savedWorksite = this.worksiteRepository.save(worksite);
		logger.trace("Worksite {} created successfully", code);
		return savedWorksite;
	}

	/**
	 * Retrieves a {@link Worksite} by its unique code.
	 *
	 * @param code the worksite code; must not be {@code null}
	 * @return the matching {@link Worksite}
	 * @throws NullPointerException      if {@code code} is {@code null}
	 * @throws ResourceNotFoundException if no worksite exists with the given code
	 */
	@Transactional(readOnly = true)
	public Worksite findWorksiteByCode(final String code) {
		Objects.requireNonNull(code, "code can't be null");
		logger.debug("Finding worksites by code {}", code);

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
	 * Verifies whether an employee can use a given {@link Worksite}.
	 *
	 * <p>
	 * Rules:
	 * <ul>
	 * <li>{@code GLOBAL}: always allowed</li>
	 * <li>{@code ASSIGNED}: allowed only if explicitly assigned</li>
	 * </ul>
	 *
	 * @param employeeEmail the employee email; must not be {@code null}
	 * @param worksite      the target worksite; must not be {@code null}
	 * @throws NullPointerException          if any parameter is {@code null}
	 * @throws WorksiteAccessDeniedException if access is not permitted
	 */
	public void assertEmployeeCanUseWorksite(final String employeeEmail, final Worksite worksite) {
		Objects.requireNonNull(employeeEmail, "employeeEmail can't be null");
		Objects.requireNonNull(worksite, "worksite can't be null");

		if (worksite.getScope() == WorksiteScope.GLOBAL) {
			return;
		}
		if (worksite.getScope() == WorksiteScope.ASSIGNED) {
			final boolean assigned = this.employeeService.isAssignedToWorksite(employeeEmail, worksite.getCode());
			if (assigned) {
				return;
			}
			logger.warn("Employee {} is not assigned to worksite {}", employeeEmail, worksite.getCode());
			throw new WorksiteAccessDeniedException(
					"Employee %s cannot use assigned worksite %s because it is not assigned".formatted(employeeEmail,
							worksite.getCode()));
		}
	}

	/**
	 * Updates an existing {@link Worksite}.
	 *
	 * @param code        the identifier of the worksite; must not be {@code null}
	 * @param newName     the new name; must not be {@code null}
	 * @param newTimeZone the new {@link ZoneId}; must not be {@code null}
	 * @param newScope    the new {@link WorksiteScope}; must not be {@code null}
	 * @return the updated {@link Worksite}
	 * @throws NullPointerException      if any parameter is {@code null}
	 * @throws ResourceNotFoundException if the worksite does not exist
	 */
	@Transactional
	public Worksite updateWorksite(final String code, final String newName, final ZoneId newTimeZone,
			final WorksiteScope newScope) {
		Objects.requireNonNull(code, "code can't be null");
		Objects.requireNonNull(newName, "newName can't be null");
		Objects.requireNonNull(newTimeZone, "newTimeZone can't be null");
		Objects.requireNonNull(newScope, "newScope can't be null");
		logger.debug("Updating worksite with code {}", code);

		final Worksite worksite = this.findWorksite(code);
		worksite.setName(newName);
		worksite.setTimeZone(newTimeZone);
		worksite.updateScope(newScope);

		final Worksite updatedWorksite = this.worksiteRepository.save(worksite);
		logger.trace("Worksite {} updated successfully", code);
		return updatedWorksite;
	}

	/**
	 * Deletes a {@link Worksite} if it is not currently associated with employees.
	 *
	 * @param worksite the worksite to delete; must not be {@code null}
	 * @throws NullPointerException  if {@code worksite} is {@code null}
	 * @throws IllegalStateException if the worksite still has assigned employees
	 */
	@Transactional
	public void deleteWorksite(final Worksite worksite) {
		Objects.requireNonNull(worksite, "worksite can't be null");
		logger.debug("Deleting worksite {}", worksite);

		final boolean inUse = this.worksiteRepository.hasEmployees(worksite.getCode());
		if (inUse) {
			throw new IllegalStateException(
					"The worksite " + worksite + " can't be deleted because it has assigned employees");
		}

		this.worksiteRepository.delete(worksite);
		logger.trace("Worksite {} deleted successfully", worksite);
	}

	/**
	 * Associates an {@link Employee} with a {@link Worksite}.
	 *
	 * <p>
	 * This operation is idempotent.
	 *
	 * @param worksite the worksite; must not be {@code null}
	 * @param employee the employee; must not be {@code null}
	 * @return {@code true} if the association was created, {@code false} otherwise
	 * @throws NullPointerException if any parameter is {@code null}
	 */
	@Transactional
	public boolean addEmployeeToWorksite(final Worksite worksite, final Employee employee) {
		Objects.requireNonNull(worksite, "worksite can't be null");
		Objects.requireNonNull(employee, "employee can't be null");

		logger.debug("Adding employee {} to worksite {}", employee, worksite);

		final boolean added = worksite.assignEmployee(employee);
		if (added) {
			this.worksiteRepository.save(worksite);
		}
		return added;
	}

	/**
	 * Removes the association between an {@link Employee} and a {@link Worksite}.
	 *
	 * @param worksite the worksite; must not be {@code null}
	 * @param employee the employee; must not be {@code null}
	 * @return {@code true} if the association was removed, {@code false} otherwise
	 * @throws NullPointerException if any parameter is {@code null}
	 */
	@Transactional
	public boolean removeEmployeeFromWorksite(final Worksite worksite, final Employee employee) {
		Objects.requireNonNull(worksite, "worksite can't be null");
		Objects.requireNonNull(employee, "employee can't be null");

		logger.debug("Removing employee {} from worksite {}", employee, worksite);

		final boolean removed = worksite.removeEmployee(employee);
		if (removed) {
			this.worksiteRepository.save(worksite);
		}
		return removed;
	}
}