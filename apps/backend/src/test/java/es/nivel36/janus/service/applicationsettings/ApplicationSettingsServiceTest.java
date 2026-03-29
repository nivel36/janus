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
package es.nivel36.janus.service.applicationsettings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class ApplicationSettingsServiceTest {

	private @Mock ApplicationSettingsRepository applicationSettingsRepository;
	private @InjectMocks ApplicationSettingsService applicationSettingsService;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
	}

	@Test
	void testGetDaysUntilLockedReturnsPersistedValue() {
		when(this.applicationSettingsRepository.findById(ApplicationSettings.GLOBAL_SETTINGS_ID))
				.thenReturn(Optional.of(new ApplicationSettings(7, false, false)));

		final int daysUntilLocked = this.applicationSettingsService.getDaysUntilLocked();

		assertEquals(7, daysUntilLocked);
		verify(this.applicationSettingsRepository).findById(ApplicationSettings.GLOBAL_SETTINGS_ID);
	}

	@Test
	void testGetDaysUntilLockedFallsBackToDefaultWhenSettingsAreMissing() {
		when(this.applicationSettingsRepository.findById(ApplicationSettings.GLOBAL_SETTINGS_ID))
				.thenReturn(Optional.empty());

		assertThrows(IllegalStateException.class, () -> this.applicationSettingsService.getDaysUntilLocked());
	}
}
