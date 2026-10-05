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
package es.nivel36.janus.service.appuser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import es.nivel36.janus.config.UserProvisioningProperties;
import es.nivel36.janus.service.ResourceNotFoundException;
import es.nivel36.janus.service.TimeFormat;
import es.nivel36.janus.service.employee.Employee;
import es.nivel36.janus.service.employee.EmployeeService;
import es.nivel36.janus.service.schedule.Schedule;

/**
 * Verifies profile orchestration and search requests independently of Spring
 * validation.
 */
@ExtendWith(MockitoExtension.class)
class AppUserServiceTest {
	private static final String SUBJECT = "user-42";
	private static final String EMAIL = "person@example.test";
	private static final ZoneId TIMEZONE = ZoneId.of("Europe/Madrid");
	private static final UUID ID = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");
	private @Mock AppUserRepository repository;
	private @Mock AppUserCreator creator;
	private @Mock EmployeeService employees;
	private AppUserService service;

	@BeforeEach
	void setUp() {
		final UserProvisioningProperties defaults = new UserProvisioningProperties();
		defaults.setLocale("fr-FR");
		defaults.setTimeFormat(TimeFormat.H12);
		defaults.setDefaultTimezone(TIMEZONE.getId());
		this.service = new AppUserService(this.repository, this.creator, defaults, this.employees, 100);
	}

	private AppUser profile() {
		return new AppUser(EMAIL, SUBJECT, Locale.ENGLISH, TimeFormat.H24, ZoneId.of("UTC"));
	}

	private Employee employee() {
		final Employee employee = new Employee(
				"EMP-42",
				"Jane",
				"Doe",
				"employee@example.test",
				new Schedule("STD", "Standard", Duration.ZERO, Duration.ZERO));
		ReflectionTestUtils.setField(employee, "id", 42L);
		return employee;
	}

	private void availableEmployee(final Employee employee) {
		when(this.employees.findEmployeeByEmployeeNumber("EMP-42")).thenReturn(employee);
		when(this.repository.findByEmployee(employee)).thenReturn(Optional.empty());
	}

	private AppUser createdWith(final Long employeeId) {
		final AppUser created = profile();
		when(this.creator.create(EMAIL, SUBJECT, Locale.FRANCE, TimeFormat.H12, TIMEZONE, employeeId))
				.thenReturn(created);
		return created;
	}

	private AppUserCreationConflict conflict(final Long employeeId) {
		final AppUserCreationConflict conflict = new AppUserCreationConflict(new RuntimeException("insert conflict"));
		when(this.creator.create(EMAIL, SUBJECT, Locale.FRANCE, TimeFormat.H12, TIMEZONE, employeeId))
				.thenThrow(conflict);
		return conflict;
	}

	@Test
	void existingProfilePreservesEmailPreferencesAndEmployee() {
		final AppUser existing = profile();
		final Employee employee = employee();
		existing.setEmployee(employee);
		when(this.repository.findByKeycloakSubject(SUBJECT)).thenReturn(Optional.of(existing));

		assertThat(this.service.findOrCreateAppUser(SUBJECT, "changed@example.test", "OTHER")).isSameAs(existing);
		assertThat(existing.getEmail()).isEqualTo(EMAIL);
		assertThat(existing.getLocale()).isEqualTo(Locale.ENGLISH);
		assertThat(existing.getTimeFormat()).isEqualTo(TimeFormat.H24);
		assertThat(existing.getDefaultTimezone()).isEqualTo(ZoneId.of("UTC"));
		assertThat(existing.getTheme()).isEqualTo(Theme.DARK);
		assertThat(existing.getEmployee()).isSameAs(employee);
		assertThat(employee.getAppUser()).isSameAs(existing);
		verifyNoInteractions(this.employees, this.creator);
	}

	@Test
	void missingEmployeeClaimCreatesUnlinkedProfileWithConfiguredDefaults() {
		final AppUser created = createdWith(null);
		assertThat(this.service.findOrCreateAppUser(SUBJECT, EMAIL, null)).isSameAs(created);
		verifyNoInteractions(this.employees);
		verify(this.repository, never()).findByEmployee(any());
	}

	@Test
	void knownUnclaimedEmployeeIsPassedToCreatorWithoutMutatingItsAssociation() {
		final Employee employee = employee();
		availableEmployee(employee);
		final AppUser created = createdWith(42L);
		assertThat(this.service.findOrCreateAppUser(SUBJECT, EMAIL, "EMP-42")).isSameAs(created);
		assertThat(employee.getAppUser()).isNull();
	}

	@Test
	void unknownEmployeeCreatesUnlinkedProfile() {
		when(this.employees.findEmployeeByEmployeeNumber("UNKNOWN"))
				.thenThrow(new ResourceNotFoundException("missing"));
		final AppUser created = createdWith(null);
		assertThat(this.service.findOrCreateAppUser(SUBJECT, EMAIL, "UNKNOWN")).isSameAs(created);
		verify(this.repository, never()).findByEmployee(any());
	}

