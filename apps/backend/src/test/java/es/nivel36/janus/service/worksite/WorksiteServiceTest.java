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
package es.nivel36.janus.service.worksite;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import es.nivel36.janus.service.employee.Employee;
import es.nivel36.janus.service.employee.EmployeeService;
import es.nivel36.janus.service.schedule.Schedule;

@ExtendWith(MockitoExtension.class)
class WorksiteServiceTest {

	private @Mock WorksiteRepository worksiteRepository;
	private @Mock EmployeeService employeeService;
	private WorksiteService worksiteService;

	@BeforeEach
	void setUp() {
		this.worksiteService = new WorksiteService(this.worksiteRepository, this.employeeService);
	}

	@Test
	void assertEmployeeCanUseWorksiteShouldAllowAssignedScopeWhenEmployeeIsAssigned() {
		final Schedule schedule = new Schedule("STD-WH", "Standard Work Hours");
		final Employee employee = new Employee("Abel", "Ferrer", "aferrer@nivel36.es", schedule);
		final Worksite worksite = new Worksite("BCN-PROJ", "Barcelona Project Site", ZoneId.of("UTC+2"),
				WorksiteScope.ASSIGNED);
		when(this.employeeService.isAssignedToWorksite("aferrer@nivel36.es", "BCN-PROJ")).thenReturn(true);

		assertDoesNotThrow(() -> this.worksiteService.assertEmployeeCanUseWorksite(employee, worksite));
		verify(this.employeeService).isAssignedToWorksite("aferrer@nivel36.es", "BCN-PROJ");
	}

	@Test
	void assertEmployeeCanUseWorksiteShouldRejectAssignedScopeWhenEmployeeIsNotAssigned() {
		final Schedule schedule = new Schedule("STD-WH", "Standard Work Hours");
		final Employee employee = new Employee("Abel", "Ferrer", "aferrer@nivel36.es", schedule);
		final Worksite worksite = new Worksite("BCN-PROJ", "Barcelona Project Site", ZoneId.of("UTC+2"),
				WorksiteScope.ASSIGNED);
		when(this.employeeService.isAssignedToWorksite("aferrer@nivel36.es", "BCN-PROJ")).thenReturn(false);

		assertThrows(WorksiteAccessDeniedException.class,
				() -> this.worksiteService.assertEmployeeCanUseWorksite(employee, worksite));
		verify(this.employeeService).isAssignedToWorksite("aferrer@nivel36.es", "BCN-PROJ");
	}
}
