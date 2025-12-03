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
package es.nivel36.janus.service.timelog;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import es.nivel36.janus.service.employee.Employee;
import es.nivel36.janus.service.worksite.Worksite;

/**
 * Repository class for managing {@link TimeLog} entities.
 */
@Repository
interface TimeLogRepository extends JpaRepository<TimeLog, Long> {

	/**
	 * Retrieves a paginated list of {@link TimeLog} records for the specified
	 * employee and work site whose {@code entryTime} falls within the given time
	 * range.
	 * <p>
	 * The {@code start} parameter is inclusive; records with
	 * {@code entryTime &gt;= start} are included.<br>
	 * The {@code end} parameter is exclusive; records with
	 * {@code entryTime &lt; end} are included.
	 *
	 * @param employee    the employee whose time logs are to be retrieved; must not
	 *                    be {@code null}
	 * @param worksite    the work site where the time log was recorded; must not be
	 *                    {@code null}
	 * @param fromInstant the inclusive lower bound of the time range; must not be
	 *                    {@code null}
	 * @param toInstant   the exclusive upper bound of the time range; must not be
	 *                    {@code null}
	 * @param page        pagination parameters including offset, size, and sort
	 *                    order; must not be {@code null}
	 */
	@EntityGraph(attributePaths = { "employee", "worksite" })
	@Query("""
			SELECT t FROM TimeLog t
			WHERE t.employee = :employee
			AND t.worksite = :worksite
			AND t.entryTime >= :start AND t.entryTime < :end
			""")
	Page<TimeLog> searchByEmployeeAndWorksiteAndEntryTimeInRange(Employee employee, Worksite worksite, Instant start,
			Instant end, Pageable page);

	/**
	 * Retrieves time logs for an {@link Employee} {@link TimeLog} records for the
	 * specified employee whose {@code entryTime} falls within the given time range.
	 * <p>
	 * The {@code start} parameter is inclusive; records with
	 * {@code entryTime &gt;= start} are included.<br>
	 * The {@code end} parameter is exclusive; records with
	 * {@code entryTime &lt; end} are included.
	 *
	 * @param employee    the employee whose time logs are to be retrieved
	 *                    (required)
	 * @param fromInstant the inclusive lower bound of the time range; must not be
	 *                    {@code null}
	 * @param toInstant   the exclusive upper bound of the time range; must not be
	 *                    {@code null}
	 * @param page        pagination parameters including offset, size, and sort
	 *                    order; must not be {@code null}
	 * @return a {@link Page} of {@link TimeLog} entries in the range for the given
	 *         employee
	 */
	@EntityGraph(attributePaths = { "employee", "worksite" })
	@Query("""
			SELECT t FROM TimeLog t
			WHERE t.employee = :employee
			AND t.entryTime >= :start AND t.entryTime < :end
			""")
	Page<TimeLog> searchByEmployeeAndEntryTimeInRange(Employee employee, Instant start, Instant end, Pageable page);

	/**
	 * Finds the most recent {@link TimeLog} for the specified employee and
	 * worksite, ordered by {@code entryTime} descending.
	 * <p>
	 * This method effectively returns the "last" clock-in record for the employee
	 * at a given worksite, if present.
	 *
	 * @param employee the employee whose last time log is to be found
	 * @param worksite the worksite to filter by
	 * @return an {@link Optional} containing the most recent time log, or empty if
	 *         none exist
	 */
	@EntityGraph(attributePaths = { "employee", "worksite" })
	TimeLog findTopByEmployeeAndWorksiteOrderByEntryTimeDesc(Employee employee, Worksite worksite);

	/**
	 * Finds the most recent {@link TimeLog} for the specified employee and worksite
	 * that has not been closed yet (i.e. {@code exitTime IS NULL}), ordered by
	 * {@code entryTime} descending.
	 *
	 * @param employee the employee whose last open time log is to be found
	 * @param worksite the worksite to filter by
	 * @return the most recent open time log, or {@code null} if none exist
	 */
	@EntityGraph(attributePaths = { "employee", "worksite" })
	TimeLog findTopByEmployeeAndWorksiteAndExitTimeIsNullOrderByEntryTimeDesc(Employee employee, Worksite worksite);

