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
package es.nivel36.janus.service.worksite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import es.nivel36.janus.service.ResourceAlreadyExistsException;
import es.nivel36.janus.service.ResourceNotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import es.nivel36.janus.service.employee.Employee;
import es.nivel36.janus.service.employee.EmployeeService;
import es.nivel36.janus.service.schedule.Schedule;

@ExtendWith(MockitoExtension.class)
class WorksiteServiceTest {

	private @Mock WorksiteRepository worksiteRepository;
	private @Mock EmployeeService employeeService;
	private WorksiteService worksiteService;

	@BeforeEach
	void setUp() {
		this.worksiteService = new WorksiteService(this.worksiteRepository, this.employeeService, 100);
	}

	@Test
	void assertEmployeeCanUseWorksiteShouldAllowAssignedScopeWhenEmployeeIsAssigned() {
		final Employee employee = org.mockito.Mockito.mock(Employee.class);
		when(employee.getId()).thenReturn(42L);
		when(this.employeeService.findEmployeeByEmployeeNumber("EMP-1")).thenReturn(employee);
		when(this.worksiteRepository.findByCode("WS-1")).thenReturn(worksite(WorksiteScope.ASSIGNED));
		when(this.employeeService.isAssignedToWorksite(42L, "WS-1")).thenReturn(true);

		assertDoesNotThrow(() -> this.worksiteService.assertEmployeeCanUseWorksite("EMP-1", "WS-1"));
		verify(this.employeeService).isAssignedToWorksite(42L, "WS-1");
	}

	@Test
	void assertEmployeeCanUseWorksiteShouldRejectAssignedScopeWhenEmployeeIsNotAssigned() {
		final Employee employee = org.mockito.Mockito.mock(Employee.class);
		when(employee.getId()).thenReturn(42L);
		when(this.employeeService.findEmployeeByEmployeeNumber("EMP-1")).thenReturn(employee);
		when(this.worksiteRepository.findByCode("WS-1")).thenReturn(worksite(WorksiteScope.ASSIGNED));

		assertThatThrownBy(() -> this.worksiteService.assertEmployeeCanUseWorksite("EMP-1", "WS-1"))
				.isInstanceOf(WorksiteAccessDeniedException.class)
				.hasMessage("Employee EMP-1 cannot use assigned worksite WS-1 because it is not assigned");
		verify(this.employeeService).isAssignedToWorksite(42L, "WS-1");
	}

	private static Worksite worksite(final WorksiteScope scope) {
		return new Worksite("WS-1", "Worksite", ZoneId.of("UTC"), scope);
	}

	private static Employee employee() {
		return new Employee(
				"EMP-1",
				"Test",
				"Employee",
				"test@example.test",
				new Schedule("STD", "Standard", Duration.ZERO, Duration.ZERO));
	}

	@Test
	void globalWorksiteDoesNotRequireAnAssignmentLookup() {
		when(this.employeeService.findEmployeeByEmployeeNumber("EMP-1")).thenReturn(employee());
		when(this.worksiteRepository.findByCode("WS-1")).thenReturn(worksite(WorksiteScope.GLOBAL));

		assertDoesNotThrow(() -> this.worksiteService.assertEmployeeCanUseWorksite("EMP-1", "WS-1"));
		verify(this.employeeService, never()).isAssignedToWorksite(any(), any());
	}

	@Test
	void missingEmployeeCannotUseWorksite() {
		when(this.employeeService.findEmployeeByEmployeeNumber("MISSING"))
				.thenThrow(new ResourceNotFoundException("Employee not found"));

		assertThatThrownBy(() -> this.worksiteService.assertEmployeeCanUseWorksite("MISSING", "WS-1"))
				.isInstanceOf(ResourceNotFoundException.class);
		verifyNoInteractions(this.worksiteRepository);
		verify(this.employeeService, never()).isAssignedToWorksite(any(), any());
	}

	@Test
	void employeeCannotUseMissingWorksite() {
		when(this.employeeService.findEmployeeByEmployeeNumber("EMP-1")).thenReturn(employee());

		assertThatThrownBy(() -> this.worksiteService.assertEmployeeCanUseWorksite("EMP-1", "MISSING"))
				.isInstanceOf(ResourceNotFoundException.class);
		verify(this.employeeService, never()).isAssignedToWorksite(any(), any());
	}

	@Test
	void absentQueryUsesDefaultSortAndPreservesPage() {
		final Page<Worksite> page = new PageImpl<>(List.of(worksite(WorksiteScope.GLOBAL)));
		when(this.worksiteRepository.search("", null, PageRequest.of(3, 20, Sort.by("code")))).thenReturn(page);
		assertThat(this.worksiteService.searchWorksites(null, null, PageRequest.of(3, 20))).isSameAs(page);
	}

