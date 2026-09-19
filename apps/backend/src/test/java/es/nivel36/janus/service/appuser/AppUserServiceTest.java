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
package es.nivel36.janus.service.appuser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

import java.time.ZoneId;
import java.util.Locale;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import es.nivel36.janus.config.UserProvisioningProperties;
import es.nivel36.janus.service.ResourceNotFoundException;
import es.nivel36.janus.service.TimeFormat;
import es.nivel36.janus.service.employee.Employee;
import es.nivel36.janus.service.employee.EmployeeService;

class AppUserServiceTest {

	private @Mock AppUserRepository appUserRepository;
	private @Mock AppUserCreator appUserCreator;
	private @Mock UserProvisioningProperties provisioningDefaults;
	private @Mock EmployeeService employeeService;
	private @Mock PasswordEncoder passwordEncoder;
	private @InjectMocks AppUserService appUserService;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
	}

	@Test
	void testFindAppUserByKeycloakSubjectUsesSubjectClaim() {
		final String subject = "oidc-provider|tenant:customers|user:aferrer:opaque-identity";
		final AppUser appUser = new AppUser("aferrer", subject, Locale.ENGLISH, TimeFormat.H24,
				ZoneId.of("Europe/Madrid"));
		when(this.appUserRepository.findByKeycloakSubject(subject)).thenReturn(java.util.Optional.of(appUser));

		assertEquals(appUser, this.appUserService.findAppUserByKeycloakSubject(subject));
	}

	@Test
	void concurrentFallbackInsertReturnsTheProfileCreatedByTheWinningRequest() {
		final String subject = "11111111-1111-4111-8111-111111111111";
		final String username = "concurrent-user";
		final ZoneId timezone = ZoneId.of("UTC");
		final Employee employee = mock(Employee.class);
		final AppUser winner = new AppUser(username, subject, Locale.ENGLISH, TimeFormat.H24, timezone);
		when(this.provisioningDefaults.locale()).thenReturn(Locale.ENGLISH);
		when(this.provisioningDefaults.getTimeFormat()).thenReturn(TimeFormat.H24);
		when(this.provisioningDefaults.defaultTimezone()).thenReturn(timezone);
		when(this.employeeService.findEmployeeForProvisioning("person@example.test")).thenReturn(Optional.of(employee));
		when(this.appUserRepository.findByEmployee(employee)).thenReturn(Optional.empty());
		when(this.appUserRepository.findByKeycloakSubject(subject)).thenReturn(Optional.empty())
				.thenReturn(Optional.empty()).thenReturn(Optional.of(winner));
		when(this.appUserRepository.existsByEmployee(employee)).thenReturn(true);
		when(this.appUserCreator.create(username, subject, Locale.ENGLISH, TimeFormat.H24, timezone, employee))
				.thenThrow(new AppUserCreationConflict(AppUserCreationConflict.Key.EMPLOYEE,
						new RuntimeException("employee claimed")));
		when(this.appUserCreator.create(eq(username), eq(subject), eq(Locale.ENGLISH), eq(TimeFormat.H24), eq(timezone),
				isNull()))
				.thenThrow(new AppUserCreationConflict(AppUserCreationConflict.Key.KEYCLOAK_SUBJECT,
						new RuntimeException("subject claimed")));

		assertSame(winner, this.appUserService.findOrCreateAppUser(subject, username, "person@example.test"));
	}

	@Test
	void testFindAppUserByUsernameUsesAccountUsernameLookup() {
		final AppUser appUser = new AppUser("aferrer", "11111111-1111-4111-8111-111111111111", Locale.ENGLISH,
				TimeFormat.H24, ZoneId.of("Europe/Madrid"));
		when(this.appUserRepository.findByUsername("aferrer")).thenReturn(appUser);

		final AppUser foundAppUser = this.appUserService.findAppUserByUsername("aferrer");

		assertEquals(appUser, foundAppUser);
		verify(this.appUserRepository).findByUsername("aferrer");
	}

	@Test
	void testFindAppUserByUsernameThrowsWhenAccountUsernameDoesNotExist() {
		when(this.appUserRepository.findByUsername("missing-user")).thenReturn(null);

		assertThrows(ResourceNotFoundException.class, () -> this.appUserService.findAppUserByUsername("missing-user"));

		verify(this.appUserRepository).findByUsername("missing-user");
	}

	@Test
	void subjectReplacementUsesExplicitRecoveryUpdateAndReloadsUser() {
		final String replacement = "opaque-provider|replacement-identity-that-is-longer-than-a-uuid";
		final AppUser existing = new AppUser("recreated-user", "old-subject", Locale.ENGLISH, TimeFormat.H24);
		existing.setId(42L);
		final AppUser updated = new AppUser("recreated-user", replacement, Locale.ENGLISH, TimeFormat.H24);
		updated.setId(42L);
		when(this.appUserRepository.findByUsername("recreated-user")).thenReturn(existing);
		when(this.appUserRepository.findByKeycloakSubject(replacement)).thenReturn(Optional.empty());
		when(this.appUserRepository.replaceKeycloakSubject(42L, replacement)).thenReturn(1);
		when(this.appUserRepository.findById(42L)).thenReturn(Optional.of(updated));

		assertSame(updated, this.appUserService.replaceKeycloakSubject("recreated-user", replacement));
		verify(this.appUserRepository).replaceKeycloakSubject(42L, replacement);
	}

	@Test
	void rejectsSubjectLongerThanDatabaseColumnBeforePersistence() {
		final String oversizedSubject = "x".repeat(256);

		assertThrows(IllegalArgumentException.class,
				() -> new AppUser("oversized-subject", oversizedSubject, Locale.ENGLISH, TimeFormat.H24));
	}

	@Test
	void concurrentSubjectReplacementTranslatesUniqueConstraintFailure() {
		final String replacement = "22222222-2222-4222-8222-222222222222";
		final AppUser appUser = new AppUser("first-admin-target", "11111111-1111-4111-8111-111111111111",
				Locale.ENGLISH, TimeFormat.H24, ZoneId.of("UTC"));
		final DataIntegrityViolationException databaseConflict = new DataIntegrityViolationException(
				"UK_APP_USER_KEYCLOAK_SUBJECT");
		when(this.appUserRepository.findByUsername("first-admin-target")).thenReturn(appUser);
		when(this.appUserRepository.findByKeycloakSubject(replacement)).thenReturn(Optional.empty());
		when(this.appUserRepository.replaceKeycloakSubject(appUser.getId(), replacement)).thenThrow(databaseConflict);

		final KeycloakSubjectConflictException conflict = assertThrows(KeycloakSubjectConflictException.class,
				() -> this.appUserService.replaceKeycloakSubject("first-admin-target", replacement));

		assertSame(databaseConflict, conflict.getCause());
		verify(this.appUserRepository).replaceKeycloakSubject(appUser.getId(), replacement);
	}
}
