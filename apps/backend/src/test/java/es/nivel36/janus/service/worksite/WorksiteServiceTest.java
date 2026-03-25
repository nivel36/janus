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

	@Mock
	private WorksiteRepository worksiteRepository;

	@Mock
	private EmployeeService employeeService;

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
				WorksiteScope.ASSIGNED, null);
		when(this.employeeService.isAssignedToWorksite("aferrer@nivel36.es", "BCN-PROJ")).thenReturn(true);

		assertDoesNotThrow(() -> this.worksiteService.assertEmployeeCanUseWorksite(employee, worksite));
		verify(this.employeeService).isAssignedToWorksite("aferrer@nivel36.es", "BCN-PROJ");
	}

	@Test
	void assertEmployeeCanUseWorksiteShouldRejectAssignedScopeWhenEmployeeIsNotAssigned() {
		final Schedule schedule = new Schedule("STD-WH", "Standard Work Hours");
		final Employee employee = new Employee("Abel", "Ferrer", "aferrer@nivel36.es", schedule);
		final Worksite worksite = new Worksite("BCN-PROJ", "Barcelona Project Site", ZoneId.of("UTC+2"),
				WorksiteScope.ASSIGNED, null);
		when(this.employeeService.isAssignedToWorksite("aferrer@nivel36.es", "BCN-PROJ")).thenReturn(false);

		assertThrows(WorksiteAccessDeniedException.class,
				() -> this.worksiteService.assertEmployeeCanUseWorksite(employee, worksite));
		verify(this.employeeService).isAssignedToWorksite("aferrer@nivel36.es", "BCN-PROJ");
	}
}
