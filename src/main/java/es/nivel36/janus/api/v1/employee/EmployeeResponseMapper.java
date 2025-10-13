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

import org.springframework.stereotype.Component;

import es.nivel36.janus.api.Mapper;
import es.nivel36.janus.service.employee.Employee;

/**
 * Maps {@link Employee} entities into {@link EmployeeResponse} DTOs.
 */
@Component
public class EmployeeResponseMapper implements Mapper<Employee, EmployeeResponse> {

	@Override
	public EmployeeResponse map(final Employee employee) {
		if (employee == null) {
			return null;
		}
		return new EmployeeResponse(employee.getName(), employee.getSurname(), employee.getEmail(), employee.getSchedule().getId());
	}

}
