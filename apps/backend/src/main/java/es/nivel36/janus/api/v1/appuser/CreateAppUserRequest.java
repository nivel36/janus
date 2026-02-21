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

import es.nivel36.janus.service.TimeFormat;
import es.nivel36.janus.service.appuser.AppUser;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request payload for creating a new {@link AppUser}.
 *
 * @param username   the unique username used to identify the user; must not be
 *                   blank and must match the allowed pattern
 * @param name       the user's first name; must not be blank and must match the
 *                   allowed pattern
 * @param surname    the user's surname; must not be blank and must match the
 *                   allowed pattern
 * @param password   the raw password for the user; must not be blank and must
 *                   meet minimum length requirements
 * @param locale     the preferred locale of the user expressed as a BCP 47
 *                   language tag (e.g. {@code "en-US"}); must not be blank and
 *                   must match the allowed pattern
 * @param timeFormat the preferred {@link TimeFormat} of the user; must not be
 *                   {@code null}
 */
public record CreateAppUserRequest( //
		@NotBlank(message = "username must not be blank") //
		@Pattern(regexp = "[A-Za-z0-9_.@-]{3,50}", //
				message = "username must contain only letters, digits, dots, underscores, hyphens or at signs (3-50 characters)") //
		String username, //

		@NotBlank(message = "name must not be blank") //
		@Pattern(regexp = "^[\\p{L} .,'-]{1,255}$", //
				message = "name must contain only letters, spaces, dots, commas, apostrophes or hyphens (max 255)") //
		String name, //

		@NotBlank(message = "surname must not be blank") //
		@Pattern(regexp = "^[\\p{L} .,'-]{1,255}$", //
				message = "surname must contain only letters, spaces, dots, commas, apostrophes or hyphens (max 255)") //
		String surname, //

		@NotBlank(message = "password must not be blank") //
		@Size(min = 8, max = 72, message = "password must be between 8 and 72 characters") //
		String password, //

		@NotBlank(message = "locale must not be blank") //
		@Pattern(regexp = "^[a-z]{2,3}-[A-Z]{2}$", //
				message = "locale must be in format ll_CC (e.g., es_ES)") //
		String locale, //

		@NotNull(message = "timeFormat must not be null") //
		TimeFormat timeFormat) {
}
