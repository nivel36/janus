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

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import es.nivel36.janus.service.employee.Employee;

/**
 * Repository class responsible for interacting with the persistence layer to
 * manage schedule-related data.
 */
@Repository
interface ScheduleRepository extends JpaRepository<Schedule, Long> {

	/**
	 * Finds a {@link Schedule} entity by its unique code.
	 *
	 * @param code the unique identifier code of the schedule; must not be
	 *             {@code null}
	 * @return the {@link Schedule} entity with the given code, or {@code null} if
	 *         no such schedule exists
	 */
	@EntityGraph(attributePaths = { "rules", "rules.dayOfWeekRanges" })
	Schedule findByCode(String code);

	/**
	 * Checks whether a {@link Schedule} with the specified code exists.
	 *
	 * @param code the unique identifier code of the schedule; must not be
	 *             {@code null}
	 * @return {@code true} if a schedule with the given code exists; {@code false}
	 *         otherwise
	 */
	boolean existsByCode(String code);

	/**
	 * Retrieves the {@link TimeRange} for a given {@link Employee} on a specific
	 * {@link LocalDate}, considering only the shift that starts on that date.
	 * <p>
	 * Business rules assumed for this query:
	 * <ul>
	 * <li>Within a {@link Schedule}, {@link ScheduleRule} date ranges never
	 * overlap. The end date of one rule is followed by the start date of the next
	 * (if any) on a later day.</li>
	 * <li>Each {@link ScheduleRule} can have at most one {@link DayOfWeekTimeRange}
	 * starting on a given day of the week.</li>
	 * <li>Night shifts that start on the previous day and extend past midnight into
	 * the given date are not considered; only shifts whose {@code dayOfWeek}
	 * matches the queried date are returned.</li>
	 * </ul>
	 * These invariants guarantee that at most one matching {@link TimeRange} can
	 * exist for any given date.
	 *
	 * @param employee  the employee whose time range is to be retrieved; must not
	 *                  be {@code null}.
	 * @param date      the date for which the time range is to be retrieved; must
	 *                  not be {@code null}.
	 * @param dayOfWeek the {@link DayOfWeek} corresponding to {@code date}; must
	 *                  not be {@code null}.
	 * @return an {@code Optional} containing the {@link TimeRange} if one starts on
	 *         that date, or an empty {@code Optional} if no shift starts on that
	 *         date.
	 * @throws NullPointerException if any of the parameters are {@code null}.
	 */
	@Query("""
			SELECT d.timeRange
			FROM Employee e
			JOIN e.schedule s
			JOIN s.rules r
			JOIN r.dayOfWeekRanges d
			WHERE e = :employee
			AND (r.startDate IS NULL OR r.startDate <= :date)
			AND (r.endDate   IS NULL OR r.endDate   >= :date)
			AND d.dayOfWeek = :dayOfWeek
			""")
	Optional<TimeRange> findTimeRangeForDate(Employee employee, LocalDate date, DayOfWeek dayOfWeek);

	/**
	 * Checks whether the {@link Schedule} has any associated {@link Employee}
	 * entities.
	 * <p>
	 * This method performs an existence check using the JPQL {@code size()}
	 * function on the {@code employees} collection without loading it into memory.
	 * </p>
	 *
	 * @param schedule the schedule to inspect; must not be {@code null}
	 * @return {@code true} if at least one employee is assigned to the schedule;
	 *         {@code false} otherwise
	 */
	@Query("""
			select (size(s.employees) > 0)
			from Schedule s
			where s = :schedule
			""")
	boolean hasEmployees(Schedule schedule);
}
