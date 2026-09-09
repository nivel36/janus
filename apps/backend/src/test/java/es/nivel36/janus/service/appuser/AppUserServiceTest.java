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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.ZoneId;
import java.time.zone.ZoneRulesException;
import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import es.nivel36.janus.service.ResourceAlreadyExistsException;
import es.nivel36.janus.config.UserProvisioningProperties;
import es.nivel36.janus.service.ResourceNotFoundException;
import es.nivel36.janus.service.TimeFormat;
import es.nivel36.janus.service.employee.Employee;

class AppUserServiceTest {

	private @Mock AppUserRepository appUserRepository;
	private @Mock AppUserCreator appUserCreator;
	private @Mock UserProvisioningProperties provisioningDefaults;
	private @Mock PasswordEncoder passwordEncoder;
	private @InjectMocks AppUserService appUserService;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
	}

	@Test
	void testCreateAppUserUsesProvidedTimezoneBeforeSave() {
		when(this.appUserRepository.existsByUsername("aferrer")).thenReturn(false);
		when(this.passwordEncoder.encode("raw-password")).thenReturn("hashed-password");
		when(this.appUserRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

		this.appUserService.createAppUser("aferrer", "11111111-1111-4111-8111-111111111111", Locale.ENGLISH,
				TimeFormat.H12, ZoneId.of("Europe/Madrid"));

		verify(this.appUserRepository).existsByUsername("aferrer");
		final ArgumentCaptor<AppUser> savedAppUserCaptor = ArgumentCaptor.forClass(AppUser.class);
		verify(this.appUserRepository).save(savedAppUserCaptor.capture());
		assertEquals("aferrer", savedAppUserCaptor.getValue().getUsername());
		assertEquals("Europe/Madrid", savedAppUserCaptor.getValue().getDefaultTimezone().getId());
	}

	@Test
	void testCreateAppUserThrowsWhenTimezoneIsInvalid() {
		assertThrows(ZoneRulesException.class,
				() -> this.appUserService.createAppUser("aferrer", "11111111-1111-4111-8111-111111111111",
						Locale.ENGLISH, TimeFormat.H24, ZoneId.of("Mars/Olympus")));
	}

	@Test
	void testCreateAppUserThrowsWhenUsernameAlreadyExistsByAccountUsername() {
		when(this.appUserRepository.existsByUsername("aferrer")).thenReturn(true);

		final ZoneId zoneId = ZoneId.of("Europe/Madrid");
		
		assertThrows(ResourceAlreadyExistsException.class, () -> {
			this.appUserService.createAppUser("aferrer", "11111111-1111-4111-8111-111111111111", Locale.ENGLISH,
					TimeFormat.H24, zoneId);
		});

		verify(this.appUserRepository).existsByUsername("aferrer");
	}

	@Test
	void testCreateAppUserRejectsAnAlreadyLinkedIdentity() {
		when(this.appUserRepository.existsByKeycloakSubject("11111111-1111-4111-8111-111111111111")).thenReturn(true);

		assertThrows(ResourceAlreadyExistsException.class,
				() -> this.appUserService.createAppUser("aferrer", "11111111-1111-4111-8111-111111111111",
						Locale.ENGLISH, TimeFormat.H24,
						ZoneId.of("Europe/Madrid")));
	}

	@Test
	void testFindAppUserByKeycloakSubjectUsesSubjectClaim() {
		final AppUser appUser = new AppUser("aferrer", "11111111-1111-4111-8111-111111111111", Locale.ENGLISH,
				TimeFormat.H24, ZoneId.of("Europe/Madrid"));
		when(this.appUserRepository.findByKeycloakSubject("11111111-1111-4111-8111-111111111111"))
			.thenReturn(java.util.Optional.of(appUser));

		assertEquals(appUser, this.appUserService.findAppUserByKeycloakSubject("11111111-1111-4111-8111-111111111111"));
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
	void testUpdateAppUserUpdatesTimezone() {
		final AppUser appUser = new AppUser("aferrer", "11111111-1111-4111-8111-111111111111", Locale.ENGLISH,
				TimeFormat.H24, ZoneId.of("Europe/Madrid"));
		when(this.appUserRepository.findByUsername("aferrer")).thenReturn(appUser);

		final AppUser updatedAppUser = this.appUserService.updateAppUser("aferrer", Locale.CANADA, TimeFormat.H12,
				ZoneId.of("America/Toronto"));

		assertEquals(Locale.CANADA, updatedAppUser.getLocale());
		assertEquals(TimeFormat.H12, updatedAppUser.getTimeFormat());
		assertEquals("America/Toronto", updatedAppUser.getDefaultTimezone().getId());
	}

	@Test
	void testUpdateAppUserKeepsSameEmployeeWhenLoadedAsDifferentEntityInstance() {
		final Employee currentlyLinkedEmployee = org.mockito.Mockito.mock(Employee.class);
		final Employee requestedEmployee = org.mockito.Mockito.mock(Employee.class);
		when(currentlyLinkedEmployee.getId()).thenReturn(42L);
		when(requestedEmployee.getId()).thenReturn(42L);
		final AppUser appUser = new AppUser("aferrer", "11111111-1111-4111-8111-111111111111", Locale.ENGLISH,
				TimeFormat.H24, ZoneId.of("Europe/Madrid"));
		appUser.setEmployee(currentlyLinkedEmployee);
		when(this.appUserRepository.findByUsername("aferrer")).thenReturn(appUser);

		final AppUser updated = this.appUserService.updateAppUser("aferrer", Locale.CANADA, TimeFormat.H12,
				ZoneId.of("America/Toronto"), requestedEmployee, true);

		assertSame(currentlyLinkedEmployee, updated.getEmployee());
		verify(this.appUserRepository, never()).existsByEmployee(any(Employee.class));
	}
}
