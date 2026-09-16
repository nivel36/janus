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
package es.nivel36.janus.api.v1.appuser;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;

@RequestMapping({ "/api/v1/appusers", "/api/v1/app-users" })
public interface AppUserResource {

	@PreAuthorize("@appUserProvisioningPolicy.canProvision(authentication)")
	@GetMapping("/me")
	ResponseEntity<AppUserResponse> findCurrentAppUser(JwtAuthenticationToken authentication);

	@PreAuthorize("@appUserAuthorization.canUpdateCurrent(authentication)")
	@PutMapping("/me")
	ResponseEntity<AppUserResponse> updateCurrentAppUser( //
			@Valid @RequestBody UpdateAppUserRequest request, //
			Authentication authentication);

	@PreAuthorize("@appUserAuthorization.canDelete(authentication)")
	@DeleteMapping("/{username}")
	ResponseEntity<Void> deleteAppUser( //
			@PathVariable("username") //
			@Pattern(regexp = "[A-Za-z0-9_.@-]{3,50}", message = "username must contain only letters, digits, dots, underscores, hyphens or at signs (3-50 characters)") //
			String username);

	@PreAuthorize("@appUserAuthorization.canDelete(authentication)")
	@PutMapping("/{username}/keycloak-subject")
	ResponseEntity<AppUserResponse> replaceKeycloakSubject(
			@PathVariable("username") @Pattern(regexp = "[A-Za-z0-9_.@-]{3,50}") String username,
			@Valid @RequestBody ReplaceKeycloakSubjectRequest request);
}
