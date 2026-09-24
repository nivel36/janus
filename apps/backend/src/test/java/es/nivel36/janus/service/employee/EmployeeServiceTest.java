/*
 * Copyright 2026 Abel Ferrer Jiménez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package es.nivel36.janus.service.employee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import es.nivel36.janus.service.schedule.ScheduleService;

class EmployeeServiceTest {

	private final EmployeeRepository repository = mock(EmployeeRepository.class);
	private final EmployeeService service = new EmployeeService(this.repository, mock(ScheduleService.class));

	@Test
	void findsEmployeeByEmail() {
		final Employee employee = mock(Employee.class);
		when(this.repository.findByEmail("person@example.test")).thenReturn(Optional.of(employee));

		assertThat(this.service.findEmployeeByEmail("person@example.test")).containsSame(employee);
	}

	@Test
	void validEmailWithoutMatchReturnsEmpty() {
		when(this.repository.findByEmail("missing@example.test")).thenReturn(Optional.empty());

		assertThat(this.service.findEmployeeByEmail("missing@example.test")).isEmpty();
	}

	@Test
	void rejectsNullOrBlankEmail() {
		assertThatThrownBy(() -> this.service.findEmployeeByEmail(null)).isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> this.service.findEmployeeByEmail("  ")).isInstanceOf(IllegalArgumentException.class);
	}
}
