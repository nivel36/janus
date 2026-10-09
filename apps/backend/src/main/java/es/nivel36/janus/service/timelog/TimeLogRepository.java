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
package es.nivel36.janus.service.timelog;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Repository class for managing {@link TimeLog} entities.
 */
@Repository
interface TimeLogRepository extends JpaRepository<TimeLog, Long>, JpaSpecificationExecutor<TimeLog> {

	/**
	 * {@inheritDoc}
	 * <p>
	 * Employee and worksite associations are loaded for the returned records.
	 */
	/**
	 * Applies one specification to both the records and their total, before
	 * pagination.
	 */
	@Override
	@EntityGraph(attributePaths = { "employee", "worksite" })
	Page<TimeLog> findAll(Specification<TimeLog> specification, Pageable page);

	/**
	 * Finds the most recent {@link TimeLog} for the specified employee that has not
	 * been closed yet (i.e. {@code exitTime IS NULL}), ordered by {@code entryTime}
	 * descending.
	 *
	 * @param  employeeId the persistent identifier of the employee
	 * @return            the most recent open time log, or {@code null} if none
	 *                    exist
	 */
	@EntityGraph(attributePaths = { "employee", "worksite" })
	TimeLog findTopByEmployeeIdAndExitTimeIsNullOrderByEntryTimeDesc(Long employeeId);

	/**
	 * Retrieves a single {@link TimeLog} for the specified employee that exactly
	 * matches the provided {@code entryTime}.
	 *
	 * @param  employeeId the internal id of the employee whose time log is to be
	 *                    retrieved
	 * @param  entryTime  the exact entry timestamp of the record
	 * @return            the matching active time log, or {@code null} if absent
	 */
	@EntityGraph(attributePaths = { "employee", "worksite" })
	TimeLog findByEmployeeIdAndEntryTime(Long employeeId, Instant entryTime);

	/**
	 * Deletes a time log by employee number and entry time through JPA, preserving
	 * its logical deletion mapping.
	 *
	 * @return number of matching time logs removed
	 */
	long deleteByEmployeeEmployeeNumberAndEntryTime(String employeeNumber, Instant entryTime);

	boolean existsByEmployeeEmployeeNumberAndEntryTime(String employeeNumber, Instant entryTime);

	/**
	 * Checks whether a {@link TimeLog} exists for the specified employee and exact
	 * {@code entryTime}.
	 *
	 * @param  employeeId the internal id of the employee to check for
	 * @param  entryTime  the exact entry timestamp to check
	 * @return            {@code true} if a record exists for the given employee and
	 *                    entry time; {@code false} otherwise
	 */
	boolean existsByEmployeeIdAndEntryTimeAndDeletedFalse(Long employeeId, Instant entryTime);

	/**
	 * Returns closed, nondeleted logs with no assigned shift, in descending
	 * entry-time order.
	 * <p>
	 * Employee and worksite associations are loaded with the result.
	 *
	 * @param  employeeId the persistent employee identifier
	 * @param  from       the inclusive entry-time lower bound
	 * @return            the eligible logs, most recent first, or an empty list if
	 *                    none match
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
	List<TimeLog> findOrphanTimeLogsSince(Long employeeId, Instant from);
}
