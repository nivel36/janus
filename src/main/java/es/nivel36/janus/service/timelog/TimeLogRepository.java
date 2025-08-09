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

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import es.nivel36.janus.service.employee.Employee;

/**
 * Repository class for managing {@link TimeLog} entities.
 */
@Repository
interface TimeLogRepository extends CrudRepository<TimeLog, Long> {
	
	/**
	 * Finds the last {@link TimeLog} for the specified employee.
	 *
	 * @param employee the employee whose last time log is to be found
	 * @return an {@link Optional} containing the last time log of the employee if
	 *         present
	 */
	Optional<TimeLog> findLastTimeLogByEmployee(final Employee employee);

	/**
	 * Retrieves all {@link TimeLog} entries for a given employee within a specified
	 * date range.
	 *
	 * @param employee  the employee whose time logs are to be retrieved
	 * @param startDate the start date of the range (inclusive)
	 * @param endDate   the end date of the range (inclusive)
	 * @param page      the page on which to search for the elements. It includes
	 *                  both the offset and the size and order.
	 * @return a page of time logs for the specified employee within the date range
	 */
	@Query("""
			SELECT t 
			FROM TimeLog t 
			WHERE t.employee = :employee 
			AND t.entryTime BETWEEN :startDate 
			AND :endDate
			ORDER BY t.entryTime ASC
			""")
	Page<TimeLog> findTimeLogsByEmployeeAndDateRange(final Employee employee, final LocalDateTime startDate,
			final LocalDateTime endDate, final Pageable page);

	/**
	 * Retrieves all {@link TimeLog} entries for a given employee, with pagination.
	 *
	 * @param employee the employee whose time logs are to be retrieved
	 * @param page     the page on which to search for the elements. It includes
	 *                 both the offset and the size and order.
	 * @return a page of time logs for the specified employee
	 */
	Page<TimeLog> findTimeLogsByEmployeeOrderByEntryTimeDesc(final Employee employee, final Pageable page);

}
