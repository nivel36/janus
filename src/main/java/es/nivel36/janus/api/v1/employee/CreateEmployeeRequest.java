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
package es.nivel36.janus.api.v1.employee;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import es.nivel36.janus.service.employee.Employee;
import es.nivel36.janus.service.schedule.Schedule;

/**
 * Request payload for creating a new {@link Employee}.
 *
 * @param name       the employee's first name; optional but, if provided, must not
 *                   exceed 255 characters
 * @param surname    the employee's surname; optional but, if provided, must not
 *                   exceed 255 characters
 * @param email      the unique email address identifying the employee; must be a
 *                   valid email address and contain at most 254 characters
 * @param scheduleId the identifier of the {@link Schedule} assigned to the
 *                   employee; must be a positive number
 */
public record CreateEmployeeRequest(@Size(max = 255) String name, @Size(max = 255) String surname,
                @NotBlank @Email @Size(max = 254) String email, @NotNull @Positive Long scheduleId) {
}