	@Test
	void claimedEmployeeKeepsItsOwnerAndNewProfileIsUnlinked() {
		final Employee employee = employee();
		final AppUser owner = profile();
		owner.setEmployee(employee);
		when(this.employees.findEmployeeByEmployeeNumber("EMP-42")).thenReturn(employee);
		when(this.repository.findByEmployee(employee)).thenReturn(Optional.of(owner));
		final AppUser created = createdWith(null);

		assertThat(this.service.findOrCreateAppUser(SUBJECT, EMAIL, "EMP-42")).isSameAs(created);
		assertThat(employee.getAppUser()).isSameAs(owner);
		assertThat(owner.getEmployee()).isSameAs(employee);
	}

	@Test
	void employeeLookupFailureIsNotSilentlyTreatedAsMissing() {
		final IllegalStateException failure = new IllegalStateException("employee service unavailable");
		when(this.employees.findEmployeeByEmployeeNumber("EMP-42")).thenThrow(failure);
		assertThatThrownBy(() -> this.service.findOrCreateAppUser(SUBJECT, EMAIL, "EMP-42")).isSameAs(failure);
		verifyNoInteractions(this.creator);
	}

	@Test
	void subjectConflictReturnsWinnerWithoutChangingItsEmailOrRetrying() {
		final Employee employee = employee();
		availableEmployee(employee);
		conflict(42L);
		final AppUser winner = new AppUser("winner@example.test", SUBJECT, Locale.ENGLISH, TimeFormat.H24);
		when(this.repository.findByKeycloakSubject(SUBJECT)).thenReturn(Optional.empty())
				.thenReturn(Optional.of(winner));

		assertThat(this.service.findOrCreateAppUser(SUBJECT, EMAIL, "EMP-42")).isSameAs(winner);
		assertThat(winner.getEmail()).isEqualTo("winner@example.test");
		assertThat(employee.getAppUser()).isNull();
		verify(this.creator, never()).create(any(), any(), any(), any(), any(), isNull());
	}

	@Test
	void employeeConflictReturnsSuccessfulUnlinkedRetry() {
		final Employee employee = employee();
		availableEmployee(employee);
		conflict(42L);
		final AppUser created = createdWith(null);

		assertThat(this.service.findOrCreateAppUser(SUBJECT, EMAIL, "EMP-42")).isSameAs(created);
		assertThat(employee.getAppUser()).isNull();
		verify(this.creator).create(EMAIL, SUBJECT, Locale.FRANCE, TimeFormat.H12, TIMEZONE, null);
	}

	@Test
	void subjectConflictDuringUnlinkedRetryReturnsWinner() {
		availableEmployee(employee());
		conflict(42L);
		conflict(null);
		final AppUser winner = profile();
		when(this.repository.findByKeycloakSubject(SUBJECT)).thenReturn(Optional.empty()).thenReturn(Optional.empty())
				.thenReturn(Optional.of(winner));

		assertThat(this.service.findOrCreateAppUser(SUBJECT, EMAIL, "EMP-42")).isSameAs(winner);
	}

	@Test
	void unreconciledUnlinkedConflictPropagatesWithoutAnotherInsert() {
		final AppUserCreationConflict failure = conflict(null);
		assertThatThrownBy(() -> this.service.findOrCreateAppUser(SUBJECT, EMAIL, null)).isSameAs(failure);
		verify(this.creator).create(EMAIL, SUBJECT, Locale.FRANCE, TimeFormat.H12, TIMEZONE, null);
		verifyNoMoreInteractions(this.creator);
	}

	@Test
	void unreconciledRetryPropagatesRetryConflict() {
		availableEmployee(employee());
		conflict(42L);
		final AppUserCreationConflict failure = conflict(null);
		assertThatThrownBy(() -> this.service.findOrCreateAppUser(SUBJECT, EMAIL, "EMP-42")).isSameAs(failure);
		verify(this.creator).create(EMAIL, SUBJECT, Locale.FRANCE, TimeFormat.H12, TIMEZONE, 42L);
		verify(this.creator).create(EMAIL, SUBJECT, Locale.FRANCE, TimeFormat.H12, TIMEZONE, null);
		verifyNoMoreInteractions(this.creator);
	}

	@Test
	void subjectLookupReturnsExistingProfileWithoutProvisioning() {
		final AppUser existing = profile();
		when(this.repository.findByKeycloakSubject(SUBJECT)).thenReturn(Optional.of(existing));
		assertThat(this.service.findAppUserByKeycloakSubject(SUBJECT)).isSameAs(existing);
		verifyNoInteractions(this.creator, this.employees);
	}

	@Test
	void missingSubjectReportsNotFoundWithoutProvisioning() {
		assertThatThrownBy(() -> this.service.findAppUserByKeycloakSubject(SUBJECT))
				.isInstanceOf(ResourceNotFoundException.class);
		verifyNoInteractions(this.creator, this.employees);
	}

