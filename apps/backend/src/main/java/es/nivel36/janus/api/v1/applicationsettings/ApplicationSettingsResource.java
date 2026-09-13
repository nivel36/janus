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

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.validation.Valid;

@RequestMapping("/api/v1/applicationsettings")
public interface ApplicationSettingsResource {

	@PreAuthorize("@applicationSettingsAuthorization.canView(authentication)")
	@GetMapping
	ResponseEntity<ApplicationSettingsResponse> findApplicationSettings();

	@PreAuthorize("@applicationSettingsAuthorization.canUpdate(authentication)")
	@PutMapping
	ResponseEntity<ApplicationSettingsResponse> updateApplicationSettings(
			@Valid @RequestBody UpdateApplicationSettingsRequest request);
}
