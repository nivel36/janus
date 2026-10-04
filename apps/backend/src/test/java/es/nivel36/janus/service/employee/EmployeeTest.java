/*
 * Copyright 2026 Abel Ferrer Jiménez Licensed under the Apache License, Version
 * 2.0 (the "License");
 */
package es.nivel36.janus.service.employee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

import es.nivel36.janus.service.schedule.Schedule;

class EmployeeTest {

	@Test
	void identityIsBasedOnImmutableEmployeeNumber() {
		final Schedule schedule = mock(Schedule.class);
		final Employee first = new Employee("EMP-0042", "Ada", "Lovelace", "ada@old.test", schedule);
		final Employee sameIdentity = new Employee("EMP-0042", "Ada", "Lovelace", "ada@new.test", schedule);

		first.changeEmail("ada@changed.test");

		assertThat(first).isEqualTo(sameIdentity).hasSameHashCodeAs(sameIdentity);
		assertThat(first.toString()).isEqualTo("EMP-0042");
		assertThat(first.getEmail()).isEqualTo("ada@changed.test");
	}

	@Test
	void employeeNumberAndChangedEmailMustNotBeBlank() {
		final Schedule schedule = mock(Schedule.class);
		assertThatThrownBy(() -> new Employee(" ", "Ada", "Lovelace", "ada@example.test", schedule))
				.isInstanceOf(IllegalArgumentException.class);
		final Employee employee = new Employee("EMP-0042", "Ada", "Lovelace", "ada@example.test", schedule);
		assertThatThrownBy(() -> employee.changeEmail(" ")).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void emailIsValidatedAndCanonicalizedInsideTheAggregate() {
		final Schedule schedule = mock(Schedule.class);
		final Employee employee = new Employee("EMP-0042", "Ada", "Lovelace", " ADA@EXAMPLE.TEST ", schedule);

		assertThat(employee.getEmail()).isEqualTo("ada@example.test");
		assertThatThrownBy(() -> employee.changeEmail("invalid")).isInstanceOf(IllegalArgumentException.class);
		assertThat(employee.getEmail()).isEqualTo("ada@example.test");
	}
}
