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
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
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
		final String email = "person@example.test";
		final ZoneId timezone = ZoneId.of("UTC");
		final Employee employee = mock(Employee.class);
		final AppUser winner = new AppUser(email, subject, Locale.ENGLISH, TimeFormat.H24, timezone);
		when(this.provisioningDefaults.locale()).thenReturn(Locale.ENGLISH);
		when(this.provisioningDefaults.getTimeFormat()).thenReturn(TimeFormat.H24);
		when(this.provisioningDefaults.defaultTimezone()).thenReturn(timezone);
		when(this.employeeService.existsEmployeeByEmail("person@example.test")).thenReturn(true);
		when(this.employeeService.findEmployeeByEmail("person@example.test")).thenReturn(employee);
		when(this.appUserRepository.findByEmployee(employee)).thenReturn(Optional.empty());
		when(this.appUserRepository.findByKeycloakSubject(subject)).thenReturn(Optional.empty())
				.thenReturn(Optional.empty()).thenReturn(Optional.of(winner));
		when(this.appUserRepository.existsByEmployee(employee)).thenReturn(true);
		when(this.appUserCreator.create(email, subject, Locale.ENGLISH, TimeFormat.H24, timezone, employee))
				.thenThrow(new AppUserCreationConflict(AppUserCreationConflict.Key.EMPLOYEE,
						new RuntimeException("employee claimed")));
		when(this.appUserCreator.create(eq(email), eq(subject), eq(Locale.ENGLISH), eq(TimeFormat.H24), eq(timezone),
				isNull()))
				.thenThrow(new AppUserCreationConflict(AppUserCreationConflict.Key.KEYCLOAK_SUBJECT,
						new RuntimeException("subject claimed")));

		assertSame(winner, this.appUserService.findOrCreateAppUser(subject, email));
	}

	@Test
	void findAppUsersByEmailReturnsEveryMatchingAccount() {
		final AppUser appUser = new AppUser("person@example.test", "11111111-1111-4111-8111-111111111111", Locale.ENGLISH,
				TimeFormat.H24, ZoneId.of("Europe/Madrid"));
		when(this.appUserRepository.findByEmail("person@example.test")).thenReturn(List.of(appUser));

		final List<AppUser> found = this.appUserService.findAppUsersByEmail("person@example.test");

		assertEquals(List.of(appUser), found);
		verify(this.appUserRepository).findByEmail("person@example.test");
	}

	@Test
	void findAppUserByIdThrowsWhenAccountDoesNotExist() {
		final UUID id = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");
		when(this.appUserRepository.findById(id)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> this.appUserService.findAppUserById(id));

		verify(this.appUserRepository).findById(id);
	}

	@Test
	void rejectsSubjectLongerThanDatabaseColumnBeforePersistence() {
		final String oversizedSubject = "x".repeat(256);

		assertThrows(IllegalArgumentException.class,
				() -> new AppUser("oversized-subject", oversizedSubject, Locale.ENGLISH, TimeFormat.H24));
	}

}
