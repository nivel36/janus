/*
 * Copyright 2026 Abel Ferrer Jiménez
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package es.nivel36.janus.service.worksite;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.ZoneId;
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

import es.nivel36.janus.service.employee.EmployeeService;
import jakarta.validation.ConstraintViolationException;

/**
 * Verifies service argument validation before dependency access through Spring.
 */
class WorksiteServiceValidationTest {
	private AnnotationConfigApplicationContext context;
	private WorksiteRepository repository;
	private EmployeeService employees;
	private WorksiteService service;

	@BeforeEach
	void setUp() {
		this.repository = mock(WorksiteRepository.class);
		this.employees = mock(EmployeeService.class);
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
				.registerBean(WorksiteService.class, () -> new WorksiteService(this.repository, this.employees, 100));
		this.context.refresh();
		this.service = this.context.getBean(WorksiteService.class);
	}

	@AfterEach
	void tearDown() {
		this.context.close();
	}

	@ParameterizedTest
	@ValueSource(strings = { "", "Madrid\nNorte", "Madrid\tNorte", "Madrid\u0000Norte" })
	void invalidQueryIsRejectedBeforeSearch(final String query) {
		assertThatThrownBy(() -> this.service.searchWorksites(query, null, PageRequest.of(0, 10)))
				.isInstanceOf(ConstraintViolationException.class);
		verifyNoInteractions(this.repository, this.employees);
	}

	@Test
	void oversizedQueryIsRejectedBeforeSearch() {
		assertThatThrownBy(() -> this.service.searchWorksites("x".repeat(101), null, PageRequest.of(0, 10)))
				.isInstanceOf(ConstraintViolationException.class);
		verifyNoInteractions(this.repository, this.employees);
	}

	@ParameterizedTest
	@ValueSource(strings = { "", " ", "bad number", " EMP-1 ", "!" })
	void invalidEmployeeFilterIsRejectedBeforeSearch(final String number) {
		assertThatThrownBy(() -> this.service.searchWorksites(null, number, PageRequest.of(0, 10)))
				.isInstanceOf(ConstraintViolationException.class);
		verifyNoInteractions(this.repository, this.employees);
	}

	@Test
	void nullPageableIsRejectedBeforeSearch() {
		assertThatThrownBy(() -> this.service.searchWorksites(null, null, null))
				.isInstanceOf(ConstraintViolationException.class);
		verifyNoInteractions(this.repository, this.employees);
	}

	@Test
	void queryAndEmployeeBoundariesAndMissingFiltersAreAccepted() {
		final PageRequest request = PageRequest.of(0, 10, Sort.by("code"));
		when(this.repository.search("x".repeat(100), "x".repeat(50), request)).thenReturn(Page.empty(request));
		when(this.repository.search("", null, request)).thenReturn(Page.empty(request));
		assertThat(this.service.searchWorksites("x".repeat(100), "x".repeat(50), request)).isEmpty();
		assertThat(this.service.searchWorksites(null, null, request)).isEmpty();
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = { "", " ", "BAD CODE", " WS-1 ", "!" })
	void invalidBusinessCodeIsRejectedBeforeLookup(final String code) {
		assertThatThrownBy(() -> this.service.findWorksiteByCode(code))
				.isInstanceOf(ConstraintViolationException.class);
		assertThatThrownBy(() -> this.service.deleteWorksite(code)).isInstanceOf(ConstraintViolationException.class);
		assertThatThrownBy(() -> this.service.assertEmployeeCanUseWorksite("EMP-1", code))
				.isInstanceOf(ConstraintViolationException.class);
		verifyNoInteractions(this.repository, this.employees);
	}

	static Stream<Arguments> invalidData() {
		return Stream.of(
				Arguments.of(null, "Site", ZoneId.of("UTC"), WorksiteScope.GLOBAL),
				Arguments.of("bad code", "Site", ZoneId.of("UTC"), WorksiteScope.GLOBAL),
				Arguments.of("x".repeat(51), "Site", ZoneId.of("UTC"), WorksiteScope.GLOBAL),
				Arguments.of("WS-1", null, ZoneId.of("UTC"), WorksiteScope.GLOBAL),
				Arguments.of("WS-1", " ", ZoneId.of("UTC"), WorksiteScope.GLOBAL),
				Arguments.of("WS-1", "Site", null, WorksiteScope.GLOBAL),
				Arguments.of("WS-1", "Site", ZoneId.of("UTC"), null));
	}

	@ParameterizedTest
	@MethodSource("invalidData")
	void invalidCreationAndUpdateAreRejectedBeforeRepositoryAccess(
			final String code,
			final String name,
			final ZoneId zone,
			final WorksiteScope scope) {
		assertThatThrownBy(() -> this.service.createWorksite(code, name, zone, scope, null, null))
				.isInstanceOf(ConstraintViolationException.class);
		assertThatThrownBy(() -> this.service.updateWorksite(code, name, zone, scope, null, null))
				.isInstanceOf(ConstraintViolationException.class);
		verifyNoInteractions(this.repository, this.employees);
	}

	@Test
	void nullResourcesAreRejectedBeforeDeletionOrAssociationChanges() {
		assertThatThrownBy(() -> this.service.deleteWorksite(null)).isInstanceOf(ConstraintViolationException.class);
		assertThatThrownBy(() -> this.service.addEmployeeToWorksite("WS-1", null))
				.isInstanceOf(ConstraintViolationException.class);
		assertThatThrownBy(() -> this.service.removeEmployeeFromWorksite("WS-1", null))
				.isInstanceOf(ConstraintViolationException.class);
		assertThatThrownBy(() -> this.service.assertEmployeeCanUseWorksite(null, "WS-1"))
				.isInstanceOf(ConstraintViolationException.class);
		verifyNoInteractions(this.repository, this.employees);
	}

	@ParameterizedTest
	@ValueSource(strings = { "", " ", "bad number", " EMP-1 ", "!" })
	void invalidAssignmentIdentifiersAreRejectedBeforeLookup(final String invalid) {
		assertThatThrownBy(() -> this.service.addEmployeeToWorksite(invalid, "EMP-1"))
				.isInstanceOf(ConstraintViolationException.class);
		assertThatThrownBy(() -> this.service.addEmployeeToWorksite("WS-1", invalid))
				.isInstanceOf(ConstraintViolationException.class);
		assertThatThrownBy(() -> this.service.removeEmployeeFromWorksite(invalid, "EMP-1"))
				.isInstanceOf(ConstraintViolationException.class);
		assertThatThrownBy(() -> this.service.removeEmployeeFromWorksite("WS-1", invalid))
				.isInstanceOf(ConstraintViolationException.class);
		verifyNoInteractions(this.repository, this.employees);
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = { "", " ", "bad number", " EMP-1 ", "!" })
	void invalidEmployeeNumberIsRejectedBeforeCheckingWorksiteAccess(final String number) {
		assertThatThrownBy(() -> this.service.assertEmployeeCanUseWorksite(number, "WS-1"))
				.isInstanceOf(ConstraintViolationException.class);
		verifyNoInteractions(this.repository, this.employees);
	}
}
