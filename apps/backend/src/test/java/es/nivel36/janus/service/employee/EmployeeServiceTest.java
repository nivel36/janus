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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import es.nivel36.janus.service.ResourceAlreadyExistsException;
import es.nivel36.janus.service.ResourceNotFoundException;
import es.nivel36.janus.service.schedule.Schedule;

import es.nivel36.janus.service.schedule.ScheduleService;

class EmployeeServiceTest {

	private final EmployeeRepository repository = mock(EmployeeRepository.class);
	private final ScheduleService schedules = mock(ScheduleService.class);
	private final EmployeeService service = new EmployeeService(this.repository, this.schedules, 100);

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

	@Test
	void absentTextFilterUsesStableDefaultSortAndPreservesPage() {
		final Pageable normalized = PageRequest.of(3, 20, Sort.by("employeeNumber"));
		final Page<Employee> page = Page.empty(normalized);
		when(this.repository.search("", null, null, normalized)).thenReturn(page);
		assertThat(this.service.searchEmployees(null, null, null, PageRequest.of(3, 20))).isSameAs(page);
	}

	@Test
	void literalQueryIsEscapedWithoutTrimmingAndPageSizeIsCapped() {
		this.service.searchEmployees(" A%_! ", "DAY", "HQ", PageRequest.of(2, 500));
		verify(this.repository).search(" A!%!_!! ", "DAY", "HQ", PageRequest.of(2, 100, Sort.by("employeeNumber")));
	}

	@Test
	void scheduleSortIsTranslatedAndOrderOptionsArePreservedWithStableTieBreaker() {
		final Sort.Order order = Sort.Order.desc("scheduleCode").ignoreCase().nullsLast();
		this.service
				.searchEmployees(null, null, null, PageRequest.of(0, 10, Sort.by(order, Sort.Order.asc("surname"))));
		final ArgumentCaptor<Pageable> request = ArgumentCaptor.forClass(Pageable.class);
		verify(this.repository).search(eq(""), isNull(), isNull(), request.capture());
		assertThat(request.getValue().getSort()).containsExactly(
				order.withProperty("schedule.code"),
				Sort.Order.asc("surname"),
				Sort.Order.asc("employeeNumber"));
	}

	@Test
	void explicitEmployeeNumberSortKeepsDirectionAndPositionWithoutDuplicateTieBreaker() {
		final Pageable request = PageRequest
				.of(0, 10, Sort.by(Sort.Order.desc("employeeNumber"), Sort.Order.asc("name")));
		this.service.searchEmployees(null, null, null, request);
		verify(this.repository).search("", null, null, request);
	}

	@ParameterizedTest
	@ValueSource(strings = { "id", "schedule.code", "appUser.email", "timeLogs", "unknown" })
	void internalAndUnknownSortFieldsAreRejectedBeforeSearching(final String field) {
		assertThatThrownBy(() -> this.service.searchEmployees(null, null, null, PageRequest.of(0, 10, Sort.by(field))))
				.isInstanceOf(IllegalArgumentException.class);
		verifyNoInteractions(this.repository, this.schedules);
	}

	@Test
	void unpagedSearchIsRejectedBeforeSearching() {
		assertThatThrownBy(() -> this.service.searchEmployees(null, null, null, Pageable.unpaged()))
				.isInstanceOf(IllegalArgumentException.class);
		verifyNoInteractions(this.repository, this.schedules);
	}

	@Test
	void configuredPageSizeLimitIsUsed() {
		new EmployeeService(this.repository, this.schedules, 7)
				.searchEmployees(null, null, null, PageRequest.of(2, 20));
		verify(this.repository).search("", null, null, PageRequest.of(2, 7, Sort.by("employeeNumber")));
	}

