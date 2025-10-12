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

import es.nivel36.janus.service.employee.Employee;

/**
 * Response DTO exposing the public representation of an {@link Employee}.
 *
 * @param name    the employee's first name
 * @param surname the employee's surname
 * @param email   the employee's unique email address
 * 
 */
public record EmployeeResponse(String name, String surname, String email) {
}
