package es.nivel36.janus.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import es.nivel36.janus.security.Actor;
import es.nivel36.janus.service.ResourceNotFoundException;
import es.nivel36.janus.service.appuser.Role;
import es.nivel36.janus.service.employee.Employee;
import es.nivel36.janus.service.employee.EmployeeService;

class EmployeeNumberResolverTest {
	private final EmployeeService employees = mock(EmployeeService.class);
	private final EmployeeNumberResolver resolver = new EmployeeNumberResolver(this.employees);
	private final Actor actor = new Actor(UUID.randomUUID(), Set.of(Role.JANUS_EMPLOYEE), 84L);

	@Test
	void explicitFilterIsPreservedAndOnlyOmittedFiltersUseLinkedEmployee() {
		assertThat(this.resolver.effectiveNumber(this.actor, "EXPLICIT", true)).isEqualTo("EXPLICIT");
		assertThat(this.resolver.effectiveNumber(this.actor, null, false)).isNull();
		verifyNoInteractions(this.employees);
		final Employee employee = mock(Employee.class);
		when(this.employees.findEmployeeById(84L)).thenReturn(employee);
		when(employee.getEmployeeNumber()).thenReturn("OWN");
		assertThat(this.resolver.effectiveNumber(this.actor, null, true)).isEqualTo("OWN");
	}

	@Test
	void missingReferencesAreAbsenceButTechnicalFailuresPropagate() {
		when(this.employees.findEmployeeByEmployeeNumber("MISSING"))
				.thenThrow(new ResourceNotFoundException("missing"));
		assertThat(this.resolver.employeeId("MISSING")).isEmpty();
		assertThat(this.resolver.owns(this.actor, "MISSING")).isFalse();
		assertThatThrownBy(() -> this.resolver.requireEmployeeId("MISSING"))
				.isInstanceOf(ResourceNotFoundException.class);
		when(this.employees.findEmployeeByEmployeeNumber("BROKEN"))
				.thenThrow(new IllegalStateException("database unavailable"));
		assertThatThrownBy(() -> this.resolver.employeeId("BROKEN")).isInstanceOf(IllegalStateException.class);
	}
}