	@Test
	void searchEscapesWildcardsWithoutTrimmingAndCapsPageSize() {
		this.worksiteService.searchWorksites("  A%_!  ", "EMP-1", PageRequest.of(2, 500));
		verify(this.worksiteRepository).search("  A!%!_!!  ", "EMP-1", PageRequest.of(2, 100, Sort.by("code")));
	}

	@Test
	void searchPreservesSortOptionsAndAddsCodeTieBreaker() {
		final Sort.Order order = Sort.Order.desc("name").ignoreCase().nullsLast();
		this.worksiteService.searchWorksites(null, null, PageRequest.of(0, 10, Sort.by(order)));
		verify(this.worksiteRepository).search("", null, PageRequest.of(0, 10, Sort.by(order, Sort.Order.asc("code"))));
	}

	@Test
	void explicitCodeSortKeepsDirectionAndPositionWithoutDuplicateTieBreaker() {
		final PageRequest request = PageRequest.of(1, 10, Sort.by(Sort.Order.desc("code"), Sort.Order.asc("name")));
		this.worksiteService.searchWorksites(null, null, request);
		verify(this.worksiteRepository).search("", null, request);
	}

	@ParameterizedTest
	@ValueSource(strings = { "id", "deleted", "employees.employeeNumber", "timeLogs.entryTime", "unknown",
			"LENGTH(name)" })
	void internalAndUnknownSortFieldsAreRejectedBeforeRepositoryAccess(final String property) {
		assertThatThrownBy(
				() -> this.worksiteService.searchWorksites(null, null, PageRequest.of(0, 10, Sort.by(property))))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Unsupported Worksite sort field");
		verifyNoInteractions(this.worksiteRepository);
	}

	@Test
	void unpagedSearchIsRejectedBeforeRepositoryAccess() {
		assertThatThrownBy(() -> this.worksiteService.searchWorksites(null, null, Pageable.unpaged()))
				.isInstanceOf(IllegalArgumentException.class);
		verifyNoInteractions(this.worksiteRepository);
	}

	@ParameterizedTest
	@ValueSource(ints = { 0, -1 })
	void invalidPageSizeLimitIsRejected(final int limit) {
		assertThatThrownBy(() -> new WorksiteService(this.worksiteRepository, this.employeeService, limit))
				.isInstanceOf(IllegalArgumentException.class);
		verifyNoInteractions(this.worksiteRepository, this.employeeService);
	}

