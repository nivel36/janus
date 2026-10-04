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

import java.time.ZoneId;

import org.junit.jupiter.api.Test;

class ApplicationSettingsTest {

	@Test
	void constructorRejectsNegativeDaysUntilLocked() {
		assertThatThrownBy(() -> new ApplicationSettings(-1, true, false, true, ZoneId.of("UTC")))
				.isInstanceOf(IllegalArgumentException.class).hasMessage("daysUntilLocked cannot be negative");
	}

	@Test
	void constructorRejectsNullTimezone() {
		assertThatThrownBy(() -> new ApplicationSettings(7, true, false, true, null))
				.isInstanceOf(NullPointerException.class).hasMessage("defaultTimezone cannot be null");
	}

	@Test
	void updateChangesAllSettings() {
		final ApplicationSettings settings = new ApplicationSettings(7, true, false, false, ZoneId.of("Europe/Madrid"));

		settings.update(30, false, true, true, ZoneId.of("UTC"));

		assertThat(settings.getDaysUntilLocked()).isEqualTo(30);
		assertThat(settings.isEmployeeWorksiteCreationAllowed()).isFalse();
		assertThat(settings.isWorksiteChangeDuringShiftAllowed()).isTrue();
		assertThat(settings.isEmployeeManualTimeLogEntryAllowed()).isTrue();
		assertThat(settings.getDefaultTimezone()).isEqualTo(ZoneId.of("UTC"));
	}

	@Test
	void invalidUpdateDoesNotModifySettings() {
		final ApplicationSettings settings = new ApplicationSettings(7, true, false, false, ZoneId.of("Europe/Madrid"));

		assertThatThrownBy(() -> settings.update(3, false, true, true, null)).isInstanceOf(NullPointerException.class);

		assertThat(settings.getDaysUntilLocked()).isEqualTo(7);
		assertThat(settings.isEmployeeWorksiteCreationAllowed()).isTrue();
		assertThat(settings.isWorksiteChangeDuringShiftAllowed()).isFalse();
		assertThat(settings.isEmployeeManualTimeLogEntryAllowed()).isFalse();
		assertThat(settings.getDefaultTimezone()).isEqualTo(ZoneId.of("Europe/Madrid"));
	}
}
