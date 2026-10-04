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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.ZoneId;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import es.nivel36.janus.config.UserProvisioningProperties;
import es.nivel36.janus.service.ResourceNotFoundException;
import es.nivel36.janus.service.TimeFormat;
import es.nivel36.janus.service.employee.Employee;
import es.nivel36.janus.service.employee.EmployeeService;

/**
 * Verifies validated profile lookups, email refresh and first-access conflict
 * reconciliation.
 */
@ExtendWith(MockitoExtension.class)
class AppUserServiceTest {

	private @Mock AppUserRepository appUserRepository;
	private @Mock AppUserCreator appUserCreator;
	private @Mock UserProvisioningProperties provisioningDefaults;
	private @Mock EmployeeService employeeService;
	private AppUserService appUserService;

	@BeforeEach
	void setUp() {
		this.appUserService = new AppUserService(
				this.appUserRepository,
				this.appUserCreator,
				this.provisioningDefaults,
				this.employeeService,
				100);
	}

	@Test
	void testFindAppUserByKeycloakSubjectUsesSubjectClaim() {
		final String subject = "oidc-provider|tenant:customers|user:aferrer:opaque-identity";
		final AppUser appUser = new AppUser(
				"aferrer@example.test",
				subject,
				Locale.ENGLISH,
				TimeFormat.H24,
				ZoneId.of("Europe/Madrid"));
		when(this.appUserRepository.findByKeycloakSubject(subject)).thenReturn(java.util.Optional.of(appUser));

		assertEquals(appUser, this.appUserService.findAppUserByKeycloakSubject(subject));
	}

	@Test
	void concurrentFallbackInsertReturnsTheProfileCreatedByTheWinningRequest() {
		final String subject = "11111111-1111-4111-8111-111111111111";
		final String email = "person@example.test";
		final ZoneId timezone = ZoneId.of("UTC");
		final Employee employee = mock(Employee.class);
		when(employee.getId()).thenReturn(42L);
		final AppUser winner = new AppUser(email, subject, Locale.ENGLISH, TimeFormat.H24, timezone);
		when(this.provisioningDefaults.locale()).thenReturn(Locale.ENGLISH);
		when(this.provisioningDefaults.getTimeFormat()).thenReturn(TimeFormat.H24);
		when(this.provisioningDefaults.defaultTimezone()).thenReturn(timezone);
		when(this.employeeService.findEmployeeByEmployeeNumber("EMP-42")).thenReturn(employee);
		when(this.appUserRepository.findByEmployee(employee)).thenReturn(Optional.empty());
		when(this.appUserRepository.findByKeycloakSubject(subject)).thenReturn(Optional.empty())
				.thenReturn(Optional.empty()).thenReturn(Optional.of(winner));
		when(this.appUserCreator.create(email, subject, Locale.ENGLISH, TimeFormat.H24, timezone, 42L))
				.thenThrow(new AppUserCreationConflict(new RuntimeException("employee claimed")));
		when(
				this.appUserCreator
						.create(eq(email), eq(subject), eq(Locale.ENGLISH), eq(TimeFormat.H24), eq(timezone), isNull()))
				.thenThrow(new AppUserCreationConflict(new RuntimeException("subject claimed")));

		assertSame(winner, this.appUserService.findOrCreateAppUser(subject, email, "EMP-42"));
	}

	@Test
	void validEmailWithoutEmployeeCreatesAnUnlinkedProfile() {
		final String subject = "22222222-2222-4222-8222-222222222222";
		final String email = "unlinked@example.test";
		final ZoneId timezone = ZoneId.of("UTC");
		final AppUser created = new AppUser(email, subject, Locale.ENGLISH, TimeFormat.H24, timezone);
		when(this.provisioningDefaults.locale()).thenReturn(Locale.ENGLISH);
		when(this.provisioningDefaults.getTimeFormat()).thenReturn(TimeFormat.H24);
		when(this.provisioningDefaults.defaultTimezone()).thenReturn(timezone);
		when(this.appUserRepository.findByKeycloakSubject(subject)).thenReturn(Optional.empty());
		when(this.appUserCreator.create(email, subject, Locale.ENGLISH, TimeFormat.H24, timezone, null))
				.thenReturn(created);

		assertSame(created, this.appUserService.findOrCreateAppUser(subject, email, null));
		verify(this.appUserCreator).create(email, subject, Locale.ENGLISH, TimeFormat.H24, timezone, null);
	}

	@Test
	void findAppUserByIdThrowsWhenAccountDoesNotExist() {
		final UUID id = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");
		when(this.appUserRepository.findById(id)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> this.appUserService.findAppUserById(id));

		verify(this.appUserRepository).findById(id);
	}

	@Test
	void unknownEmployeeNumberCreatesAnUnlinkedProfile() {
		final String subject = "33333333-3333-4333-8333-333333333333";
		final String email = "unknown@example.test";
		final ZoneId timezone = ZoneId.of("UTC");
		final AppUser created = new AppUser(email, subject, Locale.ENGLISH, TimeFormat.H24, timezone);
		when(this.provisioningDefaults.locale()).thenReturn(Locale.ENGLISH);
		when(this.provisioningDefaults.getTimeFormat()).thenReturn(TimeFormat.H24);
		when(this.provisioningDefaults.defaultTimezone()).thenReturn(timezone);
		when(this.appUserRepository.findByKeycloakSubject(subject)).thenReturn(Optional.empty());
		when(this.employeeService.findEmployeeByEmployeeNumber("UNKNOWN"))
				.thenThrow(new ResourceNotFoundException("missing"));
		when(this.appUserCreator.create(email, subject, Locale.ENGLISH, TimeFormat.H24, timezone, null))
				.thenReturn(created);

		assertSame(created, this.appUserService.findOrCreateAppUser(subject, email, "UNKNOWN"));
	}

	@Test
	void existingProfileUpdatesItsContactEmailWithoutRelinking() {
		final String subject = "44444444-4444-4444-8444-444444444444";
		final AppUser existing = new AppUser("old@example.test", subject, Locale.ENGLISH, TimeFormat.H24);
		when(this.appUserRepository.findByKeycloakSubject(subject)).thenReturn(Optional.of(existing));

		assertSame(existing, this.appUserService.findOrCreateAppUser(subject, "  NEW@EXAMPLE.TEST  ", "  EMP-99  "));
		assertEquals("new@example.test", existing.getEmail());
		org.mockito.Mockito.verifyNoInteractions(this.employeeService, this.appUserCreator);
	}

	@Test
	void subjectLookupReportsMissingProfileWithoutDependingOnSecurity() {
		assertThrows(
				ResourceNotFoundException.class,
				() -> this.appUserService.findAppUserByKeycloakSubject("missing"));
	}

	@Test
	void invalidClaimsAreRejectedBeforeLookingUpOrCreatingAProfile() {
		assertThrows(
				IllegalArgumentException.class,
				() -> this.appUserService.findOrCreateAppUser("x".repeat(256), "valid@example.test", null));
		assertThrows(
				IllegalArgumentException.class,
				() -> this.appUserService.findOrCreateAppUser("subject", "x".repeat(256), null));
		assertThrows(
				IllegalArgumentException.class,
				() -> this.appUserService.findOrCreateAppUser("subject", "valid@example.test", "bad number"));
		org.mockito.Mockito.verifyNoInteractions(this.appUserRepository, this.appUserCreator, this.employeeService);
	}

}
