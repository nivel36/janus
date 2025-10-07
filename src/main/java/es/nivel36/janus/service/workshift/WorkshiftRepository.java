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
package es.nivel36.janus.service.workshift;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import es.nivel36.janus.service.employee.Employee;

public interface WorkshiftRepository extends CrudRepository<WorkShift, Long> {

	WorkShift findByEmployeeAndDate(Employee employee, LocalDate date);
	
	@Query("""
			SELECT CASE WHEN COUNT(w) > 0 THEN true ELSE false END
			FROM WorkShift w
			WHERE w.employee = :employee
			AND w.date = :date
			""")
	boolean existsByEmployeeAndDate(Employee employee, LocalDate date);

	@Query("""
			SELECT w
			FROM WorkShift w
			WHERE w.employee = :employee
			AND w.date >= :fromInclusive
			AND w.date < :toExclusive
			""")
	Page<WorkShift> findByEmployeeAndRange(Employee employee, LocalDate fromInclusive, LocalDate toExclusive,
			Pageable pageable);
}
