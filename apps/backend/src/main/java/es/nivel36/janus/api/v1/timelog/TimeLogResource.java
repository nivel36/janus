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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import es.nivel36.janus.service.timelog.ClockOutWithoutClockInException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;

@RequestMapping("/api/v1/employees/{employeeEmail}/timelogs")
public interface TimeLogResource {

	@PreAuthorize("@timeLogAuthorization.canOperate(authentication, #employeeEmail, #entryTime != null)")
	@PostMapping("/clock-in")
	ResponseEntity<TimeLogResponse> clockIn(
			@PathVariable("employeeEmail") @Pattern(regexp = "^(?=.{1,254}$)[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "must be a valid and safe email address (max 254)") String employeeEmail,
			@RequestParam(value = "entryTime", required = false) Instant entryTime,
			@RequestParam("worksiteCode") @Pattern(regexp = "[A-Za-z0-9_-]{1,50}", message = "code must contain only letters, digits, underscores or hyphens (max 50)") String worksiteCode,
			Authentication authentication);

	@PreAuthorize("@timeLogAuthorization.canOperate(authentication, #employeeEmail, #exitTime != null)")
	@PostMapping("/clock-out")
	ResponseEntity<TimeLogResponse> clockOut(
			@PathVariable("employeeEmail") @Pattern(regexp = "^(?=.{1,254}$)[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "must be a valid and safe email address (max 254)") String employeeEmail,
			@RequestParam(value = "exitTime", required = false) Instant exitTime,
			@RequestParam("worksiteCode") @Pattern(regexp = "[A-Za-z0-9_-]{1,50}", message = "code must contain only letters, digits, underscores or hyphens (max 50)") String worksiteCode,
			Authentication authentication) throws ClockOutWithoutClockInException;

	@PreAuthorize("@timeLogAuthorization.canOperate(authentication, #employeeEmail, true)")
	@PostMapping({ "", "/" })
	ResponseEntity<TimeLogResponse> createTimeLog(
			@PathVariable("employeeEmail") @Pattern(regexp = "^(?=.{1,254}$)[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "must be a valid and safe email address (max 254)") String employeeEmail,
			@RequestParam("worksiteCode") @Pattern(regexp = "[A-Za-z0-9_-]{1,50}", message = "code must contain only letters, digits, underscores or hyphens (max 50)") String worksiteCode,
			@Valid @RequestBody CreateTimeLogRequest timeLog, Authentication authentication);

	@PreAuthorize("@timeLogAuthorization.canView(authentication, #employeeEmail)")
	@GetMapping("/{entryTime}")
	ResponseEntity<TimeLogResponse> findTimeLogByEmployeeAndEntryTime(
			@PathVariable("employeeEmail") @Pattern(regexp = "^(?=.{1,254}$)[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "must be a valid and safe email address (max 254)") String employeeEmail,
			@PathVariable("entryTime") Instant entryTime, Authentication authentication);

	@PreAuthorize("@timeLogAuthorization.canDelete(authentication)")
	@DeleteMapping("/{entryTime}")
	ResponseEntity<Void> deleteTimeLog(
			@PathVariable("employeeEmail") @Pattern(regexp = "^(?=.{1,254}$)[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "must be a valid and safe email address (max 254)") String employeeEmail,
			@PathVariable("entryTime") Instant entryTime);
}
