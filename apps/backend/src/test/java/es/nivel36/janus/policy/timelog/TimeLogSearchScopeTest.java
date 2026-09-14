/*
 * Copyright 2026 Abel Ferrer Jiménez
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package es.nivel36.janus.policy.timelog;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import es.nivel36.janus.service.timelog.TimeLogSearchScope;

class TimeLogSearchScopeTest {

	@Test
	void rejectsMissingEmployeeIdentifier() {
		assertThatThrownBy(() -> new TimeLogSearchScope.Employee(null)).isInstanceOf(NullPointerException.class);
	}

	@ParameterizedTest
	@ValueSource(longs = { 0L, -1L })
	void rejectsNonpersistentEmployeeIdentifiers(final long employeeId) {
		assertThatThrownBy(() -> new TimeLogSearchScope.Employee(employeeId)).isInstanceOf(IllegalArgumentException.class);
	}
}
