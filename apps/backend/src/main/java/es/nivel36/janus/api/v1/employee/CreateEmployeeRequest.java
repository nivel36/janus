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
package es.nivel36.janus.api.v1.employee;

import es.nivel36.janus.service.employee.Employee;
import es.nivel36.janus.validation.EmployeeNumber;
import es.nivel36.janus.validation.ScheduleCode;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Request payload for creating a new {@link Employee}.
 *
 * @param name         the employee's first name; must not be blank and must be
 *                     between 1 and 255 characters using the allowed characters
 * @param surname      the employee's surname; must not be blank and must be
 *                     between 1 and 255 characters using the allowed characters
 * @param email        the unique email address identifying the employee; must
 *                     be a valid email address and contain at most 254
 *                     characters
 * @param scheduleCode the code of the schedule of the employee; must not be
 *                     blank and must be at most 50 characters
 */
public record CreateEmployeeRequest(
		@NotNull(message = "employeeNumber must not be null")
		@EmployeeNumber
		String employeeNumber,

		@NotBlank(message = "name must not be blank")
		@Pattern(regexp = "^[\\p{L} .,'-]{1,255}$", message = "name must contain only letters, spaces, dots, commas, apostrophes or hyphens (max 255)")
		String name,

		@NotBlank(message = "surname must not be blank")
		@Pattern(regexp = "^[\\p{L} .,'-]{1,255}$", message = "surname must contain only letters, spaces, dots, commas, apostrophes or hyphens (max 255)")
		String surname,

		@NotBlank(message = "name must not be blank")
		@Email
		String email,

		@NotBlank(message = "scheduleCode must not be blank")
		@ScheduleCode
		String scheduleCode) {
}
