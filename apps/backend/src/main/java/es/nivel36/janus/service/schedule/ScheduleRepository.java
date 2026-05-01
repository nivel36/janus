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
package es.nivel36.janus.service.schedule;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import es.nivel36.janus.service.employee.Employee;

/**
 * Repository interface responsible for interacting with the persistence layer
 * to manage {@link Schedule} entities.
 *
 * <p>This repository extends {@link JpaRepository}, providing standard CRUD
 * operations as well as custom query methods for retrieving schedules and
 * related data such as {@link TimeRange} and employee associations.</p>
 *
 * <p>It includes optimized queries using {@link Query} and {@link EntityGraph}
 * to efficiently fetch related entities and enforce business constraints
 * defined at the persistence level.</p>
 */
@Repository
interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    /**
     * Finds a {@link Schedule} entity by its unique code.
     *
     * <p>The associated {@code rules} and their {@code dayOfWeekRanges} are
     * eagerly loaded using an {@link EntityGraph}.</p>
     *
     * @param code the unique identifier code of the schedule; must not be
     *             {@code null}
     * @return the {@link Schedule} entity with the given code, or {@code null}
     *         if no such schedule exists
     */
    @EntityGraph(attributePaths = { "rules", "rules.dayOfWeekRanges" })
    Schedule findByCode(String code);
    

    /**
     * Checks whether a {@link Schedule} with the specified code exists.
     *
     * @param code the unique identifier code of the schedule; must not be
     *             {@code null}
     * @return {@code true} if a schedule with the given code exists;
     *         {@code false} otherwise
     */
    boolean existsByCode(String code);

    /**
     * Retrieves the {@link TimeRange} for a given {@link Employee} on a specific
     * {@link LocalDate}, considering only the shift that starts on that date.
     *
     * <p>Business rules assumed for this query:</p>
     * <ul>
     * <li>Within a {@link Schedule}, {@link ScheduleRule} date ranges never
     * overlap.</li>
     * <li>Each {@link ScheduleRule} can have at most one
     * {@link DayOfWeekTimeRange} starting on a given {@link DayOfWeek}.</li>
     * <li>Night shifts starting on the previous day and extending past midnight
     * are not considered.</li>
     * </ul>
     *
     * <p>These constraints guarantee that at most one matching
     * {@link TimeRange} exists for a given date.</p>
     *
     * @param employeeEmail the email of the employee whose time range is to be
     *                      retrieved; must not be {@code null}
     * @param date          the date for which the time range is to be retrieved;
     *                      must not be {@code null}
     * @param dayOfWeek     the {@link DayOfWeek} corresponding to {@code date};
     *                      must not be {@code null}
     * @return an {@link Optional} containing the {@link TimeRange} if one starts
     *         on that date, or an empty {@link Optional} if no shift starts on
     *         that date
     * @throws NullPointerException if any parameter is {@code null}
     */
    @Query("""
            SELECT d.timeRange
            FROM Employee e
            JOIN e.schedule s
            JOIN s.rules r
            JOIN r.dayOfWeekRanges d
            WHERE e.email = :employeeEmail
              AND (r.startDate IS NULL OR r.startDate <= :date)
              AND (r.endDate   IS NULL OR r.endDate   >= :date)
              AND d.dayOfWeek = :dayOfWeek
            """)
    Optional<TimeRange> findTimeRangeForDate(String employeeEmail, LocalDate date, DayOfWeek dayOfWeek);

    /**
     * Checks whether the {@link Schedule} has any associated {@link Employee}
     * entities.
     *
     * <p>This method uses the JPQL {@code size()} function to determine if the
     * {@code employees} collection is non-empty without loading it into memory.</p>
     *
     * @param code the schedule code to inspect; must not be {@code null}
     * @return {@code true} if at least one employee is assigned to the schedule;
     *         {@code false} otherwise
     */
    @Query("""
            SELECT (SIZE(s.employees) > 0)
            FROM Schedule s
            WHERE s.code = :code
            """)
    boolean hasEmployees(String code);

    /**
     * Searches for {@link Schedule} entities matching the given query string and
     * optionally filters by employee association.
     *
     * <p>The search is performed against the {@code name} and {@code code}
     * fields using a case-insensitive {@code LIKE} comparison.</p>
     *
     * <p>If {@code employeeEmail} is provided, only schedules associated with
     * the specified {@link Employee} are returned. Otherwise, all matching
     * schedules are included.</p>
     *
     * @param query          the search term to match against schedule name and
     *                       code; must not be {@code null}
     * @param employeeEmail  the email of the employee used to filter schedules;
     *                       may be {@code null}
     * @param pageable       the pagination information; must not be
     *                       {@code null}
     * @return a {@link Page} of {@link Schedule} entities matching the criteria;
     *         never {@code null}
     */
    @Query("""
            SELECT DISTINCT s
            FROM Schedule s
            JOIN s.rules r
            JOIN r.dayOfWeekRanges d
            WHERE (LOWER(s.name) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(s.code) LIKE LOWER(CONCAT('%', :query, '%')))
              AND (:employeeEmail IS NULL
                OR EXISTS (
                    SELECT 1
                    FROM s.employees e
                    WHERE e.email = :employeeEmail
                ))
            """)
    Page<Schedule> search(String query, String employeeEmail, Pageable pageable);
    
    @EntityGraph(attributePaths = { "rules", "rules.dayOfWeekRanges" })
    Page<Schedule> findAll(Pageable pageable);
}
