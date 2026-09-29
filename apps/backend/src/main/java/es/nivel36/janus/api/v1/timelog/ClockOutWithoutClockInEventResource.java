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
package es.nivel36.janus.api.v1.timelog;

import java.time.Instant;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import es.nivel36.janus.api.validation.EmployeeNumber;

@RequestMapping("/api/v1")
public interface ClockOutWithoutClockInEventResource {

	@PostMapping("/employees/{employeeNumber}/clock-out-without-clock-in-events/{exitTime}/resolve")
	@PreAuthorize("@clockOutWithoutClockInEventAuthorization.canResolve(authentication, #employeeNumber)")
	ResponseEntity<ClockOutWithoutClockInEventResponse> resolveClockOutWithoutClockInEvent(
			@PathVariable("employeeNumber") @EmployeeNumber String employeeNumber,
			@RequestParam("worksiteCode") @Pattern(regexp = "[A-Za-z0-9_-]{1,50}", message = "code must contain only letters, digits, underscores or hyphens (max 50)") String worksiteCode,
			@PathVariable("exitTime") Instant exitTime,
			@Valid @RequestBody ResolveClockOutWithoutClockInEventRequest request);

	@PostMapping("/employees/{employeeNumber}/clock-out-without-clock-in-events/{exitTime}/invalidate")
	@PreAuthorize("@clockOutWithoutClockInEventAuthorization.canInvalidate(authentication, #employeeNumber)")
	ResponseEntity<ClockOutWithoutClockInEventResponse> invalidateClockOutWithoutClockInEvent(
			@PathVariable("employeeNumber") @EmployeeNumber String employeeNumber,
			@RequestParam("worksiteCode") @Pattern(regexp = "[A-Za-z0-9_-]{1,50}", message = "code must contain only letters, digits, underscores or hyphens (max 50)") String worksiteCode,
			@PathVariable("exitTime") Instant exitTime,
			@RequestBody(required = false) @Valid InvalidateClockOutWithoutClockInEventRequest request);

	@GetMapping("/employees/{employeeNumber}/clock-out-without-clock-in-events/{exitTime}")
	@PreAuthorize("@clockOutWithoutClockInEventAuthorization.canView(authentication, #employeeNumber)")
	ResponseEntity<ClockOutWithoutClockInEventResponse> findClockOutWithoutClockInEvent(
			@PathVariable("employeeNumber") @EmployeeNumber String employeeNumber,
			@RequestParam("worksiteCode") @Pattern(regexp = "[A-Za-z0-9_-]{1,50}", message = "code must contain only letters, digits, underscores or hyphens (max 50)") String worksiteCode,
			@PathVariable("exitTime") Instant exitTime);

	@Deprecated
	@PostMapping("/employees/by-email/{email}/clock-out-without-clock-in-events/{exitTime}/resolve")
	@PreAuthorize("@clockOutWithoutClockInEventAuthorization.canResolveByEmail(authentication, #email)")
	ResponseEntity<ClockOutWithoutClockInEventResponse> resolveClockOutWithoutClockInEventByEmail(
			@PathVariable @Email String email,
			@RequestParam("worksiteCode") @Pattern(regexp = "[A-Za-z0-9_-]{1,50}", message = "code must contain only letters, digits, underscores or hyphens (max 50)") String worksiteCode,
			@PathVariable Instant exitTime, @Valid @RequestBody ResolveClockOutWithoutClockInEventRequest request);

	@Deprecated
	@PostMapping("/employees/by-email/{email}/clock-out-without-clock-in-events/{exitTime}/invalidate")
	@PreAuthorize("@clockOutWithoutClockInEventAuthorization.canInvalidateByEmail(authentication, #email)")
	ResponseEntity<ClockOutWithoutClockInEventResponse> invalidateClockOutWithoutClockInEventByEmail(
			@PathVariable @Email String email,
			@RequestParam("worksiteCode") @Pattern(regexp = "[A-Za-z0-9_-]{1,50}", message = "code must contain only letters, digits, underscores or hyphens (max 50)") String worksiteCode,
			@PathVariable Instant exitTime,
			@RequestBody(required = false) @Valid InvalidateClockOutWithoutClockInEventRequest request);

	@Deprecated
	@GetMapping("/employees/by-email/{email}/clock-out-without-clock-in-events/{exitTime}")
	@PreAuthorize("@clockOutWithoutClockInEventAuthorization.canViewByEmail(authentication, #email)")
	ResponseEntity<ClockOutWithoutClockInEventResponse> findClockOutWithoutClockInEventByEmail(
			@PathVariable @Email String email,
			@RequestParam("worksiteCode") @Pattern(regexp = "[A-Za-z0-9_-]{1,50}", message = "code must contain only letters, digits, underscores or hyphens (max 50)") String worksiteCode,
			@PathVariable Instant exitTime);
}
