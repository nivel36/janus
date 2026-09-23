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
package es.nivel36.janus.api.v1.employee;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;

@RequestMapping("/api/v1/employees")
public interface EmployeeResource {

	@PreAuthorize("@employeeAuthorization.canView(authentication, #employeeNumber)")
	@GetMapping("/{employeeNumber}")
	ResponseEntity<EmployeeResponse> findEmployee(
			@PathVariable("employeeNumber") @Pattern(regexp = "[A-Za-z0-9_-]{1,50}") String employeeNumber);

	@PreAuthorize("@employeeAuthorization.canCreate(authentication)")
	@PostMapping
	ResponseEntity<EmployeeResponse> createEmployee(@Valid @RequestBody CreateEmployeeRequest request);

	@PreAuthorize("@employeeAuthorization.canUpdate(authentication, #employeeNumber)")
	@PutMapping("/{employeeNumber}")
	ResponseEntity<EmployeeResponse> updateEmployee(
			@PathVariable("employeeNumber") @Pattern(regexp = "[A-Za-z0-9_-]{1,50}") String employeeNumber,
			@Valid @RequestBody UpdateEmployeeRequest request);

	@PreAuthorize("@employeeAuthorization.canDelete(authentication)")
	@DeleteMapping("/{employeeNumber}")
	ResponseEntity<Void> deleteEmployee(
			@PathVariable("employeeNumber") @Pattern(regexp = "[A-Za-z0-9_-]{1,50}") String employeeNumber);
}
