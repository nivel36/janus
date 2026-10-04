/*
 * Copyright 2026 Abel Ferrer Jiménez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package es.nivel36.janus.validation;

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

/**
 * Validates the stable identifier assigned to an employee.
 */
@Documented
@Constraint(validatedBy = {})
@Pattern(regexp = EmployeeNumber.PATTERN)
@ReportAsSingleViolation
@Retention(RUNTIME)
@Target({ FIELD, PARAMETER, RECORD_COMPONENT, TYPE_USE, ANNOTATION_TYPE })
public @interface EmployeeNumber {

	String PATTERN = "[A-Za-z0-9_-]{1,50}";

	String message() default "employeeNumber must contain only letters, digits, underscores or hyphens (1-50 characters)";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
