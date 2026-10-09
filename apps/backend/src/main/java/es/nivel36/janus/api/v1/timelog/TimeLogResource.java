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
package es.nivel36.janus.api.v1.timelog;

import java.time.Instant;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import es.nivel36.janus.service.timelog.ClockOutWithoutClockInException;
import es.nivel36.janus.validation.EmployeeNumber;
import es.nivel36.janus.validation.WorksiteCode;
import jakarta.validation.Valid;

@RequestMapping("/api/v1")
public interface TimeLogResource {
	@PostMapping({ "/employees/{employeeNumber}/time-logs/clock-in" })
	@PreAuthorize("@timeLogAuthorization.canOperate(authentication, #employeeNumber, #entryTime != null)")
	ResponseEntity<TimeLogResponse> clockIn(
			@PathVariable("employeeNumber")
			@EmployeeNumber
			String employeeNumber,
			@RequestParam(value = "entryTime", required = false)
			Instant entryTime,
			@RequestParam("worksiteCode")
			@WorksiteCode
			String worksiteCode);

	@PostMapping({ "/employees/{employeeNumber}/time-logs/clock-out" })
	@PreAuthorize("@timeLogAuthorization.canOperate(authentication, #employeeNumber, #exitTime != null)")
	ResponseEntity<TimeLogResponse> clockOut(
			@PathVariable("employeeNumber")
			@EmployeeNumber
			String employeeNumber,
			@RequestParam(value = "exitTime", required = false)
			Instant exitTime,
			@RequestParam("worksiteCode")
			@WorksiteCode
			String worksiteCode) throws ClockOutWithoutClockInException;

	@PostMapping({ "/employees/{employeeNumber}/time-logs" })
	@PreAuthorize("@timeLogAuthorization.canOperate(authentication, #employeeNumber, true)")
	ResponseEntity<TimeLogResponse> createTimeLog(
			@PathVariable("employeeNumber")
			@EmployeeNumber
			String employeeNumber,
			@RequestParam("worksiteCode")
			@WorksiteCode
			String worksiteCode,
			@RequestBody
			@Valid
			CreateTimeLogRequest timeLog);

	@GetMapping({ "/employees/{employeeNumber}/time-logs/{entryTime}" })
	@PreAuthorize("@timeLogAuthorization.canView(authentication, #employeeNumber)")
	ResponseEntity<TimeLogResponse> findTimeLogByEmployeeAndEntryTime(
			@PathVariable("employeeNumber")
			@EmployeeNumber
			String employeeNumber,
			@PathVariable("entryTime")
			Instant entryTime);

	@DeleteMapping({ "/employees/{employeeNumber}/time-logs/{entryTime}" })
	@PreAuthorize("@timeLogAuthorization.canDelete(authentication)")
	ResponseEntity<Void> deleteTimeLog(
			@PathVariable("employeeNumber")
			@EmployeeNumber
			String employeeNumber,
			@PathVariable("entryTime")
			Instant entryTime);

}
