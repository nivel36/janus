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
import java.util.List;
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
 * Service class for managing {@link Worksite} entities.
 *
 * <p>
 * In addition to basic CRUD operations, this service centralizes the business
 * rules introduced by {@link WorksiteScope}: global worksites are visible to
 * every employee, assigned worksites are limited to explicitly assigned
 * employees, and personal worksites require a valid owner employee.
 * </p>
 */
@Service
public class WorksiteService {

	private static final Logger logger = LoggerFactory.getLogger(WorksiteService.class);

	private final WorksiteRepository worksiteRepository;
	private final EmployeeService employeeService;

	/**
	 * Constructs a new {@code WorksiteService} with its required collaborators.
	 *
	 * @param worksiteRepository the repository used to access worksite data; must
	 *                           not be {@code null}
	 * @param employeeService    the employee service to asign a worksite to the
	 *                           employee
	 * @throws NullPointerException if any argument is {@code null}
	 */
	public WorksiteService(final WorksiteRepository worksiteRepository, final EmployeeService employeeService) {
		this.worksiteRepository = Objects.requireNonNull(worksiteRepository, "worksiteRepository can't be null");
		this.employeeService = Objects.requireNonNull(employeeService, "employeeService can't be null");
	}

	/**
	 * Retrieves all {@link Worksite} entities stored in the system.
	 *
	 * @return a list containing every existing {@link Worksite}; never {@code null}
	 */
	@Transactional(readOnly = true)
	public Page<Worksite> searchWorksites(final String query, final Pageable pageable) {
		logger.debug("Retrieving all worksites");
		final Page<Worksite> worksites;
		if (query == null || query.isBlank()) {
			worksites = this.worksiteRepository.findAll(pageable);
		} else {
			worksites = this.worksiteRepository.search(query, pageable);
		}

		logger.trace("Found {} worksites", worksites.getTotalElements());
		return worksites;
	}

	/**
	 * Retrieves all worksites visible to the specified employee.
	 *
	 * <p>
	 * Visibility is primarily driven by {@link WorksiteScope}: global worksites are
	 * visible to everyone, assigned worksites are visible to explicitly assigned
	 * employees and personal worksites are visible to their owner.
	 * </p>
	 *
	 * @param employee the employee whose visible worksites should be retrieved;
	 *                 must not be {@code null}
	 * @return a list of worksites visible to the given employee; never {@code null}
	 * @throws NullPointerException if {@code employee} is {@code null}
	 */
	@Transactional(readOnly = true)
	public List<Worksite> findWorksitesByEmployee(final Employee employee) {
		Objects.requireNonNull(employee, "employee can't be null");
		logger.debug("Finding worksites visible by employee {}", employee);

		final List<Worksite> worksites = this.worksiteRepository.findVisibleByEmployee(employee);
		logger.trace("Found {} worksites", worksites.size());
		return worksites;
	}

	/**
	 * Creates a new {@link Worksite} with the provided classification data.
	 *
	 * @param code          the unique code identifying the worksite; must not be
	 *                      {@code null} and not be used by another worksite
	 * @param name          the human readable name of the worksite; must not be
	 *                      {@code null}
	 * @param timeZone      the {@link ZoneId} associated with the worksite; must
	 *                      not be {@code null}
	 * @param scope         the scope assigned to the worksite; must not be
	 *                      {@code null}
	 * @param ownerEmployee the owner employee for personal worksites; must be
	 *                      {@code null} for global/assigned worksites
	 * @return the persisted {@link Worksite}
	 * @throws NullPointerException           if any mandatory argument is
	 *                                        {@code null}
	 * @throws IllegalArgumentException       if the combination of {@code scope}
	 *                                        and {@code ownerEmployeeId} is
	 *                                        inconsistent
	 * @throws ResourceAlreadyExistsException if a worksite with the given code
	 *                                        already exists
	 * @throws ResourceNotFoundException      if a personal worksite references a
	 *                                        non-existing owner employee
	 */
	@Transactional
	public Worksite createWorksite(final String code, final String name, final ZoneId timeZone,
			final WorksiteScope scope, final Employee ownerEmployee) {
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

		this.checkOwner(scope, ownerEmployee);

		final Worksite worksite = new Worksite(code, name, timeZone, scope, ownerEmployee);
		final Worksite savedWorksite = this.worksiteRepository.save(worksite);
		if (scope == WorksiteScope.PERSONAL) {
			this.employeeService.addWorksiteToEmployee(savedWorksite, ownerEmployee);
		}
		logger.trace("Worksite {} created successfully", code);
		return savedWorksite;
	}

