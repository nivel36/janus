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
package es.nivel36.janus.service.applicationsettings;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApplicationSettingsService {

	private static final Logger logger = LoggerFactory.getLogger(ApplicationSettingsService.class);

	private final ApplicationSettingsRepository applicationSettingsRepository;

	public ApplicationSettingsService(final ApplicationSettingsRepository applicationSettingsRepository) {
		this.applicationSettingsRepository = Objects.requireNonNull(applicationSettingsRepository,
				"applicationSettingsRepository cannot be null");
	}

	@Transactional
	public ApplicationSettings update(int daysUntilLocked, boolean employeeWorkplaceCreationAllowed,
			boolean worksiteChangeDuringShiftAllowed) {
		logger.debug("Updating application settings");
		ApplicationSettings applicationSettings = this.applicationSettingsRepository
				.findById(ApplicationSettings.GLOBAL_SETTINGS_ID)
				.orElseThrow(() -> new IllegalStateException("Global application settings row is missing"));
		applicationSettings.setDaysUntilLocked(daysUntilLocked);
		applicationSettings.setEmployeeWorkplaceCreationAllowed(employeeWorkplaceCreationAllowed);
		applicationSettings.setWorksiteChangeDuringShiftAllowed(worksiteChangeDuringShiftAllowed);
		return applicationSettings;
	}

	@Transactional(readOnly = true)
	public ApplicationSettings getApplicationSettings() {
		logger.debug("Finding application settings");
		return this.applicationSettingsRepository.findById(ApplicationSettings.GLOBAL_SETTINGS_ID)
				.orElseThrow(() -> new IllegalStateException("Global application settings row is missing"));
	}

	@Transactional(readOnly = true)
	public int getDaysUntilLocked() {
		return this.getApplicationSettings().getDaysUntilLocked();
	}

	@Transactional(readOnly = true)
	public boolean isEmployeeWorkplaceCreationAllowed() {
		return this.getApplicationSettings().isEmployeeWorkplaceCreationAllowed();
	}

	@Transactional(readOnly = true)
	public boolean isWorksiteChangeDuringShiftAllowed() {
		return this.getApplicationSettings().isWorksiteChangeDuringShiftAllowed();
	}
}
