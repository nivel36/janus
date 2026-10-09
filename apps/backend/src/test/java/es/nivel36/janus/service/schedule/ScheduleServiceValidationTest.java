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
package es.nivel36.janus.service.schedule;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;
import jakarta.validation.ConstraintViolationException;

/**
 * Verifies that Spring rejects invalid service arguments before persistence.
 */
class ScheduleServiceValidationTest {
	private AnnotationConfigApplicationContext context;
	private ScheduleRepository repository;
	private ScheduleService service;

	@BeforeEach
	void setUp() {
		this.repository = mock(ScheduleRepository.class);
		this.context = new AnnotationConfigApplicationContext();
		this.context.registerBean(
				LocalValidatorFactoryBean.class,
				definition -> definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE));
		this.context.registerBean(MethodValidationPostProcessor.class, () -> {
			final MethodValidationPostProcessor processor = new MethodValidationPostProcessor();
			processor.setValidator(this.context.getBean(LocalValidatorFactoryBean.class));
			return processor;
		});
		this.context.registerBean(ScheduleService.class, () -> new ScheduleService(this.repository, 100));
		this.context.refresh();
		this.service = this.context.getBean(ScheduleService.class);
	}

	@AfterEach
	void tearDown() {
		this.context.close();
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = { "", " ", "bad code", " STD ", "STD!" })
	void invalidCodeIsRejectedBeforeLookup(final String code) {
		assertThatThrownBy(() -> this.service.findScheduleByCode(code))
				.isInstanceOf(ConstraintViolationException.class);
		verifyNoInteractions(this.repository);
	}

	static Stream<Arguments> invalidDefinitions() {
		return Stream.of(
				Arguments.of(null, "Name", Duration.ZERO, Duration.ZERO, List.of()),
				Arguments.of("x".repeat(51), "Name", Duration.ZERO, Duration.ZERO, List.of()),
				Arguments.of("STD", null, Duration.ZERO, Duration.ZERO, List.of()),
				Arguments.of("STD", " ", Duration.ZERO, Duration.ZERO, List.of()),
				Arguments.of("STD", "x".repeat(251), Duration.ZERO, Duration.ZERO, List.of()),
				Arguments.of("STD", "Name!", Duration.ZERO, Duration.ZERO, List.of()),
				Arguments.of("STD", "Name", null, Duration.ZERO, List.of()),
				Arguments.of("STD", "Name", Duration.ZERO, null, List.of()),
				Arguments.of("STD", "Name", Duration.ofSeconds(-1), Duration.ZERO, List.of()),
				Arguments.of("STD", "Name", Duration.ZERO, Duration.ofSeconds(-1), List.of()),
				Arguments.of("STD", "Name", Duration.ZERO, Duration.ZERO, null),
				Arguments
						.of("STD", "Name", Duration.ZERO, Duration.ZERO, Arrays.asList((ScheduleRuleDefinition) null)));
	}

	@ParameterizedTest
	@MethodSource("invalidDefinitions")
	void invalidCreationAndReplacementAreRejectedBeforePersistence(
			final String code,
			final String name,
			final Duration entry,
			final Duration exit,
			final List<ScheduleRuleDefinition> rules) {
		assertThatThrownBy(() -> this.service.createSchedule(code, name, entry, exit, rules))
				.isInstanceOf(ConstraintViolationException.class);
		assertThatThrownBy(() -> this.service.updateSchedule(code, name, entry, exit, rules))
				.isInstanceOf(ConstraintViolationException.class);
		verifyNoInteractions(this.repository);
	}

	@ParameterizedTest
	@ValueSource(strings = { "", "a\nb", "a\tb" })
	void invalidQueryIsRejectedBeforeSearching(final String query) {
		assertThatThrownBy(() -> this.service.searchSchedules(query, null, PageRequest.of(0, 20)))
				.isInstanceOf(ConstraintViolationException.class);
		verifyNoInteractions(this.repository);
	}

	@ParameterizedTest
	@ValueSource(strings = { "", " ", "bad number", " EMP-42 ", "!" })
	void invalidEmployeeFilterIsRejectedBeforeSearching(final String employee) {
		assertThatThrownBy(() -> this.service.searchSchedules(null, employee, PageRequest.of(0, 20)))
				.isInstanceOf(ConstraintViolationException.class);
		verifyNoInteractions(this.repository);
	}

	@Test
	void oversizedQueryAndNullRequiredArgumentsAreRejectedBeforeRepositoryAccess() {
		assertThatThrownBy(() -> this.service.searchSchedules("a".repeat(101), null, PageRequest.of(0, 20)))
				.isInstanceOf(ConstraintViolationException.class);
		assertThatThrownBy(() -> this.service.searchSchedules(null, null, null))
				.isInstanceOf(ConstraintViolationException.class);
		assertThatThrownBy(() -> this.service.deleteSchedule(null)).isInstanceOf(ConstraintViolationException.class);
		assertThatThrownBy(() -> this.service.findTimeRangeForEmployeeByDate(null, null))
				.isInstanceOf(ConstraintViolationException.class);
		verifyNoInteractions(this.repository);
	}

	@ParameterizedTest
	@ValueSource(strings = { "", " ", "bad key", " KEY ", "!" })
	void invalidDeletionKeyIsRejectedBeforePersistence(final String key) {
		assertThatThrownBy(() -> this.service.deleteSchedule(key)).isInstanceOf(ConstraintViolationException.class);
		verifyNoInteractions(this.repository);
	}

}
