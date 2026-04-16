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

import java.util.List;

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
 * queries for retrieving worksites visible to a given {@link Employee} and
 * checking whether a worksite still has explicit employee assignments.
 * </p>
 *
 * <p>
 * This repository extends {@link CrudRepository}, inheriting basic CRUD
 * operations such as save, delete, and find by identifier.
 * </p>
 */
@Repository
interface WorksiteRepository extends JpaRepository<Worksite, Long> {

	/**
	 * Retrieves all worksites visible to the specified employee.
	 * <p>
	 * Global worksites are visible to everyone. Personal worksites are visible to
	 * their owner. The legacy employee-worksite assignment is still considered so
	 * the existing association can keep any secondary purpose it may still have.
	 * </p>
	 *
	 * @param employee the employee whose visible worksites are to be retrieved
	 * @return the worksites visible to the employee
	 */
	@Query("""
			SELECT DISTINCT w
			FROM Worksite w
			LEFT JOIN w.employees e
			WHERE w.scope = es.nivel36.janus.service.worksite.WorksiteScope.GLOBAL
			OR w.ownerEmployee = :employee
			OR e = :employee
			""")
	List<Worksite> findVisibleByEmployee(Employee employee);

	Worksite findByCode(String code);

	boolean existsByCode(String code);

	@Query("""
			SELECT (SIZE(w.employees) > 0)
			FROM Worksite w
			WHERE w = :worksite
			""")
	boolean hasEmployees(Worksite worksite);

	@Query("""
			SELECT w
			FROM Worksite w
			WHERE LOWER(w.name) LIKE LOWER(CONCAT('%', :query, '%'))
			   OR LOWER(w.code) LIKE LOWER(CONCAT('%', :query, '%'))
			""")
	Page<Worksite> search(String query, Pageable pageable);

}
