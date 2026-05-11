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
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing {@link Worksite} entities.
 *
 * <p>
 * Provides data access operations for {@link Worksite}, including lookup by code,
 * existence checks, employee association validation, and advanced search with
 * optional filtering based on employee visibility.
 * </p>
 *
 * <p>
 * This repository extends {@link JpaRepository}, inheriting standard CRUD
 * operations such as save, delete, and find by identifier, as well as pagination
 * and sorting capabilities.
 * </p>
 *
 * <p>
 * Example usage:
 * <pre>
 * {@code
 * Optional<Worksite> worksite = repository.findById(1L);
 * boolean exists = repository.existsByCode("WS-001");
 * Page<Worksite> results = repository.search("central", null, PageRequest.of(0, 10));
 * }
 * </pre>
 * </p>
 */
@Repository
interface WorksiteRepository extends JpaRepository<Worksite, Long> {

    /**
     * Retrieves a {@link Worksite} by its unique code.
     *
     * @param code the unique identifier of the worksite. Can't be {@code null}.
     * @return the {@link Worksite} associated with the given code, or {@code null}
     *         if no worksite matches the provided code.
     */
    Worksite findByCode(String code);

    /**
     * Checks whether a {@link Worksite} exists with the specified code.
     *
     * @param code the unique identifier of the worksite. Can't be {@code null}.
     * @return {@code true} if a worksite exists with the given code;
     *         {@code false} otherwise.
     */
    boolean existsByCode(String code);

    /**
     * Determines whether the {@link Worksite} identified by the given code
     * has at least one associated employee.
     *
     * @param worksiteCode the unique code of the worksite. Can't be {@code null}.
     * @return {@code true} if the worksite has one or more associated employees;
     *         {@code false} otherwise.
     */
    @Query("""
            SELECT (SIZE(w.employees) > 0)
            FROM Worksite w
            WHERE w.code = :worksiteCode
            """)
    boolean hasEmployees(String worksiteCode);

    /**
     * Searches for {@link Worksite} entities whose name, code, description or address matches
     * the given query string, with optional filtering based on employee visibility.
     *
     * <p>
     * A worksite is included in the result if:
     * <ul>
     *   <li>Its name or code contains the provided query string (case-insensitive).</li>
     *   <li>And one of the following conditions is met:
     *     <ul>
     *       <li>{@code employeeEmail} is {@code null}.</li>
     *       <li>The worksite scope is {@code GLOBAL}.</li>
     *       <li>The worksite has an associated employee with the given email.</li>
     *     </ul>
     *   </li>
     * </ul>
     * </p>
     *
     * @param query the search text to match against worksite name, code, description or address.
     *              Can't be {@code null}.
     * @param employeeEmail the email of the employee used to filter visible worksites.
     *                      Can be {@code null}.
     * @param pageable the pagination information. Can't be {@code null}.
     * @return a {@link Page} of {@link Worksite} instances matching the criteria;
     *         never {@code null}.
     */
    @Query("""
            SELECT DISTINCT w
            FROM Worksite w
            WHERE (LOWER(w.name) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(w.code) LIKE LOWER(CONCAT('%', :query, '%'))
               OR (w.description IS NOT NULL
                AND LOWER(w.description) LIKE LOWER(CONCAT('%', :query, '%')))
               OR (w.address IS NOT NULL
                AND LOWER(w.address) LIKE LOWER(CONCAT('%', :query, '%'))))
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
