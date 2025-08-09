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
package es.nivel36.janus.service.schedule;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import es.nivel36.janus.service.employee.Employee;

/**
 * Repository class responsible for interacting with the persistence layer to
 * manage schedule-related data.
 */
@Repository
interface ScheduleRepository extends CrudRepository<Schedule, Long> { 

	/**
	 * Finds the {@link TimeRange} for a given {@link Employee} on a specific
	 * {@link LocalDate}.
	 *
	 * @param employee the employee whose time range is to be found. Cannot be
	 *                 {@code null}.
	 * @param date     the date for which the time range is to be found. Cannot be
	 *                 {@code null}.
	 * @return an {@code Optional} containing the {@link TimeRange} if one is
	 *         defined, or an empty {@code Optional} if no time range exists.
	 * @throws NullPointerException if either {@code employee} or {@code date} is
	 *                              {@code null}.
	 */
	@Query("""
			SELECT d.timeRange
			FROM Employee e
			JOIN e.schedule s
			JOIN s.rules r
			JOIN r.dayOfWeekRanges d
			WHERE e = :employee
			AND :date BETWEEN r.startDate AND r.endDate
			AND d.dayOfWeek = :dayOfWeek
			""")
	Optional<TimeRange> findTimeRangeForEmployeeByDate(final Employee employee, final LocalDate date);
}
