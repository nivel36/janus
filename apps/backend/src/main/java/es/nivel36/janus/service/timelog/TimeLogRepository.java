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
package es.nivel36.janus.service.timelog;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import es.nivel36.janus.service.employee.Employee;
import es.nivel36.janus.service.workshift.WorkShift;
import es.nivel36.janus.service.worksite.Worksite;

/**
 * Repository class for managing {@link TimeLog} entities.
 */
@Repository
interface TimeLogRepository extends JpaRepository<TimeLog, Long>, JpaSpecificationExecutor<TimeLog> {

	/** Applies one specification to both the records and their total, before pagination. */
	@Override
	@EntityGraph(attributePaths = { "employee", "worksite" })
	Page<TimeLog> findAll(Specification<TimeLog> specification, Pageable page);

	/**
	 * Finds the most recent {@link TimeLog} for the specified employee that has not
	 * been closed yet (i.e. {@code exitTime IS NULL}), ordered by {@code entryTime}
	 * descending.
	 *
	 * @param employee the employee whose last open time log is to be found
	 * @return the most recent open time log, or {@code null} if none exist
	 */
	@EntityGraph(attributePaths = { "employee", "worksite" })
	TimeLog findTopByEmployeeIdAndExitTimeIsNullOrderByEntryTimeDesc(Long employeeId);

	/**
	 * Retrieves a single {@link TimeLog} for the specified employee that exactly
	 * matches the provided {@code entryTime}.
	 *
	 * @param employeeId the internal id of the employee whose time log is to be
	 *                      retrieved
	 * @param entryTime     the exact entry timestamp of the record
	 * @return an {@link Optional} containing the matching time log, or empty if not
	 *         found
	 */
	@EntityGraph(attributePaths = { "employee", "worksite" })
	TimeLog findByEmployeeIdAndEntryTime(Long employeeId, Instant entryTime);

	/**
	 * Checks whether a {@link TimeLog} exists for the specified employee and exact
	 * {@code entryTime}.
	 *
	 * @param employeeId the internal id of the employee to check for
	 * @param entryTime     the exact entry timestamp to check
	 * @return {@code true} if a record exists for the given employee and entry
	 *         time; {@code false} otherwise
	 */
	boolean existsByEmployeeIdAndEntryTimeAndDeletedFalse(Long employeeId, Instant entryTime);

	/**
	 * Returns the list of {@link TimeLog} records for the given employee that are
	 * considered "orphans" since the specified instant; i.e., time logs that are
	 * not linked to any {@link WorkShift} (their {@code workShift} association is
	 * {@code null}).
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
	 * <li>A time log is "orphan" when it has no assigned {@link WorkShift}.</li>
	 * <li>Results are ordered by {@code entryTime} in descending order (most recent
	 * first).</li>
	 * </ul>
	 * </p>
	 *
	 * @param from          lower bound (inclusive) for {@code entryTime}
	 * @param employeeId the internal id of the employee whose orphan time logs will be
	 *                      returned
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
			AND t.exitTime IS NOT NULL
			AND e.id = :employeeId
			AND t.workShift IS NULL
			ORDER BY t.entryTime DESC
			""")
	List<TimeLog> findOrphanTimeLogsSince(Instant from, Long employeeId);
}
