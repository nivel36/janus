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

import org.springframework.stereotype.Component;

import es.nivel36.janus.api.Mapper;
import es.nivel36.janus.service.applicationsettings.ApplicationSettings;

/**
 * Maps {@link ApplicationSettings} entities into
 * {@link ApplicationSettingsResponse} DTOs.
 */
@Component
public class ApplicationSettingsResponseMapper implements Mapper<ApplicationSettings, ApplicationSettingsResponse> {

	@Override
	public ApplicationSettingsResponse map(final ApplicationSettings applicationSettings) {
		if (applicationSettings == null) {
			return null;
		}
		final int daysUntilLocked = applicationSettings.getDaysUntilLocked();
		final boolean employeeWorkplaceCreationAllowed = applicationSettings.isEmployeeWorkplaceCreationAllowed();
		final boolean worksiteChangeDuringShiftAllowed = applicationSettings.isWorksiteChangeDuringShiftAllowed();
		final boolean employeeManualTimelogEntryAllowed = applicationSettings.isEmployeeManualTimelogEntryAllowed();
		final String zoneId = applicationSettings.getDefaultTimezone().getId();
		return new ApplicationSettingsResponse(daysUntilLocked, employeeWorkplaceCreationAllowed,
				worksiteChangeDuringShiftAllowed, employeeManualTimelogEntryAllowed, zoneId);
	}
}
