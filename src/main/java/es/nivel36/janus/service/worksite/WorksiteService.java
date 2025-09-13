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

import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
	 * Finds a {@link Worksite} entity by its unique code.
	 *
	 * @param code the unique worksite code; must not be {@code null}
	 * @return the {@link Worksite} with the given code, or {@code null} if none
	 *         exists
	 * @throws NullPointerException if {@code code} is {@code null}
	 */
	@Transactional(readOnly = true)
	public Worksite findWorksiteByCode(final String code) {
		Objects.requireNonNull(code, "Code can't be null");
		logger.debug("Finding worksite by code {}", code);
		
		return worksiteRepository.findWorksiteByCode(code);
	}
}
