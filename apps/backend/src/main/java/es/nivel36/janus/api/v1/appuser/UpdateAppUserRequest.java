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
package es.nivel36.janus.api.v1.appuser;

import es.nivel36.janus.service.TimeFormat;
import es.nivel36.janus.service.appuser.Theme;
import es.nivel36.janus.validation.LanguageTag;
import es.nivel36.janus.validation.ValidTimeZone;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;

/**
 * Immutable payload for replacing all application-user preferences.
 * Construction stores values without validation. Before resource execution,
 * Bean Validation requires a nonblank valid BCP 47 tag, nonnull time format and
 * theme, and a nonblank valid timezone. Successful application replaces all
 * preferences while preserving identity, contact email and employee
 * association.
 *
 * @param locale          valid nonblank BCP 47 tag; surrounding whitespace is
 *                        trimmed on use
 * @param timeFormat      nonnull time display format
 * @param defaultTimezone nonblank identifier accepted by java.time.ZoneId;
 *                        surrounding whitespace is trimmed on use
 * @param theme           nonnull color theme
 */
public record UpdateAppUserRequest(

		@NotBlank(message = "locale must not be blank")
		@LanguageTag
		@Size(max = 64, message = "locale must not exceed 64 characters")
		String locale,

		@NotNull(message = "timeFormat must not be null")
		TimeFormat timeFormat,

		@NotBlank(message = "defaultTimezone must not be blank")
		@ValidTimeZone(message = "defaultTimezone must be a valid time-zone identifier")
		@Size(max = 64, message = "defaultTimezone must not exceed 64 characters")
		String defaultTimezone,

		@NotNull(message = "theme must not be null")
		Theme theme) {
}
