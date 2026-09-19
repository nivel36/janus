package es.nivel36.janus.api.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Documented
@Constraint(validatedBy = LanguageTagValidator.class)
@Target({ //
		ElementType.FIELD, //
		ElementType.PARAMETER, //
		ElementType.METHOD, //
		ElementType.ANNOTATION_TYPE, //
		ElementType.TYPE_USE })
@Retention(RetentionPolicy.RUNTIME)
public @interface LanguageTag {

	String message() default "must be a well-formed BCP 47 language tag";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}