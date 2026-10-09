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
package es.nivel36.janus.service;

import java.util.Locale;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converts JPA locale attributes to BCP 47 language tags and back.
 * <p>
 * {@code null} values remain {@code null} in both directions. Stored tags are
 * parsed with {@link Locale#forLanguageTag(String)}; ill-formed trailing
 * subtags are ignored rather than rejected.
 */
@Converter(autoApply = true)
public class LocaleConverter implements AttributeConverter<Locale, String> {

	/**
	 * Returns the locale's BCP 47 language tag for storage.
	 *
	 * @param  locale the locale to convert, or {@code null}
	 * @return        the language tag, or {@code null} if the locale is
	 *                {@code null}
	 */
	@Override
	public String convertToDatabaseColumn(final Locale locale) {
		return locale != null ? locale.toLanguageTag() : null;
	}

	/**
	 * Returns the locale represented by the stored language tag.
	 *
	 * @param  dbValue the stored BCP 47 language tag, or {@code null}
	 * @return         the parsed locale, or {@code null} if the stored value is
	 *                 {@code null}
	 */
	@Override
	public Locale convertToEntityAttribute(final String dbValue) {
		return dbValue != null ? Locale.forLanguageTag(dbValue) : null;
	}
}
