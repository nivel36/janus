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
package es.nivel36.janus.api.v1.applicationsettings;

import es.nivel36.janus.validation.ValidTimeZone;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Request payload used to update global application settings.
 *
 * <p>
 * For a temporary migration window, deserialization also accepts the obsolete
 * {@code employeeWorkplaceCreationAllowed} and
 * {@code employeeManualTimelogEntryAllowed} property names. Clients must use
 * the canonical record component names; the aliases will be removed.
 *
 * @param daysUntilLocked                   number of days a time log remains
 *                                          editable; must be greater than or
 *                                          equal to zero
 * @param employeeWorksiteCreationAllowed   whether employees can create
 *                                          personal worksites
 * @param worksiteChangeDuringShiftAllowed  whether changing worksite during a
 *                                          shift is allowed
 * @param employeeManualTimeLogEntryAllowed whether employees can set custom
 *                                          entry/exit instants in timelog
 *                                          operations
 * @param defaultTimezone                   valid IANA time-zone identifier used
 *                                          as default; must not be blank
 */
public record UpdateApplicationSettingsRequest(

		@PositiveOrZero(message = "daysUntilLocked must be greater than or equal to 0") //
		@NotNull(message = "daysUntilLocked is required") //
		Integer daysUntilLocked, //

		@NotNull(message = "employeeWorksiteCreationAllowed is required") //
		Boolean employeeWorksiteCreationAllowed, //

		@NotNull(message = "worksiteChangeDuringShiftAllowed is required") //
		Boolean worksiteChangeDuringShiftAllowed, //

		@NotNull(message = "employeeManualTimeLogEntryAllowed is required") //
		Boolean employeeManualTimeLogEntryAllowed, //

		@NotBlank(message = "defaultTimezone is required") //
		@ValidTimeZone(message = "defaultTimezone must be a valid time-zone identifier") //
		String defaultTimezone) {
}
