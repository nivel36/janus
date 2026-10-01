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

import es.nivel36.janus.api.validation.WorksiteCode;

import java.time.Instant;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import es.nivel36.janus.api.validation.EmployeeNumber;
import jakarta.validation.Valid;

@RequestMapping("/api/v1")
public interface ClockOutWithoutClockInEventResource {

	@PatchMapping("/employees/{employeeNumber}/worksites/{worksiteCode}/clock-out-without-clock-in-events/{exitTime}")
	@PreAuthorize("@clockOutWithoutClockInEventAuthorization.canTransition(authentication, #employeeNumber, #request.action())")
	ResponseEntity<ClockOutWithoutClockInEventResponse> transitionClockOutWithoutClockInEvent(
			@PathVariable("employeeNumber") @EmployeeNumber String employeeNumber,
			@PathVariable("worksiteCode") @WorksiteCode String worksiteCode,
			@PathVariable("exitTime") Instant exitTime,
			@Valid @RequestBody TransitionClockOutWithoutClockInEventRequest request);

	@GetMapping("/employees/{employeeNumber}/worksites/{worksiteCode}/clock-out-without-clock-in-events/{exitTime}")
	@PreAuthorize("@clockOutWithoutClockInEventAuthorization.canView(authentication, #employeeNumber)")
	ResponseEntity<ClockOutWithoutClockInEventResponse> findClockOutWithoutClockInEvent(
			@PathVariable("employeeNumber") @EmployeeNumber String employeeNumber,
			@PathVariable("worksiteCode") @WorksiteCode String worksiteCode,
			@PathVariable("exitTime") Instant exitTime);

}
