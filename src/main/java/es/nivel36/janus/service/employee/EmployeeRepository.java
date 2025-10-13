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
package es.nivel36.janus.service.employee;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import es.nivel36.janus.service.timelog.TimeLog;
import es.nivel36.janus.service.workshift.WorkShift;

/**
 * Repository class for managing {@link Employee} entities.
 */
@Repository
interface EmployeeRepository extends CrudRepository<Employee, Long> {

	/**
	 * Checks whether a {@link Employee} exists for the specified email.
	 *
	 *
	 * @param email the email to check for
	 * @return the employee with the specified email, or {@code null} if no employee
	 *         is found
	 */
	boolean existsByEmail(final String email);

	/**
	 * Finds an {@link Employee} by email.
	 *
	 * @param email the email of the employee to find
	 * @return the employee with the specified email, or {@code null} if no employee
	 *         is found
	 */
	@EntityGraph(attributePaths = "schedule")
	Employee findByEmail(final String email);

	/**
	 * Finds the IDs of employees who have at least one {@link TimeLog} entry since
	 * the given instant but do not have any associated {@link WorkShift}.
	 * <p>
	 * A time log is considered "not associated" when it does not appear in the
	 * {@code workshift_timelog} join table. This query returns distinct employee
	 * IDs that satisfy this condition.
	 * </p>
	 *
	 * @param fromInclusive the lower bound instant; only time logs with
	 *                      {@code entryTime} greater than or equal to this value
	 *                      are considered
	 * @return a list of unique employee IDs corresponding to employees with time
	 *         logs since the given instant but without any linked work shifts
	 */
	@Query(value = """
			SELECT DISTINCT t.employee_id
			FROM time_log t
			WHERE t.deleted = false
			AND t.entry_time >= :fromInclusive
			AND NOT EXISTS (
			SELECT 1
			FROM workshift_timelog wstl
			WHERE wstl.timelog_id = t.id
			);
			""", nativeQuery = true)
	List<Long> findWithoutWorkshiftsSince(@Param("fromInclusive") Instant fromInclusive);
}
