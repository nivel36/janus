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
package es.nivel36.janus.service.employee;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import es.nivel36.janus.service.schedule.Schedule;
import es.nivel36.janus.service.timelog.TimeLog;
import es.nivel36.janus.service.workshift.WorkShift;

/**
 * Repository class for managing {@link Employee} entities.
 */
@Repository
interface EmployeeRepository extends CrudRepository<Employee, Long> {
	/**
	 * Searches employees using a literal fragment already escaped with
	 * LikePatterns. Text matches employee number, names and email
	 * case-insensitively; optional exact schedule and worksite filters combine with
	 * AND. EXISTS avoids duplicate employees with multiple worksite assignments.
	 * Schedules are loaded.
	 *
	 * @param  query        nonnull escaped text fragment; empty matches all text
	 * @param  scheduleCode optional exact schedule code
	 * @param  worksiteCode optional exact worksite code
	 * @param  pageable     validated, bounded page and persistence sort fields
	 * @return              matching page including its total count
	 */
	@EntityGraph(attributePaths = "schedule")
	@Query("""
			SELECT e
			FROM Employee e
			WHERE (LOWER(e.employeeNumber) LIKE LOWER(CONCAT('%', :query, '%')) ESCAPE '!'
			   OR LOWER(e.name) LIKE LOWER(CONCAT('%', :query, '%')) ESCAPE '!'
			   OR LOWER(e.surname) LIKE LOWER(CONCAT('%', :query, '%')) ESCAPE '!'
			   OR LOWER(e.email) LIKE LOWER(CONCAT('%', :query, '%')) ESCAPE '!')
			  AND (:scheduleCode IS NULL OR e.schedule.code = :scheduleCode)
			  AND (:worksiteCode IS NULL OR EXISTS (
			       SELECT 1 FROM e.worksites w WHERE w.code = :worksiteCode
			  ))
			""")
	Page<Employee> search(String query, String scheduleCode, String worksiteCode, Pageable pageable);

	@EntityGraph(attributePaths = "schedule")
	Employee findByEmployeeNumber(String employeeNumber);

	boolean existsByEmployeeNumber(String employeeNumber);

	@Query("""
			SELECT COUNT(DISTINCT e.id)
			FROM Employee e
			JOIN e.worksites w
			WHERE w.code = :worksiteCode
			""")
	long countByWorksiteCode(String worksiteCode);

	@Query("""
			SELECT COUNT(DISTINCT t.employee.id)
			FROM TimeLog t
			WHERE t.worksite.code = :worksiteCode
			AND t.entryTime >= :start
			AND t.entryTime < :end
			""")
	long countDistinctEmployeesWithTimeLogsInRange(String worksiteCode, Instant start, Instant end);

	@Query("""
			SELECT COUNT(t.id)
			FROM TimeLog t
			WHERE t.worksite.code = :worksiteCode
			AND t.entryTime >= :start
			AND t.entryTime < :end
			""")
	long countTimeLogsInRange(String worksiteCode, Instant start, Instant end);

	@Query("""
			SELECT COUNT(t.id)
			FROM TimeLog t
			WHERE t.worksite.code = :worksiteCode
			AND t.entryTime >= :start
			AND t.entryTime < :end
			AND t.exitTime IS NULL
			""")
	long countOpenTimeLogsInRange(String worksiteCode, Instant start, Instant end);

	@Query("""
			SELECT COUNT(DISTINCT t.employee.schedule.id)
			FROM TimeLog t
			WHERE t.worksite.code = :worksiteCode
			AND t.entryTime >= :start
			AND t.entryTime < :end
			""")
	long countDistinctSchedulesInRange(String worksiteCode, Instant start, Instant end);

	/**
	 * Checks whether a {@link Employee} exists for the specified email.
	 *
	 * @param  email the email to check for
	 * @return       {@code true} if the employee with the specified email exists,
	 *               or {@code false} if no employee is found
	 */
	boolean existsByEmail(final String email);

	/**
	 * Finds an {@link Employee} by email.
	 *
	 * @param  email the email of the employee to find
	 * @return       the employee with the specified email, or
	 *               {@link Optional#empty()} if no employee is found
	 */
	@EntityGraph(attributePaths = "schedule")
	Optional<Employee> findByEmail(final String email);

	@EntityGraph(attributePaths = "schedule")
	@Query("""
			SELECT e
			FROM Employee e
			JOIN e.appUser u
			WHERE u.keycloakSubject = :keycloakSubject
			""")
	Optional<Employee> findByKeycloakSubject(String keycloakSubject);

	/**
	 * Finds the IDs of employees who have at least one {@link TimeLog} entry since
	 * the given instant that is not associated with any {@link WorkShift}.
	 * <p>
	 * A time log is considered "not associated" when its {@code workshift_id}
	 * foreign key is {@code null} and it has a non-null exit time. Only time logs
	 * whose {@code entryTime} is greater than or equal to the given instant are
	 * considered. The query returns distinct employee IDs.
	 * </p>
	 *
	 * @param  start the lower bound instant; only time logs with {@code entryTime}
	 *               greater than or equal to this value are considered
	 * @return       a list of unique employee IDs corresponding to employees with
	 *               at least one unlinked time log since the given instant
	 */
	@Query(value = """
			SELECT DISTINCT t.employee_id
			FROM time_log t
			WHERE t.deleted = false
			AND t.entry_time >= :start
			AND t.exit_time IS NOT NULL
			AND t.workshift_id IS NULL;
			""", nativeQuery = true)
	List<Long> findWithoutWorkshiftsSince(Instant start);

	/**
	 * Determines whether an employee identified by its internal persistence ID is
	 * assigned to a schedule with the specified business code.
	 * <p>
	 * This method checks for the existence of an {@link Employee} whose internal
	 * identifier ({@code id}) matches the provided value and whose associated
	 * {@link Schedule} has the given {@code code}. The comparison is performed at
	 * the persistence layer without loading full entities into memory.
	 * </p>
	 * <p>
	 * The employee id is an internal key and the schedule code is a business
	 * identifier. The method returns {@code true} as soon as a matching assignment
	 * is found.
	 * </p>
	 *
	 * @param  employeeId   the internal id of the employee; must not be
	 *                      {@code null}
	 * @param  scheduleCode the business code of the schedule; must not be
	 *                      {@code null}
	 * @return              {@code true} if the employee is assigned to the
	 *                      specified schedule; {@code false} otherwise
	 */
	boolean existsByIdAndSchedule_Code(Long employeeId, String scheduleCode);

	/**
	 * Indicates whether an employee identified by its internal persistence ID is
	 * assigned to a worksite identified by the given code.
	 *
	 * @param  employeeId   the internal employee ID
	 * @param  worksiteCode the worksite code
	 * @return              {@code true} if the employee is assigned to the
	 *                      worksite; {@code false} otherwise
	 */
	boolean existsByIdAndWorksites_Code(Long employeeId, String worksiteCode);
}
