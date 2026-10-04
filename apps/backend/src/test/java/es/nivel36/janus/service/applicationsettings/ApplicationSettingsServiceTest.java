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
package es.nivel36.janus.service.applicationsettings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApplicationSettingsServiceTest {

	@Mock
	private ApplicationSettingsRepository repository;

	@InjectMocks
	private ApplicationSettingsService service;

	@Test
	void findsGlobalApplicationSettings() {
		final ApplicationSettings settings = new ApplicationSettings(7, true, false, false, ZoneId.of("Europe/Madrid"));

		when(this.repository.findById(ApplicationSettings.GLOBAL_SETTINGS_ID)).thenReturn(Optional.of(settings));

		assertThat(this.service.findApplicationSettings()).isSameAs(settings);

		verify(this.repository).findById(ApplicationSettings.GLOBAL_SETTINGS_ID);
	}

	@Test
	void updateModifiesExistingApplicationSettings() {
		final ApplicationSettings settings = new ApplicationSettings(7, true, false, false, ZoneId.of("Europe/Madrid"));

		when(this.repository.findById(ApplicationSettings.GLOBAL_SETTINGS_ID)).thenReturn(Optional.of(settings));

		final ApplicationSettings result = this.service.update(30, false, true, true, ZoneId.of("UTC"));

		assertThat(result).isSameAs(settings);
		assertThat(settings.getDaysUntilLocked()).isEqualTo(30);
		assertThat(settings.isEmployeeWorksiteCreationAllowed()).isFalse();
		assertThat(settings.isWorksiteChangeDuringShiftAllowed()).isTrue();
		assertThat(settings.isEmployeeManualTimeLogEntryAllowed()).isTrue();
		assertThat(settings.getDefaultTimezone()).isEqualTo(ZoneId.of("UTC"));
	}

	@Test
	void findFailsWhenGlobalSettingsAreMissing() {
		when(this.repository.findById(ApplicationSettings.GLOBAL_SETTINGS_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> this.service.findApplicationSettings())
				.isInstanceOf(MissingApplicationSettingsException.class)
				.hasMessage("Global application settings row is missing");
	}

	@Test
	void updateFailsWhenGlobalSettingsAreMissing() {
		when(this.repository.findById(ApplicationSettings.GLOBAL_SETTINGS_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> this.service.update(3, false, true, true, ZoneId.of("UTC")))
				.isInstanceOf(MissingApplicationSettingsException.class);
	}
}
