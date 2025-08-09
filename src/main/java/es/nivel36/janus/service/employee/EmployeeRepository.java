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

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository class for managing {@link Employee} entities.
 */
@Repository
interface EmployeeRepository extends CrudRepository<Employee, Long> {

	/**
	 * Finds an {@link Employee} by email.
	 *
	 * @param email the email of the employee to find
	 * @return the employee with the specified email, or {@code null} if no employee
	 *         is found
	 */
	Employee findEmployeeByEmail(final String email);
}
