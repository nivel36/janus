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
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Request payload for updating an existing {@link Worksite}.
 *
 * @param name               the new human readable name of the worksite; must
 *                           contain between 1 and 250 characters
 * @param timeZone           the new {@link java.time.ZoneId} identifier of the
 *                           worksite; must contain between 1 and 80 characters
 * @param scope              the new visibility scope of the worksite
 * @param ownerEmployeeEmail optional owner employee identifier; mandatory only
 *                           for personal worksites
 */
public record UpdateWorksiteRequest( //
		@NotBlank(message = "name must not be blank") //
		@Pattern( //
				regexp = "^[\\p{L}0-9 _'.,-]{1,250}$", //
				message = "name must contain only letters, digits, spaces, and basic punctuation (max 250)") //
		String name, //

		@NotBlank(message = "timeZone must not be blank") //
		@Pattern( //
				regexp = "^[A-Za-z0-9_./+:-]{1,80}$", //
				message = "timeZone must contain only letters, digits, underscores, dots, slashes, plus, minus, or colons (max 80)") //
		String timeZone, //

		@NotNull(message = "scope must not be null") //
		WorksiteScope scope, //

		@Pattern( //
				regexp = "^(?=.{1,254}$)[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", //
				message = "must be a valid and safe email address (max 254)" //
		) //
		String ownerEmployeeEmail) {

	@AssertTrue(message = "ownerEmployeeEmail must not be null when scope is PERSONAL")
	public boolean isOwnerEmailValidForScope() {
		return scope != WorksiteScope.PERSONAL || ownerEmployeeEmail != null;
	}
}
