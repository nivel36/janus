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
import es.nivel36.janus.validation.ScheduleCode;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

/**
 * Request payload for updating an existing {@link Employee}.
 * <p>
 * The constraints below apply during Bean Validation. Construction alone stores
 * the supplied values without validation or normalization.
 *
 * @param name         the new first name of the employee; must not be blank and
 *                     must be between 1 and 255 characters drawn from letters,
 *                     spaces, dots, commas, apostrophes or hyphens
 * @param surname      the new surname of the employee; must not be blank and
 *                     must be between 1 and 255 characters drawn from letters,
 *                     spaces, dots, commas, apostrophes or hyphens
 * @param email        new contact email; valid, nonblank and at most 254
 *                     characters
 * @param scheduleCode the code of the new schedule of the employee; must not be
 *                     blank and must match {@code [A-Za-z0-9_-]{1,50}}
 */
public record UpdateEmployeeRequest(
		@NotBlank(message = "name must not be blank")
		@Pattern(regexp = "^[\\p{L} .,'-]{1,255}$", message = "name must contain only letters, spaces, dots, commas, apostrophes or hyphens (max 255)")
		String name,

		@NotBlank(message = "surname must not be blank")
		@Pattern(regexp = "^[\\p{L} .,'-]{1,255}$", message = "surname must contain only letters, spaces, dots, commas, apostrophes or hyphens (max 255)")
		String surname,

		@NotBlank(message = "email must not be blank")
		@Email
		@Size(max = 254, message = "email must not exceed 254 characters")
		String email,

		@NotBlank(message = "scheduleCode must not be blank")
		@ScheduleCode
		String scheduleCode) {
}
