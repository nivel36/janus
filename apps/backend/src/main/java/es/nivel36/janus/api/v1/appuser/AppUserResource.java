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
package es.nivel36.janus.api.v1.appuser;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import es.nivel36.janus.service.ResourceNotFoundException;
import jakarta.validation.ConstraintViolationException;

/**
 * HTTP contract for local application profiles at {@code /api/v1/app-users}.
 * Requests require a validated bearer JWT. The current-profile operation may
 * provision an account; other operations require a previously provisioned
 * actor. Authorization precedes execution. Identity-provider accounts and
 * employee records are never created or deleted by this resource.
 */
@RequestMapping({ "/api/v1/app-users" })
public interface AppUserResource {

	/**
	 * Returns a page of local profiles for a provisioned {@code JANUS_ADMIN}.
	 * <p>
	 * The optional employee number must match {@code [A-Za-z0-9_-]{1,50}} without
	 * trimming and {@code pageable} must be nonnull and paged. Email fragments are
	 * used literally without trimming and limited to 254 characters; null or empty
	 * email disables the email restriction. Sort fields are {@code id},
	 * {@code email} and {@code employeeNumber}.
	 * </p>
	 * <p>
	 * Returns HTTP 200 without changing profiles. Filters combine with AND; absent
	 * filters include unlinked profiles. Email matching is partial,
	 * case-insensitive and literal. Employee-number matching is exact and
	 * case-sensitive. Page size is capped at the configured
	 * {@code spring.data.rest.max-page-size} (100 by default), with ascending email
	 * as the default sort and an ascending UUID tie-breaker unless UUID is
	 * explicitly sorted.
	 * </p>
	 *
	 * @param  email                        optional email fragment; null disables
	 *                                      this filter
	 * @param  employeeNumber               optional employee number; null disables
	 *                                      this filter
	 * @param  pageable                     requested page and ordering; HTTP
	 *                                      defaults are page 0 and size 20
	 * @return                              HTTP 200 containing profile responses
	 *                                      and page metadata, possibly empty
	 * @throws ConstraintViolationException if employee number is invalid
	 * @throws IllegalArgumentException     if paging or a sort field is unsupported
	 * @throws AccessDeniedException        if the caller is unprovisioned or lacks
	 *                                      the administrator role
	 */
	@GetMapping
	ResponseEntity<Page<AppUserResponse>> searchAppUsers(@RequestParam(name = "email", required = false)
	String email, @RequestParam(required = false)
	String employeeNumber, Pageable pageable);

	/**
	 * Retrieves or provisions the profile identified by the authenticated subject.
	 * <p>
	 * Authentication must be a nonnull validated JWT with a recognized Janus role
	 * and verified email. Its subject must match {@code [A-Za-z0-9_-]{1,255}}, its
	 * email must be nonblank and satisfy {@code @Email}, and an optional
	 * employee-number claim must match {@code [A-Za-z0-9_-]{1,50}} without
	 * trimming. Creation trims and lowercases email and limits it to 254 normalized
	 * characters.
	 * </p>
	 * <p>
	 * Returns HTTP 200 for the subject's unique local profile. Existing profiles
	 * retain their contact email, preferences and employee association. New
	 * profiles use configured preferences and DARK theme; they link only a known,
	 * unclaimed employee. Missing, unknown or already claimed employee numbers
	 * yield an unlinked profile. Concurrent creation returns the winning subject
	 * profile without reassigning another profile's employee.
	 * </p>
	 *
	 * @param  authentication               trusted JWT authentication of the
	 *                                      current caller
	 * @return                              HTTP 200 containing the current profile;
	 *                                      the provider subject is omitted
	 * @throws ConstraintViolationException if a claim violates the service
	 *                                      parameter constraints
	 * @throws IllegalArgumentException     if a new profile's normalized email
	 *                                      exceeds 254 characters
	 * @throws AccessDeniedException        if provisioning is not authorized
	 */
	@GetMapping("/me")
	ResponseEntity<AppUserResponse> findCurrentAppUser(JwtAuthenticationToken authentication);

	/**
	 * Replaces all preferences of an authorized local profile.
	 * <p>
	 * Both id and request must be nonnull, the target must exist, and the payload
	 * must pass Bean Validation. A provisioned administrator may edit any profile;
	 * JANUS_USER and JANUS_EMPLOYEE may edit only their own persistent UUID.
	 * </p>
	 * <p>
	 * Returns HTTP 200 with the persisted locale, time format, timezone and theme.
	 * Subject, contact email and employee association are preserved. Invalid
	 * requests and authorization denials do not modify profiles.
	 * </p>
	 *
	 * @param  id                        persistent UUID of the target profile
	 * @param  request                   complete validated preference replacement
	 * @return                           HTTP 200 containing the updated profile
	 * @throws ResourceNotFoundException if the target is absent
	 * @throws AccessDeniedException     if the caller cannot update the target
	 */
	@PutMapping("/{id}")
	ResponseEntity<AppUserResponse> updateAppUser(@PathVariable
	UUID id, @RequestBody
	UpdateAppUserRequest request);

	/**
	 * Deletes a local profile as a provisioned administrator.
	 * <p>
	 * The id must be nonnull, the target must exist, and the caller must have
	 * JANUS_ADMIN.
	 * </p>
	 * <p>
	 * Returns HTTP 204 with no body after removing the profile and clearing its
	 * employee association. Employee data and the provider account are preserved.
	 * Deletion does not revoke JWTs; later authorized access to {@code /me} may
	 * provision a new profile.
	 * </p>
	 *
	 * @param  id                        persistent UUID of the profile to delete
	 * @return                           HTTP 204 with an empty body
	 * @throws ResourceNotFoundException if the profile is absent
	 * @throws AccessDeniedException     if the caller is not a provisioned
	 *                                   administrator
	 */
	@DeleteMapping("/{id}")
	ResponseEntity<Void> deleteAppUser(@PathVariable
	UUID id);
}
