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

import java.util.UUID;

import es.nivel36.janus.service.TimeFormat;
import es.nivel36.janus.service.appuser.Theme;

/**
 * Immutable public snapshot of a local application profile. The mapper supplies
 * a persisted UUID and nonnull contact/preferences; employee number is nullable
 * for an unlinked profile. Construction itself performs no validation. Reading
 * this record has no persistence effects and never exposes the provider subject
 * or credentials.
 *
 * @param id              persistent public UUID
 * @param email           normalized contact email; not necessarily unique
 * @param employeeNumber  linked employee number, or null for an unlinked
 *                        profile
 * @param locale          preferred BCP 47 language tag
 * @param timeFormat      preferred time display format
 * @param defaultTimezone preferred timezone identifier
 * @param theme           preferred color theme
 */
public record AppUserResponse(
		UUID id,
		String email,
		String employeeNumber,
		String locale,
		TimeFormat timeFormat,
		String defaultTimezone,
		Theme theme) {
}
