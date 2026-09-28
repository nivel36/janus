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
package es.nivel36.janus.api.validation;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Validates bounded, single-line free text used by repository search queries.
 * Unicode letters, marks, numbers, spaces, punctuation and symbols are accepted;
 * control characters are deliberately excluded.
 */
@Documented
@Constraint(validatedBy = {})
@Pattern(regexp = "[\\p{L}\\p{M}\\p{N}\\p{Zs}\\p{P}\\p{S}]+")
@Size(max = SearchQuery.MAX_LENGTH)
@ReportAsSingleViolation
@Retention(RUNTIME)
@Target({ FIELD, PARAMETER, ANNOTATION_TYPE })
public @interface SearchQuery {

	int MAX_LENGTH = 100;

	String message() default "query must be single-line text containing at most 100 characters";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
