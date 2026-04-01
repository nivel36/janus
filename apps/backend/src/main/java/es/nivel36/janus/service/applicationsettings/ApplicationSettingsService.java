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

import java.time.ZoneId;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service responsible for managing and retrieving global
 * {@link ApplicationSettings}.
 * 
 */
@Service
public class ApplicationSettingsService {

	private static final Logger logger = LoggerFactory.getLogger(ApplicationSettingsService.class);

	private final ApplicationSettingsRepository applicationSettingsRepository;

	/**
	 * Constructs the service with the required repository.
	 *
	 * @param applicationSettingsRepository repository used to manage application
	 *                                      settings. Can't be {@code null}.
	 * @throws NullPointerException if {@code applicationSettingsRepository} is
	 *                              {@code null}.
	 */
	public ApplicationSettingsService(final ApplicationSettingsRepository applicationSettingsRepository) {
		this.applicationSettingsRepository = Objects.requireNonNull(applicationSettingsRepository,
				"applicationSettingsRepository cannot be null");
	}

	/**
	 * Updates the global {@link ApplicationSettings} with the provided values.
	 *
	 * <p>
	 * The existing settings are retrieved using
	 * {@link ApplicationSettings#GLOBAL_SETTINGS_ID} and then updated with the
	 * supplied parameters.
	 *
	 * @param daysUntilLocked                  number of days before an entity
	 *                                         becomes locked.
	 * @param employeeWorkplaceCreationAllowed whether employees are allowed to
	 *                                         create workplaces.
	 * @param worksiteChangeDuringShiftAllowed whether worksite changes are allowed
	 *                                         during a shift.
	 * @param defaultTimezone                  default application time zone.
	 * @return the updated {@link ApplicationSettings} instance.
	 * @throws IllegalStateException if the global application settings entry does
	 *                               not exist.
	 */
	@Transactional
	public ApplicationSettings update(final int daysUntilLocked, final boolean employeeWorkplaceCreationAllowed,
			final boolean worksiteChangeDuringShiftAllowed, final ZoneId defaultTimezone) {
		logger.debug("Updating application settings");
		final ApplicationSettings applicationSettings = this.findById();
		applicationSettings.setDaysUntilLocked(daysUntilLocked);
		applicationSettings.setEmployeeWorkplaceCreationAllowed(employeeWorkplaceCreationAllowed);
		applicationSettings.setWorksiteChangeDuringShiftAllowed(worksiteChangeDuringShiftAllowed);
		applicationSettings.setDefaultTimezone(defaultTimezone);
		return applicationSettings;
	}

	/**
	 * Retrieves the global {@link ApplicationSettings}.
	 *
	 * @return the current global {@link ApplicationSettings}.
	 * @throws IllegalStateException if the global application settings entry does
	 *                               not exist.
	 */
	@Transactional(readOnly = true)
	public ApplicationSettings findApplicationSettings() {
		logger.debug("Finding application settings");
		return this.findById();
	}

	private ApplicationSettings findById() {
		return this.applicationSettingsRepository.findById(ApplicationSettings.GLOBAL_SETTINGS_ID)
				.orElseThrow(() -> new IllegalStateException("Global application settings row is missing"));
	}

	/**
	 * Retrieves the number of days before entities become locked.
	 *
	 * @return the number of days until locked.
	 * @throws IllegalStateException if the global application settings entry does
	 *                               not exist.
	 */
	@Transactional(readOnly = true)
	public int getDaysUntilLocked() {
		return this.findById().getDaysUntilLocked();
	}

	/**
	 * Indicates whether employees are allowed to create workplaces.
	 *
	 * @return {@code true} if workplace creation is allowed for employees;
	 *         {@code false} otherwise.
	 * @throws IllegalStateException if the global application settings entry does
	 *                               not exist.
	 */
	@Transactional(readOnly = true)
	public boolean isEmployeeWorkplaceCreationAllowed() {
		return this.findById().isEmployeeWorkplaceCreationAllowed();
	}

	/**
	 * Indicates whether worksite changes are allowed during a shift.
	 *
	 * @return {@code true} if worksite changes during a shift are allowed;
	 *         {@code false} otherwise.
	 * @throws IllegalStateException if the global application settings entry does
	 *                               not exist.
	 */
	@Transactional(readOnly = true)
	public boolean isWorksiteChangeDuringShiftAllowed() {
		return this.findById().isWorksiteChangeDuringShiftAllowed();
	}

	@Transactional(readOnly = true)
	public ZoneId getDefaultTimezone() {
		return this.findById().getDefaultTimezone();
	}
}