	@Test
	void nonpositivePageSizeLimitIsRejected() {
		assertThatThrownBy(() -> new EmployeeService(this.repository, this.schedules, 0))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new EmployeeService(this.repository, this.schedules, -1))
				.isInstanceOf(IllegalArgumentException.class);
		verifyNoInteractions(this.repository, this.schedules);
	}

	@Test
	void duplicateEmployeeNumberIsRejectedBeforeSaving() {
		when(this.repository.existsByEmployeeNumber("EMP-42")).thenReturn(true);
		assertThatThrownBy(
				() -> this.service
						.createEmployee("EMP-42", "Ada", "Lovelace", "ada@example.test", mock(Schedule.class)))
				.isInstanceOf(ResourceAlreadyExistsException.class);
		verify(this.repository, never()).existsByEmail(anyString());
		verify(this.repository, never()).save(any());
	}

	@Test
	void creationChecksNormalizedEmailAndReturnsPersistedEmployee() {
		final Schedule schedule = mock(Schedule.class);
		when(this.repository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));
		final Employee employee = this.service
				.createEmployee("EMP-42", "Ada", "Lovelace", "ADA@EXAMPLE.TEST", schedule);
		verify(this.repository).existsByEmail("ada@example.test");
		assertThat(employee.getEmail()).isEqualTo("ada@example.test");
		assertThat(employee.getEmployeeNumber()).isEqualTo("EMP-42");
		assertThat(employee.getSchedule()).isSameAs(schedule);
		assertThat(employee.getAppUser()).isNull();
	}

	@Test
	void duplicateEmailCreationIsRejectedBeforeSaving() {
		when(this.repository.existsByEmail("ada@example.test")).thenReturn(true);
		assertThatThrownBy(
				() -> this.service
						.createEmployee("EMP-42", "Ada", "Lovelace", "ADA@EXAMPLE.TEST", mock(Schedule.class)))
				.isInstanceOf(ResourceAlreadyExistsException.class);
		verify(this.repository, never()).save(any());
	}

	@Test
	void duplicateEmailUpdateLeavesAllEmployeeValuesUnchanged() {
		final Schedule original = mock(Schedule.class);
		final Employee employee = new Employee("EMP-42", "Ada", "Lovelace", "ada@example.test", original);
		when(this.repository.findByEmployeeNumber("EMP-42")).thenReturn(employee);
		when(this.schedules.findScheduleByCode("NIGHT")).thenReturn(mock(Schedule.class));
		when(this.repository.existsByEmail("taken@example.test")).thenReturn(true);
		assertThatThrownBy(() -> this.service.updateEmployee("EMP-42", "New", "Name", "TAKEN@EXAMPLE.TEST", "NIGHT"))
				.isInstanceOf(ResourceAlreadyExistsException.class);
		assertThat(employee.getName()).isEqualTo("Ada");
		assertThat(employee.getSurname()).isEqualTo("Lovelace");
		assertThat(employee.getEmail()).isEqualTo("ada@example.test");
		assertThat(employee.getSchedule()).isSameAs(original);
	}

	@Test
	void unchangedNormalizedEmailDoesNotConflictWithItselfAndUpdateKeepsIdentity() {
		final Employee employee = new Employee("EMP-42", "Ada", "Lovelace", "ada@example.test", mock(Schedule.class));
		final Schedule replacement = mock(Schedule.class);
		when(this.repository.findByEmployeeNumber("EMP-42")).thenReturn(employee);
		when(this.schedules.findScheduleByCode("NIGHT")).thenReturn(replacement);
		assertThat(this.service.updateEmployee("EMP-42", "New", "Name", "ADA@EXAMPLE.TEST", "NIGHT"))
				.isSameAs(employee);
		verify(this.repository, never()).existsByEmail(anyString());
		assertThat(employee.getEmployeeNumber()).isEqualTo("EMP-42");
		assertThat(employee.getName()).isEqualTo("New");
		assertThat(employee.getSurname()).isEqualTo("Name");
		assertThat(employee.getSchedule()).isSameAs(replacement);
	}

	@Test
	void missingScheduleDoesNotMutateEmployee() {
		when(this.schedules.findScheduleByCode("MISSING")).thenThrow(new ResourceNotFoundException("Missing schedule"));
		assertThatThrownBy(() -> this.service.updateEmployee("EMP-42", "New", "Name", "new@example.test", "MISSING"))
				.isInstanceOf(ResourceNotFoundException.class);
		verifyNoInteractions(this.repository);
	}

	@Test
	void missingEmployeeNumberReportsNotFound() {
		assertThatThrownBy(() -> this.service.findEmployeeByEmployeeNumber("MISSING"))
				.isInstanceOf(ResourceNotFoundException.class);
	}
}
