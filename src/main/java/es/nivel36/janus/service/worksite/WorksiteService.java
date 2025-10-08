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
package es.nivel36.janus.service.worksite;

import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.nivel36.janus.service.ResourceNotFoundException;
import es.nivel36.janus.service.employee.Employee;

/**
 * Service class for managing {@link Worksite} entities.
 */
@Service
public class WorksiteService {

	private static final Logger logger = LoggerFactory.getLogger(WorksiteService.class);

	private final WorksiteRepository worksiteRepository;

	/**
	 * Constructs a new {@code WorksiteService} with the given repository.
	 *
	 * @param worksiteRepository the repository used to access worksite data; must
	 *                           not be {@code null}
	 * @throws NullPointerException if {@code worksiteRepository} is {@code null}
	 */
	public WorksiteService(final WorksiteRepository worksiteRepository) {
		this.worksiteRepository = Objects.requireNonNull(worksiteRepository, "WorksiteRepository can't be null");
	}

	/**
	 * Retrieves all {@link Worksite} entities stored in the system.
	 *
	 * @return a list containing every existing {@link Worksite}; never {@code null}
	 */
	@Transactional(readOnly = true)
	public List<Worksite> findAllWorksites() {
		logger.debug("Retrieving all worksites");

		final Iterable<Worksite> worksites = worksiteRepository.findAll();
		return StreamSupport.stream(worksites.spliterator(), false).toList();
	}

	/**
	 * Retrieves all {@link Worksite} entities in which the specified
	 * {@link Employee} is registered.
	 *
	 * @param employee the employee whose worksites should be retrieved; must not be
	 *                 {@code null}
	 * @return a list of {@link Worksite} entities associated with the given
	 *         employee, or an empty list if none exist
	 * @throws NullPointerException if {@code employee} is {@code null}
	 */
	@Transactional(readOnly = true)
	public List<Worksite> findWorksitesByEmployee(final Employee employee) {
		Objects.requireNonNull(employee, "Employee can't be null");
		logger.debug("Finding worksites by employee {}", employee);

		return worksiteRepository.findByEmployee(employee);
	}

	/**
	 * Creates a new {@link Worksite} with the provided data.
	 *
	 * @param code     the unique code identifying the worksite; must not be
	 *                 {@code null}
	 * @param name     the human readable name of the worksite; must not be
	 *                 {@code null}
	 * @param timeZone the {@link ZoneId} associated with the worksite; must not be
	 *                 {@code null}
	 * @return the persisted {@link Worksite}
	 * @throws NullPointerException     if any argument is {@code null}
	 * @throws IllegalArgumentException if a worksite with the given code already
	 *                                  exists
	 */
	@Transactional
	public Worksite createWorksite(final String code, final String name, final ZoneId timeZone) {
		Objects.requireNonNull(code, "Code can't be null");
		Objects.requireNonNull(name, "Name can't be null");
		Objects.requireNonNull(timeZone, "TimeZone can't be null");
		logger.debug("Creating worksite with code {}", code);

		if (worksiteRepository.existsByCode(code)) {
			logger.warn("Unable to create worksite. Code {} already exists", code);
			throw new IllegalArgumentException("Worksite already exists with code " + code);
		}

		final Worksite worksite = new Worksite(code, name, timeZone);
		final Worksite savedWorksite = worksiteRepository.save(worksite);
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
		Objects.requireNonNull(code, "Code can' be null");
		logger.debug("Finding worksites by code {}", code);
		final Worksite worksite = this.worksiteRepository.findWorksiteByCode(code);
		if (worksite == null) {
			logger.warn("No worksite found with code {}", code);
			throw new ResourceNotFoundException("No worksite found with code " + code);
		}
		return worksite;
	}

	/**
	 * Updates an existing {@link Worksite} with the provided data.
	 *
	 * @param code     the code identifying the worksite to update; must not be
	 *                 {@code null}
	 * @param name     the new name to assign; must not be {@code null}
	 * @param timeZone the new {@link ZoneId} to assign; must not be {@code null}
	 * @return the updated {@link Worksite}
	 * @throws NullPointerException      if any argument is {@code null}
	 * @throws ResourceNotFoundException if the worksite cannot be found
	 */
	@Transactional
	public Worksite updateWorksite(final String code, final String name, final ZoneId timeZone) {
		Objects.requireNonNull(code, "Code can't be null");
		Objects.requireNonNull(name, "Name can't be null");
		Objects.requireNonNull(timeZone, "TimeZone can't be null");
		logger.debug("Updating worksite with code {}", code);

		final Worksite worksite = findWorksiteByCode(code);
		worksite.setName(name);
		worksite.setTimeZone(timeZone);

		final Worksite updatedWorksite = worksiteRepository.save(worksite);
		logger.trace("Worksite {} updated successfully", code);
		return updatedWorksite;
	}

	/**
	 * Deletes an existing {@link Worksite}.
	 * 
	 * Before deletion, it is verified that the work site has no assigned workers.
	 *
	 * @param worksite the worksite to delete; must not be {@code null}
	 * @throws NullPointerException if {@code code} is {@code null}
	 */
	@Transactional
	public void deleteWorksite(final Worksite worksite) {
		Objects.requireNonNull(worksite, "Worksite can't be null");
		logger.debug("Deleting worksite {}", worksite);

		final boolean inUse = this.worksiteRepository.hasEmployees(worksite);
		if (inUse) {
			throw new IllegalStateException(
					"The worksite " + worksite + " can't be deleted because it has assigned employees");
		}

		worksiteRepository.delete(worksite);
		logger.trace("Worksite {} deleted successfully", worksite);
	}
}
