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

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import es.nivel36.janus.service.employee.Employee;

/**
 * Repository interface for managing {@link Worksite} entities.
 * <p>
 * Provides access methods to query {@link Worksite} data, including custom
 * queries for retrieving worksites associated with a given {@link Employee} or
 * by a unique worksite code.
 * </p>
 *
 * <p>
 * This repository extends {@link CrudRepository}, inheriting basic CRUD
 * operations such as save, delete, and find by identifier.
 * </p>
 */
@Repository
public interface WorksiteRepository extends CrudRepository<Worksite, Long> {

	/**
	 * Retrieves a list of {@link Worksite} instances in which the specified
	 * {@link Employee} is registered as a member.
	 *
	 * @param employee the employee whose worksites are to be retrieved; must not be
	 *                 {@code null}
	 * @return a list of {@link Worksite} entities associated with the given
	 *         employee, or an empty list if none are found
	 */
	@Query("""
			SELECT w
			FROM Worksite w
			WHERE :employee MEMBER OF w.employees
			""")
	List<Worksite> findByEmployee(Employee employee);

	/**
	 * Finds a {@link Worksite} entity by its unique code.
	 *
	 * @param code the unique identifier code of the worksite; must not be
	 *             {@code null}
	 * @return the {@link Worksite} entity with the given code, or {@code null} if
	 *         no such worksite exists
	 */
	Worksite findByCode(String code);

	/**
	 * Checks whether a {@link Worksite} with the specified code exists.
	 *
	 * @param code the unique identifier code of the worksite; must not be
	 *             {@code null}
	 * @return {@code true} if a worksite with the given code exists; {@code false}
	 *         otherwise
	 */
	boolean existsByCode(String code);

	/**
	 * Checks whether the {@link Worksite} has any associated {@link Employee}
	 * entities.
	 * <p>
	 * This method performs an existence check using the JPQL {@code size()}
	 * function on the {@code employees} collection without loading it into memory.
	 * </p>
	 *
	 * @param worksite the worksite to inspect; must not be {@code null}
	 * @return {@code true} if at least one employee is assigned to the worksite;
	 *         {@code false} otherwise
	 */
	@Query("""
			SELECT (SIZE(w.employees) > 0)
			FROM Worksite w
			WHERE w = :worksite
			""")
	boolean hasEmployees(Worksite worksite);
}
