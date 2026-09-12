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

import java.time.ZoneId;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.nivel36.janus.api.Mapper;
import es.nivel36.janus.service.applicationsettings.ApplicationSettings;
import es.nivel36.janus.service.applicationsettings.ApplicationSettingsService;
import jakarta.validation.Valid;

/**
 * REST controller exposing read and update operations for global application
 * settings.
 */
@RestController
@RequestMapping("/api/v1/applicationsettings")
public class ApplicationSettingsController {

	private static final Logger logger = LoggerFactory.getLogger(ApplicationSettingsController.class);

	private final ApplicationSettingsService applicationSettingsService;
	private final Mapper<ApplicationSettings, ApplicationSettingsResponse> applicationSettingsResponseMapper;

	/**
	 * Builds a controller for managing global {@link ApplicationSettings}.
	 *
	 * @param applicationSettingsService        service handling application
	 *                                          settings operations; must not be
	 *                                          {@code null}
	 * @param applicationSettingsResponseMapper mapper translating
	 *                                          {@link ApplicationSettings} entities
	 *                                          into
	 *                                          {@link ApplicationSettingsResponse}
	 *                                          DTOs; must not be {@code null}
	 */
	public ApplicationSettingsController(final ApplicationSettingsService applicationSettingsService,
			final @Qualifier("applicationSettingsResponseMapper") Mapper<ApplicationSettings, ApplicationSettingsResponse> applicationSettingsResponseMapper) {
		this.applicationSettingsService = Objects.requireNonNull(applicationSettingsService,
				"applicationSettingsService can't be null");
		this.applicationSettingsResponseMapper = Objects.requireNonNull(applicationSettingsResponseMapper,
				"applicationSettingsResponseMapper can't be null");
	}

	/**
	 * Retrieves the global application settings.
	 *
	 * @return a {@link ResponseEntity} containing the current application settings
	 */
	@PreAuthorize("@applicationSettingsAuthorization.canView(authentication)")
	@GetMapping
	public ResponseEntity<ApplicationSettingsResponse> findApplicationSettings() {
		logger.debug("Find application settings ACTION performed");
		final ApplicationSettings applicationSettings = this.applicationSettingsService.findApplicationSettings();
		return ResponseEntity.ok(this.applicationSettingsResponseMapper.map(applicationSettings));
	}

	/**
	 * Updates the global application settings.
	 *
	 * @param request the payload describing the new settings; must not be
	 *                {@code null}
	 * @return a {@link ResponseEntity} containing the updated application settings
	 */
	@PreAuthorize("@applicationSettingsAuthorization.canUpdate(authentication)")
	@PutMapping
	public ResponseEntity<ApplicationSettingsResponse> updateApplicationSettings(
			@Valid @RequestBody final UpdateApplicationSettingsRequest request) {
		logger.debug("Update application settings ACTION performed");
		final int daysUntilLocked = request.daysUntilLocked();
		final boolean employeeWorkplaceCreationAllowed = request.employeeWorkplaceCreationAllowed();
		final boolean worksiteChangeDuringShiftAllowed = request.worksiteChangeDuringShiftAllowed();
		final boolean employeeManualTimelogEntryAllowed = request.employeeManualTimelogEntryAllowed();
		final ZoneId zoneId = ZoneId.of(request.defaultTimezone().trim());
		final ApplicationSettings updatedSettings = this.applicationSettingsService.update(daysUntilLocked,
				employeeWorkplaceCreationAllowed, worksiteChangeDuringShiftAllowed, employeeManualTimelogEntryAllowed,
				zoneId);
		return ResponseEntity.ok(this.applicationSettingsResponseMapper.map(updatedSettings));
	}
}
