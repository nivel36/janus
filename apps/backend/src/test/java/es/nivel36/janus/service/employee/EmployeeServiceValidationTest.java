/*
 * Copyright 2026 Abel Ferrer Jiménez Licensed under the Apache License, Version
 * 2.0 (the "License");
 */
package es.nivel36.janus.service.employee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

import es.nivel36.janus.service.schedule.Schedule;
import es.nivel36.janus.service.schedule.ScheduleService;
import jakarta.validation.ConstraintViolationException;

/**
 * Verifies validation at the service proxy before persistence or schedule
 * lookup.
 */
class EmployeeServiceValidationTest {
	private AnnotationConfigApplicationContext context;
	private EmployeeRepository repository;
	private ScheduleService schedules;
	private EmployeeService service;

	@BeforeEach
	void setUp() {
		this.repository = mock(EmployeeRepository.class);
		this.schedules = mock(ScheduleService.class);
		this.context = new AnnotationConfigApplicationContext();
		this.context.registerBean(
				LocalValidatorFactoryBean.class,
				definition -> definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE));
		this.context.registerBean(MethodValidationPostProcessor.class, () -> {
			final MethodValidationPostProcessor processor = new MethodValidationPostProcessor();
			processor.setValidator(this.context.getBean(LocalValidatorFactoryBean.class));
			return processor;
		});
		this.context
				.registerBean(EmployeeService.class, () -> new EmployeeService(this.repository, this.schedules, 100));
		this.context.refresh();
		this.service = this.context.getBean(EmployeeService.class);
	}

	@AfterEach
	void tearDown() {
		this.context.close();
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = { "", " ", "bad number", " EMP-42 ", "!" })
	void invalidEmployeeNumberIsRejectedBeforeLookup(final String number) {
		assertThatThrownBy(() -> this.service.findEmployeeByEmployeeNumber(number))
				.isInstanceOf(ConstraintViolationException.class);
		verifyNoInteractions(this.repository, this.schedules);
	}

	static Stream<Arguments> invalidSearchFilters() {
		return Stream.of(
				Arguments.of("", null, null),
				Arguments.of("line\nbreak", null, null),
				Arguments.of("x".repeat(101), null, null),
				Arguments.of(null, "", null),
				Arguments.of(null, " DAY ", null),
				Arguments.of(null, "x".repeat(51), null),
				Arguments.of(null, null, ""),
				Arguments.of(null, null, " HQ "),
				Arguments.of(null, null, "x".repeat(51)));
	}

	@ParameterizedTest
	@MethodSource("invalidSearchFilters")
	void invalidFiltersAreRejectedBeforeSearching(final String query, final String schedule, final String worksite) {
		assertThatThrownBy(() -> this.service.searchEmployees(query, schedule, worksite, PageRequest.of(0, 10)))
				.isInstanceOf(ConstraintViolationException.class);
		verifyNoInteractions(this.repository, this.schedules);
	}

	@Test
	void nullPageableIsRejectedBeforeSearching() {
		assertThatThrownBy(() -> this.service.searchEmployees(null, null, null, null))
				.isInstanceOf(ConstraintViolationException.class);
		verifyNoInteractions(this.repository, this.schedules);
	}

	static Stream<Arguments> invalidPersonalData() {
		return Stream.of(
				Arguments.of(null, "Ada", "Lovelace", "ada@example.test"),
				Arguments.of("EMP 42", "Ada", "Lovelace", "ada@example.test"),
				Arguments.of("x".repeat(51), "Ada", "Lovelace", "ada@example.test"),
				Arguments.of("EMP-42", null, "Lovelace", "ada@example.test"),
				Arguments.of("EMP-42", " ", "Lovelace", "ada@example.test"),
				Arguments.of("EMP-42", "Ada123", "Lovelace", "ada@example.test"),
				Arguments.of("EMP-42", "x".repeat(256), "Lovelace", "ada@example.test"),
				Arguments.of("EMP-42", "Ada", null, "ada@example.test"),
				Arguments.of("EMP-42", "Ada", " ", "ada@example.test"),
				Arguments.of("EMP-42", "Ada", "Name!", "ada@example.test"),
				Arguments.of("EMP-42", "Ada", "Lovelace", null),
				Arguments.of("EMP-42", "Ada", "Lovelace", "invalid"),
				Arguments.of("EMP-42", "Ada", "Lovelace", " ada@example.test "));
	}

	@ParameterizedTest
	@MethodSource("invalidPersonalData")
	void invalidCreationAndUpdateAreRejectedBeforeDependencyAccess(
			final String number,
			final String name,
			final String surname,
			final String email) {
		final Schedule schedule = mock(Schedule.class);
		assertThatThrownBy(() -> this.service.createEmployee(number, name, surname, email, schedule))
				.isInstanceOf(ConstraintViolationException.class);
		assertThatThrownBy(() -> this.service.updateEmployee(number, name, surname, email, "DAY"))
				.isInstanceOf(ConstraintViolationException.class);
		verifyNoInteractions(this.repository, this.schedules);
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = { "", " NIGHT ", "!" })
	void invalidUpdateScheduleCodeIsRejectedBeforeLookup(final String code) {
		assertThatThrownBy(() -> this.service.updateEmployee("EMP-42", "Ada", "Lovelace", "ada@example.test", code))
				.isInstanceOf(ConstraintViolationException.class);
		verifyNoInteractions(this.repository, this.schedules);
	}

	@Test
	void missingScheduleAndDeletionTargetAreRejectedBeforeDependencyAccess() {
		assertThatThrownBy(() -> this.service.createEmployee("EMP-42", "Ada", "Lovelace", "ada@example.test", null))
				.isInstanceOf(ConstraintViolationException.class);
		assertThatThrownBy(() -> this.service.deleteEmployee(null)).isInstanceOf(ConstraintViolationException.class);
		verifyNoInteractions(this.repository, this.schedules);
	}

	@Test
	void boundarySearchFiltersPassValidation() {
		final String query = "x".repeat(100);
		final String code = "x".repeat(50);
		final PageRequest request = PageRequest.of(0, 10, Sort.by("employeeNumber"));
		final Page<Employee> page = Page.empty(request);
		when(this.repository.search(query, code, code, request)).thenReturn(page);
		assertThat(this.service.searchEmployees(query, code, code, request)).isSameAs(page);
	}
}