	@Test
	void creationPersistsAllFieldsWithoutAssigningEmployees() {
		when(this.worksiteRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
		final Worksite created = this.worksiteService.createWorksite(
				"WS-1",
				"Site",
				ZoneId.of("Europe/Madrid"),
				WorksiteScope.ASSIGNED,
				"Description",
				"Address");
		assertThat(created.getCode()).isEqualTo("WS-1");
		assertThat(created.getName()).isEqualTo("Site");
		assertThat(created.getTimeZone()).isEqualTo(ZoneId.of("Europe/Madrid"));
		assertThat(created.getScope()).isEqualTo(WorksiteScope.ASSIGNED);
		assertThat(created.getDescription()).isEqualTo("Description");
		assertThat(created.getAddress()).isEqualTo("Address");
		assertThat(created.getEmployees()).isEmpty();
		verify(this.worksiteRepository).save(created);
		verifyNoInteractions(this.employeeService);
	}

	@Test
	void duplicateCodeIsRejectedWithoutSaving() {
		when(this.worksiteRepository.existsByCode("WS-1")).thenReturn(true);
		assertThatThrownBy(
				() -> this.worksiteService
						.createWorksite("WS-1", "Site", ZoneId.of("UTC"), WorksiteScope.GLOBAL, null, null))
				.isInstanceOf(ResourceAlreadyExistsException.class);
		verify(this.worksiteRepository, never()).save(any());
	}

	@Test
	void missingWorksiteCannotBeFoundOrUpdated() {
		assertThatThrownBy(() -> this.worksiteService.findWorksiteByCode("MISSING"))
				.isInstanceOf(ResourceNotFoundException.class);
		assertThatThrownBy(
				() -> this.worksiteService
						.updateWorksite("MISSING", "Site", ZoneId.of("UTC"), WorksiteScope.GLOBAL, null, null))
				.isInstanceOf(ResourceNotFoundException.class);
		verify(this.worksiteRepository, never()).save(any());
	}

	@Test
	void updateExpandsScopeAndClearsOptionalFieldsWithoutChangingCodeOrAssignments() {
		final Worksite site = worksite(WorksiteScope.ASSIGNED);
		final Employee employee = employee();
		site.assignEmployee(employee);
		site.setDescription("Old description");
		site.setAddress("Old address");
		when(this.worksiteRepository.findByCode("WS-1")).thenReturn(site);
		when(this.worksiteRepository.save(site)).thenReturn(site);
		assertThat(
				this.worksiteService.updateWorksite(
						"WS-1",
						"Updated",
						ZoneId.of("Europe/Madrid"),
						WorksiteScope.GLOBAL,
						null,
						null))
				.isSameAs(site);
		assertThat(site.getCode()).isEqualTo("WS-1");
		assertThat(site.getName()).isEqualTo("Updated");
		assertThat(site.getTimeZone()).isEqualTo(ZoneId.of("Europe/Madrid"));
		assertThat(site.getScope()).isEqualTo(WorksiteScope.GLOBAL);
		assertThat(site.getDescription()).isNull();
		assertThat(site.getAddress()).isNull();
		assertThat(site.getEmployees()).containsExactly(employee);
		assertThat(employee.getWorksites()).containsExactly(site);
	}

	@Test
	void globalScopeCannotBeRestrictedAndRejectedUpdateIsNotSaved() {
		final Worksite site = worksite(WorksiteScope.GLOBAL);
		when(this.worksiteRepository.findByCode("WS-1")).thenReturn(site);
		assertThatThrownBy(
				() -> this.worksiteService
						.updateWorksite("WS-1", "Updated", ZoneId.of("UTC"), WorksiteScope.ASSIGNED, null, null))
				.isInstanceOf(IllegalArgumentException.class);
		assertThat(site.getName()).isEqualTo("Worksite");
		assertThat(site.getTimeZone()).isEqualTo(ZoneId.of("UTC"));
		assertThat(site.getScope()).isEqualTo(WorksiteScope.GLOBAL);
		verify(this.worksiteRepository, never()).save(any());
	}

	@Test
	void deletionRejectsAssignedEmployees() {
		when(this.worksiteRepository.hasEmployees("WS-1")).thenReturn(true);
		assertThatThrownBy(() -> this.worksiteService.deleteWorksite("WS-1")).isInstanceOf(IllegalStateException.class);
		verify(this.worksiteRepository, never()).deleteByCode(any());
		verify(this.worksiteRepository, never()).findByCode(any());
	}

	@Test
	void deletionDelegatesToRepositoryWhenNoEmployeeIsAssigned() {
		when(this.worksiteRepository.deleteByCode("WS-1")).thenReturn(1L);
		this.worksiteService.deleteWorksite("WS-1");
		verify(this.worksiteRepository).deleteByCode("WS-1");
		verify(this.worksiteRepository, never()).findByCode(any());
	}

	@Test
	void missingWorksiteCannotBeDeleted() {
		assertThatThrownBy(() -> this.worksiteService.deleteWorksite("MISSING"))
				.isInstanceOf(ResourceNotFoundException.class);
		verify(this.worksiteRepository).deleteByCode("MISSING");
		verify(this.worksiteRepository, never()).findByCode(any());
	}

	@Test
	void repeatedAssignmentUpdatesBothSidesAndSavesOnlyOnce() {
		final Worksite site = worksite(WorksiteScope.ASSIGNED);
		final Employee employee = employee();
		when(this.worksiteRepository.findByCode("WS-1")).thenReturn(site);
		when(this.employeeService.findEmployeeByEmployeeNumber("EMP-1")).thenReturn(employee);
		assertThat(this.worksiteService.addEmployeeToWorksite("WS-1", "EMP-1")).isTrue();
		assertThat(this.worksiteService.addEmployeeToWorksite("WS-1", "EMP-1")).isFalse();
		assertThat(site.getEmployees()).containsExactly(employee);
		assertThat(employee.getWorksites()).containsExactly(site);
		verify(this.worksiteRepository).save(site);
	}

	@Test
	void repeatedRemovalUpdatesBothSidesAndSavesOnlyOnce() {
		final Worksite site = worksite(WorksiteScope.ASSIGNED);
		final Employee employee = employee();
		when(this.worksiteRepository.findByCode("WS-1")).thenReturn(site);
		when(this.employeeService.findEmployeeByEmployeeNumber("EMP-1")).thenReturn(employee);
		site.assignEmployee(employee);
		assertThat(this.worksiteService.removeEmployeeFromWorksite("WS-1", "EMP-1")).isTrue();
		assertThat(this.worksiteService.removeEmployeeFromWorksite("WS-1", "EMP-1")).isFalse();
		assertThat(site.getEmployees()).isEmpty();
		assertThat(employee.getWorksites()).isEmpty();
		verify(this.worksiteRepository).save(site);
	}

	@Test
	void assignmentFromEmployeeSideKeepsWorksiteInSyncAndIsIdempotent() {
		final Worksite site = worksite(WorksiteScope.ASSIGNED);
		final Employee employee = employee();
		assertThat(employee.assignWorksite(site)).isTrue();
		assertThat(employee.assignWorksite(site)).isFalse();
		assertThat(site.getEmployees()).containsExactly(employee);
		assertThat(employee.getWorksites()).containsExactly(site);
		assertThat(employee.removeWorksite(site)).isTrue();
		assertThat(employee.removeWorksite(site)).isFalse();
		assertThat(site.getEmployees()).isEmpty();
		assertThat(employee.getWorksites()).isEmpty();
	}
}
