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

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import es.nivel36.janus.service.applicationsettings.MissingApplicationSettingsException;

import jakarta.validation.Valid;

/**
 * HTTP contract for reading and replacing global application settings.
 * <p>
 * Operations require an authenticated identity with an existing application
 * profile and the roles specified for each operation.
 */
@RequestMapping({ "/api/v1/application-settings" })
public interface ApplicationSettingsResource {

	/**
	 * Returns the current global application settings.
	 * <p>
	 * Access requires the {@code JANUS_EMPLOYEE}, {@code JANUS_USER}, or
	 * {@code JANUS_ADMIN} role.
	 *
	 * @return                                     an HTTP {@code 200 OK} response
	 *                                             containing the current settings
	 * @throws MissingApplicationSettingsException if the global application
	 *                                             settings do not exist
	 */
	@GetMapping
	@PreAuthorize("@applicationSettingsAuthorization.canView(authentication)")
	ResponseEntity<ApplicationSettingsResponse> findApplicationSettings();

	/**
	 * Replaces all global application settings with the supplied values.
	 * <p>
	 * Access requires the {@code JANUS_ADMIN} role. HTTP requests are validated
	 * against the constraints on {@link UpdateApplicationSettingsRequest}; invalid
	 * payloads produce an HTTP {@code 400 Bad Request} response without changing
	 * the settings. Leading and trailing whitespace in the time-zone identifier is
	 * removed before the zone is stored.
	 *
	 * @param  request                             the complete replacement
	 *                                             settings; must not be
	 *                                             {@code null} and must satisfy the
	 *                                             request validation constraints
	 * @return                                     an HTTP {@code 200 OK} response
	 *                                             containing the updated settings
	 * @throws MissingApplicationSettingsException if the global application
	 *                                             settings do not exist
	 */
	@PutMapping
	@PreAuthorize("@applicationSettingsAuthorization.canUpdate(authentication)")
	ResponseEntity<ApplicationSettingsResponse> updateApplicationSettings(
			@RequestBody
			@Valid
			UpdateApplicationSettingsRequest request);
}
