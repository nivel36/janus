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
package es.nivel36.janus.service.applicationsettings;

import jakarta.validation.ConstraintViolationException;

import java.time.ZoneId;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Service responsible for managing and retrieving global
 * {@link ApplicationSettings}.
 */
@Validated
@Service
public class ApplicationSettingsService {

	private static final Logger logger = LoggerFactory.getLogger(ApplicationSettingsService.class);

	private final ApplicationSettingsRepository applicationSettingsRepository;

	/**
	 * Constructs the service with the required repository.
	 *
	 * @param  applicationSettingsRepository repository used to manage application
	 *                                       settings. Must not be {@code null}.
	 * @throws NullPointerException          if
	 *                                       {@code applicationSettingsRepository}
	 *                                       is {@code null}.
	 */
	public ApplicationSettingsService(final ApplicationSettingsRepository applicationSettingsRepository) {
		this.applicationSettingsRepository = Objects
				.requireNonNull(applicationSettingsRepository, "applicationSettingsRepository cannot be null");
	}

	/**
	 * Replaces all values of the existing global application settings.
	 * <p>
	 * The complete input is validated before any configuration value is changed.
	 * This operation does not create the global configuration if it is absent.
	 *
	 * @param  daysUntilLocked                     the non-negative time-log
	 *                                             modification window in days
	 * @param  employeeWorksiteCreationAllowed     whether employees may create
	 *                                             personal worksites
	 * @param  worksiteChangeDuringShiftAllowed    whether clock-out at another
	 *                                             worksite is allowed
	 * @param  employeeManualTimeLogEntryAllowed   whether employees may supply
	 *                                             manual timestamps
	 * @param  defaultTimezone                     the stored global time zone; must
	 *                                             not be {@code null}
	 * @return                                     the updated global configuration
	 * @throws MissingApplicationSettingsException if the global configuration is
	 *                                             absent
	 * @throws ConstraintViolationException        if the modification window is
	 *                                             negative or the zone is
	 *                                             {@code null} when method
	 *                                             validation is active
	 * @throws IllegalArgumentException            if the window is negative without
	 *                                             method validation
	 * @throws NullPointerException                if the zone is {@code null}
	 *                                             without method validation
	 */
	@Transactional
	public ApplicationSettings update(
			final @PositiveOrZero int daysUntilLocked,
			final boolean employeeWorksiteCreationAllowed,
			final boolean worksiteChangeDuringShiftAllowed,
			final boolean employeeManualTimeLogEntryAllowed,
			final @NotNull ZoneId defaultTimezone) {
		logger.atDebug().addKeyValue("daysUntilLocked", daysUntilLocked)
				.addKeyValue("employeeWorksiteCreationAllowed", employeeWorksiteCreationAllowed)
				.addKeyValue("worksiteChangeDuringShiftAllowed", worksiteChangeDuringShiftAllowed)
				.addKeyValue("employeeManualTimeLogEntryAllowed", employeeManualTimeLogEntryAllowed)
				.addKeyValue("defaultTimezone", defaultTimezone).log("Updating application settings");
		final ApplicationSettings applicationSettings = this.findById();

		applicationSettings.update(
				daysUntilLocked,
				employeeWorksiteCreationAllowed,
				worksiteChangeDuringShiftAllowed,
				employeeManualTimeLogEntryAllowed,
				defaultTimezone);
		return applicationSettings;
	}

	/**
	 * Retrieves the global {@link ApplicationSettings}.
	 *
	 * @return                                     the current global
	 *                                             {@link ApplicationSettings}.
	 * @throws MissingApplicationSettingsException if the global application
	 *                                             settings entry does not exist.
	 */
	@Transactional(readOnly = true)
	public ApplicationSettings findApplicationSettings() {
		return this.findById();
	}

	private ApplicationSettings findById() {
		return this.applicationSettingsRepository.findById(ApplicationSettings.GLOBAL_SETTINGS_ID)
				.orElseThrow(MissingApplicationSettingsException::new);
	}

	/**
	 * Returns the configured time-log modification window in days.
	 *
	 * @return                                     the number of days until locked.
	 * @throws MissingApplicationSettingsException if the global application
	 *                                             settings entry does not exist.
	 */
	@Transactional(readOnly = true)
	public int getDaysUntilLocked() {
		return this.findById().getDaysUntilLocked();
	}

	/**
	 * Indicates whether employees are allowed to create personal worksites.
	 *
	 * @return                                     {@code true} if personal worksite
	 *                                             creation is allowed for
	 *                                             employees; {@code false}
	 *                                             otherwise.
	 * @throws MissingApplicationSettingsException if the global application
	 *                                             settings entry does not exist.
	 */
	@Transactional(readOnly = true)
	public boolean isEmployeeWorksiteCreationAllowed() {
		return this.findById().isEmployeeWorksiteCreationAllowed();
	}

	/**
	 * Indicates whether worksite changes are allowed during a shift.
	 *
	 * @return                                     {@code true} if worksite changes
	 *                                             during a shift are allowed;
	 *                                             {@code false} otherwise.
	 * @throws MissingApplicationSettingsException if the global application
	 *                                             settings entry does not exist.
	 */
	@Transactional(readOnly = true)
	public boolean isWorksiteChangeDuringShiftAllowed() {
		return this.findById().isWorksiteChangeDuringShiftAllowed();
	}

	/**
	 * Indicates whether employees are allowed to create manual timelog entries with
	 * explicit timestamps.
	 *
	 * @return                                     {@code true} if manual timelog
	 *                                             entry is allowed; {@code false}
	 *                                             otherwise.
	 * @throws MissingApplicationSettingsException if the global application
	 *                                             settings entry does not exist.
	 */
	@Transactional(readOnly = true)
	public boolean isEmployeeManualTimeLogEntryAllowed() {
		return this.findById().isEmployeeManualTimeLogEntryAllowed();
	}

	/**
	 * Returns the stored global time zone; does not override worksite or account
	 * zones.
	 *
	 * @return                                     ZoneId with the stored global
	 *                                             time zone
	 * @throws MissingApplicationSettingsException if the global settings row is
	 *                                             missing
	 */
	@Transactional(readOnly = true)
	public ZoneId getDefaultTimezone() {
		return this.findById().getDefaultTimezone();
	}
}