	@Test
	void preferenceUpdateChangesAllPreferencesAndPreservesIdentityAndAssociation() {
		final AppUser existing = profile();
		final Employee employee = employee();
		existing.setEmployee(employee);
		when(this.repository.findById(ID)).thenReturn(Optional.of(existing));

		assertThat(this.service.updatePreferences(ID, Locale.FRANCE, TimeFormat.H12, TIMEZONE, Theme.LIGHT))
				.isSameAs(existing);
		assertThat(existing.getLocale()).isEqualTo(Locale.FRANCE);
		assertThat(existing.getTimeFormat()).isEqualTo(TimeFormat.H12);
		assertThat(existing.getDefaultTimezone()).isEqualTo(TIMEZONE);
		assertThat(existing.getTheme()).isEqualTo(Theme.LIGHT);
		assertThat(existing.getKeycloakSubject()).isEqualTo(SUBJECT);
		assertThat(existing.getEmail()).isEqualTo(EMAIL);
		assertThat(existing.getEmployee()).isSameAs(employee);
		assertThat(employee.getAppUser()).isSameAs(existing);
	}

	@Test
	void deletionClearsBothSidesOfEmployeeLinkBeforeDeletingProfile() {
		final AppUser existing = profile();
		final Employee employee = employee();
		existing.setEmployee(employee);
		when(this.repository.findById(ID)).thenReturn(Optional.of(existing));
		doAnswer(invocation -> {
			assertThat(invocation.getArgument(0, AppUser.class)).isSameAs(existing);
			assertThat(existing.getEmployee()).isNull();
			assertThat(employee.getAppUser()).isNull();
			return null;
		}).when(this.repository).delete(existing);

		this.service.deleteAppUser(ID);
		verify(this.repository).delete(existing);
		verifyNoInteractions(this.employees);
	}

	@Test
	void missingProfileCannotBeUpdatedOrDeleted() {
		assertThatThrownBy(
				() -> this.service.updatePreferences(ID, Locale.FRANCE, TimeFormat.H12, TIMEZONE, Theme.LIGHT))
				.isInstanceOf(ResourceNotFoundException.class);
		assertThatThrownBy(() -> this.service.deleteAppUser(ID)).isInstanceOf(ResourceNotFoundException.class);
		verify(this.repository, never()).delete(any());
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = { "" })
	void absentEmailFilterUsesDefaultSortAndPreservesPage(final String filter) {
		final Page<AppUser> page = new PageImpl<>(java.util.List.of(profile()));
		when(this.repository.search("", null, PageRequest.of(3, 20, Sort.by("email", "id")))).thenReturn(page);
		assertThat(this.service.searchAppUsers(filter, null, PageRequest.of(3, 20))).isSameAs(page);
	}

	@Test
	void searchEscapesLiteralWildcardsWithoutTrimmingAndCapsPageSize() {
		this.service.searchAppUsers("  A%_!  ", "EMP-42", PageRequest.of(2, 500));
		verify(this.repository).search("  A!%!_!!  ", "EMP-42", PageRequest.of(2, 100, Sort.by("email", "id")));
	}

	@Test
	void publicEmployeeSortIsTranslatedAndOrderOptionsArePreserved() {
		final Sort.Order order = Sort.Order.desc("employeeNumber").nullsLast().ignoreCase();
		this.service.searchAppUsers(null, null, PageRequest.of(0, 10, Sort.by(order, Sort.Order.desc("email"))));
		final ArgumentCaptor<Pageable> request = ArgumentCaptor.forClass(Pageable.class);
		verify(this.repository).search(eq(""), isNull(), request.capture());
		assertThat(request.getValue().getSort()).containsExactly(
				order.withProperty("employee.employeeNumber"),
				Sort.Order.desc("email"),
				Sort.Order.asc("id"));
	}

	@Test
	void explicitUuidSortKeepsDirectionAndPositionWithoutDuplicateTieBreaker() {
		final Pageable request = PageRequest.of(0, 10, Sort.by(Sort.Order.desc("id"), Sort.Order.asc("email")));
		this.service.searchAppUsers(null, null, request);
		verify(this.repository).search("", null, request);
	}

	@ParameterizedTest
	@ValueSource(strings = { "keycloakSubject", "employee.employeeNumber", "theme" })
	void internalSortFieldsAreRejectedBeforeSearching(final String property) {
		assertThatThrownBy(() -> this.service.searchAppUsers(null, null, PageRequest.of(0, 10, Sort.by(property))))
				.isInstanceOf(IllegalArgumentException.class);
		verifyNoInteractions(this.repository);
	}

	@Test
	void unpagedSearchIsRejectedBeforeSearching() {
		assertThatThrownBy(() -> this.service.searchAppUsers(null, null, Pageable.unpaged()))
				.isInstanceOf(IllegalArgumentException.class);
		verifyNoInteractions(this.repository);
	}
}
