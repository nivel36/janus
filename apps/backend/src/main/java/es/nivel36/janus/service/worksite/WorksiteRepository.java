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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Internal persistence contract for nondeleted worksites.
 */
@Repository
interface WorksiteRepository extends JpaRepository<Worksite, Long> {

	/**
	 * Retrieves a {@link Worksite} by its unique code.
	 *
	 * @param  code the unique identifier of the worksite. Can't be {@code null}.
	 * @return      the {@link Worksite} associated with the given code, or
	 *              {@code null} if no worksite matches the provided code.
	 */
	Worksite findByCode(String code);

	/**
	 * Checks whether a {@link Worksite} exists with the specified code.
	 *
	 * @param  code the unique identifier of the worksite. Can't be {@code null}.
	 * @return      {@code true} if a worksite exists with the given code;
	 *              {@code false} otherwise.
	 */
	boolean existsByCode(String code);

	/**
	 * Determines whether the {@link Worksite} identified by the given code has at
	 * least one associated employee.
	 *
	 * @param  worksiteCode the unique code of the worksite. Can't be {@code null}.
	 * @return              {@code true} if the worksite has one or more associated
	 *                      employees; {@code false} otherwise.
	 */
	@Query("""
			SELECT (SIZE(w.employees) > 0)
			FROM Worksite w
			WHERE w.code = :worksiteCode
			""")
	boolean hasEmployees(String worksiteCode);

	/**
	 * Queries worksites with an already escaped literal text fragment. Code, name,
	 * description and address match case-insensitively. An empty query disables the
	 * text restriction. Filters combine with AND. A null employee number includes
	 * all scopes; otherwise GLOBAL and explicitly assigned worksites are included.
	 * No worksite or employee association is changed.
	 *
	 * @param  query          nonnull fragment escaped with LikePatterns.escape for
	 *                        SQL LIKE
	 * @param  employeeNumber exact validated number, or null to disable visibility
	 *                        filtering
	 * @param  pageable       nonnull normalized paging and entity-property sorting
	 * @return                matching page, possibly empty; deleted worksites are
	 *                        excluded
	 */
	@Query("""
			SELECT DISTINCT w
			FROM Worksite w
			WHERE (LOWER(w.name) LIKE LOWER(CONCAT('%', :query, '%')) ESCAPE '!'
			   OR LOWER(w.code) LIKE LOWER(CONCAT('%', :query, '%')) ESCAPE '!'
			   OR (w.description IS NOT NULL
			    AND LOWER(w.description) LIKE LOWER(CONCAT('%', :query, '%')) ESCAPE '!')
			   OR (w.address IS NOT NULL
			    AND LOWER(w.address) LIKE LOWER(CONCAT('%', :query, '%')) ESCAPE '!'))
			  AND (:employeeNumber IS NULL
			   OR w.scope = es.nivel36.janus.service.worksite.WorksiteScope.GLOBAL
			   OR EXISTS (
			        SELECT 1
			        FROM w.employees e
			        WHERE e.employeeNumber = :employeeNumber
			   ))
			""")
	Page<Worksite> search(String query, String employeeNumber, Pageable pageable);

}
