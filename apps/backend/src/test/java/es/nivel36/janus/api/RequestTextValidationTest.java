/*
 * Copyright 2026 Abel Ferrer Jiménez Licensed under the Apache License, Version
 * 2.0 (the "License");
 */
package es.nivel36.janus.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import es.nivel36.janus.api.v1.applicationsettings.UpdateApplicationSettingsRequest;
import es.nivel36.janus.api.v1.appuser.UpdateAppUserRequest;
import es.nivel36.janus.api.v1.employee.CreateEmployeeRequest;
import es.nivel36.janus.api.v1.employee.UpdateEmployeeRequest;
import es.nivel36.janus.api.v1.schedule.CreateScheduleRequest;
import es.nivel36.janus.api.v1.schedule.ScheduleRuleRequest;
import es.nivel36.janus.api.v1.schedule.UpdateScheduleRequest;
import es.nivel36.janus.api.v1.timelog.TransitionClockOutWithoutClockInEventRequest;
import es.nivel36.janus.api.v1.worksite.CreateWorksiteRequest;
import es.nivel36.janus.api.v1.worksite.UpdateWorksiteRequest;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class RequestTextValidationTest {
	private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();

	@AfterAll
	static void closeValidator() {
		FACTORY.close();
	}

	static Stream<Arguments> boundedText() {
		return Stream.of(
				Arguments.of(CreateEmployeeRequest.class, "name", 255),
				Arguments.of(UpdateEmployeeRequest.class, "surname", 255),
				Arguments.of(CreateWorksiteRequest.class, "name", 250),
				Arguments.of(UpdateWorksiteRequest.class, "name", 250),
				Arguments.of(CreateScheduleRequest.class, "name", 250),
				Arguments.of(UpdateScheduleRequest.class, "name", 250),
				Arguments.of(ScheduleRuleRequest.class, "name", 250),
				Arguments.of(CreateWorksiteRequest.class, "description", 500),
				Arguments.of(UpdateWorksiteRequest.class, "address", 500),
				Arguments.of(TransitionClockOutWithoutClockInEventRequest.class, "reason", 255),
				Arguments.of(CreateWorksiteRequest.class, "code", 50),
				Arguments.of(CreateScheduleRequest.class, "code", 50),
				Arguments.of(CreateEmployeeRequest.class, "employeeNumber", 50));
	}

	@ParameterizedTest
	@MethodSource("boundedText")
	void acceptsMaximumAndRejectsOneMore(final Class<?> type, final String field, final int maximum) {
		final Validator validator = FACTORY.getValidator();
		assertThat(validator.validateValue(type, field, "a".repeat(maximum))).isEmpty();
		assertThat(validator.validateValue(type, field, "a".repeat(maximum + 1))).isNotEmpty();
	}

	static Stream<Class<?>> employeeRequests() {
		return Stream.of(CreateEmployeeRequest.class, UpdateEmployeeRequest.class);
	}

	@ParameterizedTest
	@MethodSource("employeeRequests")
	void emailHasAnExplicitTotalLengthLimit(final Class<?> type) {
		final Validator validator = FACTORY.getValidator();
		final String prefix = "a".repeat(64) + "@" + "b".repeat(63) + "." + "c".repeat(63) + ".";
		assertThat(validator.validateValue(type, "email", prefix + "d".repeat(61))).isEmpty();
		assertThat(validator.validateValue(type, "email", prefix + "d".repeat(62))).anySatisfy(
				violation -> assertThat(
						violation.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName())
						.isEqualTo("Size"));
	}

	@Test
	void localeAndTimezonesAreBoundedBeforeNormalization() {
		final Validator validator = FACTORY.getValidator();
		final String locale64 = "en-x-" + "abcdefgh-".repeat(6) + "abcde";
		assertThat(locale64).hasSize(64);
		assertThat(validator.validateValue(UpdateAppUserRequest.class, "locale", locale64)).isEmpty();
		assertThat(validator.validateValue(UpdateAppUserRequest.class, "locale", locale64 + "f")).isNotEmpty();
		for (final Class<?> type : List.of(UpdateAppUserRequest.class, UpdateApplicationSettingsRequest.class)) {
			assertThat(validator.validateValue(type, "defaultTimezone", " ".repeat(61) + "UTC")).isEmpty();
			assertThat(validator.validateValue(type, "defaultTimezone", " ".repeat(62) + "UTC")).isNotEmpty();
		}
		for (final Class<?> type : List.of(CreateWorksiteRequest.class, UpdateWorksiteRequest.class)) {
			assertThat(validator.validateValue(type, "timeZone", "UTC")).isEmpty();
			assertThat(validator.validateValue(type, "timeZone", "a".repeat(65))).anySatisfy(
					violation -> assertThat(
							violation.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName())
							.isEqualTo("Size"));
		}
	}

	@Test
	void optionalTextAllowsNullAndMultilineUnicodeButRejectsNul() {
		final Validator validator = FACTORY.getValidator();
		for (final Class<?> type : List.of(
				CreateWorksiteRequest.class,
				UpdateWorksiteRequest.class,
				TransitionClockOutWithoutClockInEventRequest.class)) {
			final String field = type == TransitionClockOutWithoutClockInEventRequest.class ? "reason" : "description";
			assertThat(validator.validateValue(type, field, null)).isEmpty();
			assertThat(validator.validateValue(type, field, "Información\n追加 😀")).isEmpty();
			assertThat(validator.validateValue(type, field, "before\u0000after")).isNotEmpty();
		}
	}

	@Test
	void nestedRuleNamesAreValidatedThroughScheduleRequests() {
		final ScheduleRuleRequest valid = new ScheduleRuleRequest("a".repeat(250), null, null, List.of());
		final ScheduleRuleRequest invalid = new ScheduleRuleRequest("a".repeat(251), null, null, List.of());
		final Validator validator = FACTORY.getValidator();
		assertThat(
				validator.validate(
						new CreateScheduleRequest("SCHED", "Schedule", Duration.ZERO, Duration.ZERO, List.of(valid))))
				.isEmpty();
		assertThat(
				validator.validate(
						new UpdateScheduleRequest("Schedule", Duration.ZERO, Duration.ZERO, List.of(invalid))))
				.anySatisfy(violation -> assertThat(violation.getPropertyPath().toString()).contains("rules[0].name"));
	}
}
