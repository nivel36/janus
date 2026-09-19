package es.nivel36.janus.api.validation;

import java.util.IllformedLocaleException;
import java.util.Locale;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class LanguageTagValidator implements ConstraintValidator<LanguageTag, String> {

	@Override
	public boolean isValid(final String value, final ConstraintValidatorContext context) {
		if (value == null || value.isBlank()) {
			return true;
		}

		try {
			new Locale.Builder().setLanguageTag(value.trim()).build();
			return true;
		} catch (final IllformedLocaleException _) {
			return false;
		}
	}
}