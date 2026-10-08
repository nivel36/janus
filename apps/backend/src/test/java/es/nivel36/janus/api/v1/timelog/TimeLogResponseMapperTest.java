/*
 * Copyright 2026 Abel Ferrer Jiménez
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package es.nivel36.janus.api.v1.timelog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import es.nivel36.janus.api.Mapper;
import es.nivel36.janus.service.employee.Employee;
import es.nivel36.janus.service.timelog.TimeLog;
import es.nivel36.janus.service.worksite.Worksite;

class TimeLogResponseMapperTest {

	@Test
	void mapsStableEmployeeNumberFromEmployee() {
		final Employee employee = mock(Employee.class);
		final Worksite worksite = mock(Worksite.class);
		final TimeLog timeLog = mock(TimeLog.class);
		@SuppressWarnings("unchecked")
		final Mapper<Duration, DurationResponse> durationMapper = mock(Mapper.class);
		when(employee.getEmployeeNumber()).thenReturn("EMP-0042");
		when(timeLog.getEmployee()).thenReturn(employee);
		when(timeLog.getWorksite()).thenReturn(worksite);

		final TimeLogResponse response = new TimeLogResponseMapper(durationMapper).map(timeLog);

		assertThat(response.employeeNumber()).isEqualTo("EMP-0042");
	}
}