	/**
	 * Retrieves a {@link Worksite} by its code, throwing an exception if it does
	 * not exist.
	 *
	 * @param code the unique worksite code; must not be {@code null}
	 * @return the {@link Worksite} with the given code
	 * @throws NullPointerException      if {@code code} is {@code null}
	 * @throws ResourceNotFoundException if the worksite does not exist
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
	 * Verifies that the given employee is allowed to use the specified worksite.
	 *
	 * <p>
	 * Global worksites can be used by any employee. Assigned worksites can only be
	 * used by explicitly assigned employees. Personal worksites can only be used by
	 * their owner employee.
	 * </p>
	 *
	 * @param employee the employee attempting to use the worksite; must not be
	 *                 {@code null}
	 * @param worksite the target worksite; must not be {@code null}
	 * @throws NullPointerException          if {@code employee} or {@code worksite}
	 *                                       is {@code null}
	 * @throws WorksiteAccessDeniedException if the employee is not allowed to use
	 *                                       the worksite
	 */
	public void assertEmployeeCanUseWorksite(final Employee employee, final Worksite worksite) {
		Objects.requireNonNull(employee, "employee can't be null");
		Objects.requireNonNull(worksite, "worksite can't be null");

		if (worksite.getScope() == WorksiteScope.GLOBAL) {
			return;
		}
		if (worksite.getScope() == WorksiteScope.ASSIGNED) {
			final boolean assigned = this.employeeService.isAssignedToWorksite(employee.getEmail(), worksite.getCode());
			if (assigned) {
				return;
			}
			logger.warn("Employee {} is not assigned to worksite {}", employee.getEmail(), worksite.getCode());
			throw new WorksiteAccessDeniedException(
					"Employee %s cannot use assigned worksite %s because it is not assigned"
							.formatted(employee.getEmail(), worksite.getCode()));
		}

		final Employee ownerEmployee = worksite.getOwnerEmployee();
		if (ownerEmployee != null && Objects.equals(ownerEmployee.getId(), employee.getId())) {
			return;
		}

		logger.warn("Employee {} is not allowed to use personal worksite {} owned by {}", employee.getEmail(),
				worksite.getCode(), ownerEmployee == null ? null : ownerEmployee.getEmail());
		throw new WorksiteAccessDeniedException(
				"Employee %s cannot use personal worksite %s because it belongs to another employee"
						.formatted(employee.getEmail(), worksite.getCode()));
	}

	/**
	 * Updates an existing {@link Worksite} with the provided classification data.
	 *
	 * @param code          the code identifying the worksite to update; must not be
	 *                      {@code null}
	 * @param newName       the new name to assign; must not be {@code null}
	 * @param newTimeZone   the new {@link ZoneId} to assign; must not be
	 *                      {@code null}
	 * @param newScope      the new worksite scope; must not be {@code null}
	 * @param ownerEmployee the owner employee for personal worksites; must be
	 *                      {@code null} for global/assigned worksites
	 * @return the updated {@link Worksite}
	 * @throws NullPointerException      if any mandatory argument is {@code null}
	 * @throws IllegalArgumentException  if the combination of {@code newScope} and
	 *                                   {@code ownerEmployeeId} is inconsistent
	 * @throws ResourceNotFoundException if the worksite or the requested owner
	 *                                   employee cannot be found
	 */
	@Transactional
	public Worksite updateWorksite(final String code, final String newName, final ZoneId newTimeZone,
			final WorksiteScope newScope, final Employee ownerEmployee) {
		Objects.requireNonNull(code, "code can't be null");
		Objects.requireNonNull(newName, "newName can't be null");
		Objects.requireNonNull(newTimeZone, "newTimeZone can't be null");
		Objects.requireNonNull(newScope, "newScope can't be null");
		logger.debug("Updating worksite with code {}", code);

		this.checkOwner(newScope, ownerEmployee);

		final Worksite worksite = this.findWorksite(code);
		worksite.setName(newName);
		worksite.setTimeZone(newTimeZone);
		worksite.updateScope(newScope, ownerEmployee);

		final Worksite updatedWorksite = this.worksiteRepository.save(worksite);
		logger.trace("Worksite {} updated successfully", code);
		return updatedWorksite;
	}

	private void checkOwner(final WorksiteScope scope, final Employee ownerEmployee) {
		if (scope != WorksiteScope.PERSONAL) {
			if (ownerEmployee != null) {
				throw new IllegalArgumentException("global or assigned worksites can't define an owner employee");
			}
		} else if (ownerEmployee == null) {
			throw new IllegalArgumentException("personal worksites require an owner employee");
		}
	}

	/**
	 * Deletes an existing {@link Worksite}.
	 *
	 * <p>
	 * Before deletion, it is verified that the worksite has no explicitly assigned
	 * employees in the legacy association.
	 * </p>
	 *
	 * @param worksite the worksite to delete; must not be {@code null}
	 * @throws NullPointerException if {@code worksite} is {@code null}
	 */
	@Transactional
	public void deleteWorksite(final Worksite worksite) {
		Objects.requireNonNull(worksite, "worksite can't be null");
		logger.debug("Deleting worksite {}", worksite);

		final boolean inUse = this.worksiteRepository.hasEmployees(worksite);
		if (inUse) {
			throw new IllegalStateException(
					"The worksite " + worksite + " can't be deleted because it has assigned employees");
		}

		this.worksiteRepository.delete(worksite);
		logger.trace("Worksite {} deleted successfully", worksite);
	}
}
