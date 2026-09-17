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
package es.nivel36.janus.api.v1.appuser;

import java.util.IllformedLocaleException;
import java.util.Locale;

import es.nivel36.janus.service.TimeFormat;
import es.nivel36.janus.service.appuser.AppUser;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload for updating an existing {@link AppUser}.
 *
 * @param locale          the preferred locale of the user expressed as a BCP 47
 *                        language tag (e.g. {@code "en-US"}); must not be blank
 *                        and must identify a supported locale
 * @param timeFormat      the preferred {@link TimeFormat} of the user; must not
 *                        be {@code null}
 * @param defaultTimezone the default timezone identifier of the user (for
 *                        example {@code "Europe/Madrid"}); must not be blank
 */
public record UpdateAppUserRequest( //
		@NotBlank(message = "locale must not be blank") //
		String locale, //

		@NotNull(message = "timeFormat must not be null") //
		TimeFormat timeFormat, //

		@NotBlank(message = "defaultTimezone must not be blank") //
		String defaultTimezone) {

	@AssertTrue(message = "locale must be a valid BCP 47 language tag")
	boolean isLocaleValid() {
		if (this.locale == null || this.locale.isBlank()) {
			return true;
		}
		try {
			new Locale.Builder().setLanguageTag(this.locale.trim()).build();
			return true;
		} catch (final IllformedLocaleException invalidLocale) {
			return false;
		}
	}

	@AssertTrue(message = "locale must identify a supported locale")
	boolean isLocaleSupported() {
		if (this.locale == null || this.locale.isBlank() || !isLocaleValid()) {
			return true;
		}
		final String languageTag = new Locale.Builder().setLanguageTag(this.locale.trim()).build()
				.toLanguageTag();
		return Locale.availableLocales().anyMatch(candidate -> candidate.toLanguageTag().equals(languageTag));
	}
}
