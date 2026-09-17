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
package es.nivel36.janus.api.v1.worksite;

import es.nivel36.janus.service.worksite.Worksite;
import es.nivel36.janus.service.worksite.WorksiteScope;
import es.nivel36.janus.api.validation.ValidTimeZone;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request payload for updating an existing {@link Worksite}.
 *
 * @param name        the new human-readable name of the worksite; must not be blank and must
 *                    contain between 1 and 250 allowed characters
 * @param timeZone    valid new {@link java.time.ZoneId} identifier of the worksite; must not be
 *                    blank and must contain at most 64 characters
 * @param scope       the new visibility scope of the worksite; must not be {@code null}
 * @param description optional worksite description of at most 500 characters
 * @param address     optional worksite address of at most 500 characters
 */
public record UpdateWorksiteRequest( //
		@NotBlank(message = "name must not be blank") //
		@Pattern( //
				regexp = "^[\\p{L}0-9 _'.,-]{1,250}$", //
				message = "name must contain only letters, digits, spaces, and basic punctuation (max 250)") //
		String name, //

		@NotBlank(message = "timeZone must not be blank") //
		@Pattern( //
				 regexp = "^[A-Za-z0-9_./+:-]{1,64}$", //
		message = "timeZone must contain only letters, digits, underscores, dots, slashes, plus, minus, or colons (max 64)") //
		@Size(max = 64, message = "timeZone must not exceed 64 characters") //
		@ValidTimeZone(message = "timeZone must be a valid time-zone identifier") //
		String timeZone, //

		@NotNull(message = "scope must not be null") //
		WorksiteScope scope, //

		@Size(max = 500, message = "description must not exceed 500 characters") String description, //

		@Size(max = 500, message = "address must not exceed 500 characters") String address) {

}
