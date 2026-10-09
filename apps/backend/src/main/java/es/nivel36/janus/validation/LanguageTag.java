package es.nivel36.janus.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Validates a BCP 47 language tag after trimming leading and trailing
 * whitespace.
 * <p>
 * Well-formedness is checked with
 * {@link java.util.Locale.Builder#setLanguageTag(String)}; locale availability
 * is not checked. {@code null} and blank values are accepted and require a
 * separate constraint when they must be rejected.
 */
@Documented
@Constraint(validatedBy = LanguageTagValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD, ElementType.ANNOTATION_TYPE,
		ElementType.TYPE_USE })
@Retention(RetentionPolicy.RUNTIME)
public @interface LanguageTag {

	String message() default "must be a well-formed BCP 47 language tag";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
