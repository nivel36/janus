/*
 * Copyright 2026 Abel Ferrer Jiménez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package es.nivel36.janus.api.validation;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.RECORD_COMPONENT;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
import jakarta.validation.constraints.Pattern;

/** Validates the stable business code assigned to a schedule. */
@Documented
@Constraint(validatedBy = {})
@Pattern(regexp = ScheduleCode.PATTERN)
@ReportAsSingleViolation
@Retention(RUNTIME)
@Target({ FIELD, PARAMETER, RECORD_COMPONENT, TYPE_USE, ANNOTATION_TYPE })
public @interface ScheduleCode {

	String PATTERN = "[A-Za-z0-9_-]{1,50}";

	String message() default "scheduleCode must contain only letters, digits, underscores or hyphens (1-50 characters)";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
