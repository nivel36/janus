/*
 * Copyright 2026 Abel Ferrer Jiménez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package es.nivel36.janus.api.v1.timelog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import es.nivel36.janus.service.employee.Employee;
import es.nivel36.janus.service.timelog.ClockOutWithoutClockInEvent;
import es.nivel36.janus.service.worksite.Worksite;

class ClockOutWithoutClockInEventResponseMapperTest {

	@Test
	void mapsStableEmployeeNumberFromEmployee() {
		final var employee = mock(Employee.class);
		final var worksite = mock(Worksite.class);
		final var event = mock(ClockOutWithoutClockInEvent.class);
		when(employee.getEmployeeNumber()).thenReturn("EMP-0042");
		when(event.getEmployee()).thenReturn(employee);
		when(event.getWorksite()).thenReturn(worksite);

		final var response = new ClockOutWithoutClockInEventResponseMapper().map(event);

		assertThat(response.employeeNumber()).isEqualTo("EMP-0042");
	}
}
