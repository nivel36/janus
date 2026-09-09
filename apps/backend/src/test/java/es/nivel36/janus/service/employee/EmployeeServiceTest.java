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
package es.nivel36.janus.service.employee;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import es.nivel36.janus.service.ResourceNotFoundException;
import es.nivel36.janus.service.schedule.Schedule;
import es.nivel36.janus.service.schedule.ScheduleService;

class EmployeeServiceTest {

	private @Mock EmployeeRepository employeeRepository;
	private @Mock ScheduleService scheduleService;
	private @InjectMocks EmployeeService employeeService;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
	}

	@Test
	void testFindEmployeeByKeycloakSubjectReturnsLinkedEmployee() {
		final Employee employee = new Employee("Abel", "Ferrer", "aferrer@nivel36.es",
				mock(Schedule.class));
		when(this.employeeRepository.findByKeycloakSubject("11111111-1111-4111-8111-111111111111"))
			.thenReturn(Optional.of(employee));

		assertEquals(employee,
				this.employeeService.findEmployeeByKeycloakSubject("11111111-1111-4111-8111-111111111111"));
		verify(this.employeeRepository).findByKeycloakSubject("11111111-1111-4111-8111-111111111111");
	}

	@Test
	void testFindEmployeeByKeycloakSubjectThrowsWhenNoEmployeeIsLinked() {
		when(this.employeeRepository.findByKeycloakSubject("11111111-1111-4111-8111-111111111111"))
			.thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class,
				() -> this.employeeService.findEmployeeByKeycloakSubject("11111111-1111-4111-8111-111111111111"));
	}
}
