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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import es.nivel36.janus.service.employee.Employee;

/**
 * Repository interface for managing {@link Worksite} entities.
 *
 * <p>
 * Provides access methods to query {@link Worksite} data, including custom
 * queries for searching worksites (with optional visibility filtering by
 * employee) and checking whether a worksite still has explicit employee
 * assignments.
 * </p>
 *
 * <p>
 * This repository extends {@link CrudRepository}, inheriting basic CRUD
 * operations such as save, delete, and find by identifier.
 * </p>
 */
@Repository
interface WorksiteRepository extends JpaRepository<Worksite, Long> {

	Worksite findByCode(String code);

	boolean existsByCode(String code);

	@Query("""
			SELECT (SIZE(w.employees) > 0)
			FROM Worksite w
			WHERE w.code = :worksiteCode
			""")
	boolean hasEmployees(String worksiteCode);

	@Query("""
			SELECT DISTINCT w
			FROM Worksite w
			WHERE (LOWER(w.name) LIKE LOWER(CONCAT('%', :query, '%'))
			   OR LOWER(w.code) LIKE LOWER(CONCAT('%', :query, '%')))
			  AND (:employeeEmail IS NULL
			   OR w.scope = es.nivel36.janus.service.worksite.WorksiteScope.GLOBAL
			   OR EXISTS (
					SELECT 1
					FROM w.employees e
					WHERE e.email = :employeeEmail
			   ))
			""")
	Page<Worksite> search(String query, String employeeEmail, Pageable pageable);

}