	/**
	 * Retrieves all {@link TimeLog} records for a given employee, with pagination.
	 * <p>
	 * The {@link Pageable} parameter defines offset, size, and sort order. Use this
	 * method to efficiently browse an employee’s full history of recorded time
	 * logs.
	 *
	 * @param employee the employee whose time logs are to be retrieved
	 * @param page     pagination parameters including offset, size, and sort order
	 * @return a {@link Page} of time logs belonging to the specified employee
	 */
	@EntityGraph(attributePaths = { "employee", "worksite" })
	@Query("""
			SELECT t
			FROM TimeLog t
			WHERE t.employee = :employee
			""")
	Page<TimeLog> searchTimeLogsByEmployee(@Param("employee") Employee employee, Pageable page);

	/**
	 * Retrieves a single {@link TimeLog} for the specified employee that exactly
	 * matches the provided {@code entryTime}.
	 *
	 * @param employee  the employee whose time log is to be retrieved
	 * @param entryTime the exact entry timestamp of the record
	 * @return an {@link Optional} containing the matching time log, or empty if not
	 *         found
	 */
	@EntityGraph(attributePaths = { "employee", "worksite" })
	TimeLog findByEmployeeAndEntryTime(Employee employee, Instant entryTime);

	/**
	 * Checks whether a {@link TimeLog} exists for the specified employee and exact
	 * {@code entryTime}.
	 *
	 * @param employee  the employee to check for
	 * @param entryTime the exact entry timestamp to check
	 * @return {@code true} if a record exists for the given employee and entry
	 *         time; {@code false} otherwise
	 */
	boolean existsByEmployeeAndEntryTimeAndDeletedFalse(Employee employee, Instant entryTime);

	/**
	 * Returns the list of {@link TimeLog} records for the given employee that are
	 * considered "orphans" since the specified instant; i.e., time logs that are
	 * not linked to any {@link WorkShift} through the {@code workshift_timelog}
	 * join table.
	 * <p>
	 * The result is returned with the associated {@link Employee} and
	 * {@link Worksite} eagerly loaded (via <em>fetch join</em>) to prevent
	 * lazy-loading overhead and N+1 queries.
	 * </p>
	 * <p>
	 * Selection rules:
	 * <ul>
	 * <li>Only non-deleted time logs are considered
	 * ({@code t.deleted = false}).</li>
	 * <li>Only time logs with {@code entryTime >= :from} are included.</li>
	 * <li>Only time logs of the specified employee are included.</li>
	 * <li>A time log is "orphan" when no {@link WorkShift} exists such that the
	 * time log is a {@code member} of its {@code timeLogs} collection.</li>
	 * <li>Results are ordered by {@code entryTime} in descending order (most recent
	 * first).</li>
	 * </ul>
	 * </p>
	 *
	 * @param from     lower bound (inclusive) for {@code entryTime}
	 * @param employee the employee whose orphan time logs will be returned
	 * @return a list of orphan {@link TimeLog} entities (with {@link Employee} and
	 *         {@link Worksite} initialized) since {@code from}, ordered most recent
	 *         first
	 */
	@Query("""
			SELECT t
			FROM TimeLog t
			JOIN FETCH t.employee e
			JOIN FETCH t.worksite ws
			WHERE t.deleted = false
			AND t.entryTime >= :from
			AND e = :employee
			AND NOT EXISTS (
			SELECT 1
			FROM WorkShift w
			WHERE t MEMBER OF w.timeLogs
			)
			ORDER BY t.entryTime DESC
			""")
	List<TimeLog> findOrphanTimeLogsSince(@Param("from") Instant from, @Param("employee") Employee employee);

}