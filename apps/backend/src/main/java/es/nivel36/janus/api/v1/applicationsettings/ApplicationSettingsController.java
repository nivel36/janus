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

	public ApplicationSettingsController(final ApplicationSettingsService applicationSettingsService,
			final @Qualifier("applicationSettingsResponseMapper") Mapper<ApplicationSettings, ApplicationSettingsResponse> applicationSettingsResponseMapper) {
		this.applicationSettingsService = Objects.requireNonNull(applicationSettingsService,
				"applicationSettingsService can't be null");
		this.applicationSettingsResponseMapper = Objects.requireNonNull(applicationSettingsResponseMapper,
				"applicationSettingsResponseMapper can't be null");
	}

	@PreAuthorize("@applicationSettingsAuthorization.canView(authentication)")
	@GetMapping
	public ResponseEntity<ApplicationSettingsResponse> findApplicationSettings() {
		logger.debug("Find application settings ACTION performed");
		final ApplicationSettings applicationSettings = this.applicationSettingsService.findApplicationSettings();
		return ResponseEntity.ok(this.applicationSettingsResponseMapper.map(applicationSettings));
	}

	@PreAuthorize("@applicationSettingsAuthorization.canUpdate(authentication)")
	@PutMapping
	public ResponseEntity<ApplicationSettingsResponse> updateApplicationSettings(
			@Valid @RequestBody final UpdateApplicationSettingsRequest request) {
		logger.debug("Update application settings ACTION performed");
		final ApplicationSettings updatedSettings = this.applicationSettingsService.update(request.daysUntilLocked(),
				request.employeeWorkplaceCreationAllowed(), request.worksiteChangeDuringShiftAllowed(),
				request.employeeManualTimelogEntryAllowed(), ZoneId.of(request.defaultTimezone()));
		return ResponseEntity.ok(this.applicationSettingsResponseMapper.map(updatedSettings));
	}
}
