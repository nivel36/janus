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
package es.nivel36.janus.api.v1.applicationsettings;

import es.nivel36.janus.validation.ValidTimeZone;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Request payload for replacing all global application settings.
 * <p>
 * All components are required when the payload is validated. Construction alone
 * does not enforce the validation constraints or normalize the values.
 *
 * @param daysUntilLocked                   number of days a time log remains
 *                                          editable; must not be {@code null}
 *                                          and must be greater than or equal to
 *                                          {@code 0}
 * @param employeeWorksiteCreationAllowed   whether employees can create
 *                                          personal worksites; must not be
 *                                          {@code null}
 * @param worksiteChangeDuringShiftAllowed  whether changing worksite during a
 *                                          shift is allowed; must not be
 *                                          {@code null}
 * @param employeeManualTimeLogEntryAllowed whether employees can set custom
 *                                          entry and exit timestamps in time
 *                                          log operations; must not be
 *                                          {@code null}
 * @param defaultTimezone                   global time-zone identifier; must
 *                                          not be {@code null} or blank and
 *                                          must not exceed {@code 64}
 *                                          characters before trimming; the
 *                                          trimmed value must be accepted by
 *                                          {@link java.time.ZoneId#of(String)}
 */
public record UpdateApplicationSettingsRequest(

		@PositiveOrZero(message = "daysUntilLocked must be greater than or equal to 0")
		@NotNull(message = "daysUntilLocked is required")
		Integer daysUntilLocked,

		@NotNull(message = "employeeWorksiteCreationAllowed is required")
		Boolean employeeWorksiteCreationAllowed,

		@NotNull(message = "worksiteChangeDuringShiftAllowed is required")
		Boolean worksiteChangeDuringShiftAllowed,

		@NotNull(message = "employeeManualTimeLogEntryAllowed is required")
		Boolean employeeManualTimeLogEntryAllowed,

		@NotBlank(message = "defaultTimezone is required")
		@ValidTimeZone(message = "defaultTimezone must be a valid time-zone identifier")
		@Size(max = 64, message = "defaultTimezone must not exceed 64 characters")
		String defaultTimezone) {
}
