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
package es.nivel36.janus.api.v1.schedule;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;

@RequestMapping("/api/v1/schedules")
public interface ScheduleResource {

	@GetMapping
	@PreAuthorize("@scheduleAuthorization.canSearch(authentication, #employeeEmail)")
	ResponseEntity<Page<ScheduleResponse>> searchSchedules(
			@RequestParam(required = false) @Pattern(regexp = "[A-Za-z0-9_-]{1,50}", message = "query must contain only letters, digits, underscores or hyphens (max 50)") String query,
			@RequestParam(required = false) @Pattern(regexp = "^(?=.{1,254}$)[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "employeeEmail must be a valid and safe email address (max 254)") String employeeEmail,
			Pageable pageable, Authentication authentication);

	@PreAuthorize("@scheduleAuthorization.canView(authentication, #scheduleCode)")
	@GetMapping("/{scheduleCode}")
	ResponseEntity<ScheduleResponse> findSchedule(
			@PathVariable("scheduleCode") @Pattern(regexp = "[A-Za-z0-9_-]{1,50}", message = "code must contain only letters, digits, underscores or hyphens (max 50)") String scheduleCode);

	@PreAuthorize("@scheduleAuthorization.canCreate(authentication)")
	@PostMapping
	ResponseEntity<ScheduleResponse> createSchedule(@Valid @RequestBody CreateScheduleRequest request);

	@PreAuthorize("@scheduleAuthorization.canUpdate(authentication)")
	@PutMapping("/{scheduleCode}")
	ResponseEntity<ScheduleResponse> updateSchedule(
			@PathVariable("scheduleCode") @Pattern(regexp = "[A-Za-z0-9_-]{1,50}", message = "code must contain only letters, digits, underscores or hyphens (max 50)") String scheduleCode,
			@Valid @RequestBody UpdateScheduleRequest request);

	@PreAuthorize("@scheduleAuthorization.canDelete(authentication)")
	@DeleteMapping("/{scheduleCode}")
	ResponseEntity<Void> deleteSchedule(
			@PathVariable("scheduleCode") @Pattern(regexp = "[A-Za-z0-9_-]{1,50}", message = "code must contain only letters, digits, underscores or hyphens (max 50)") String scheduleCode);
}
