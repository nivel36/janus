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

import java.time.ZoneId;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import es.nivel36.janus.service.TimeFormat;

/**
 * Verifies profile input invariants and atomic preference mutation without
 * persistence.
 */
class AppUserTest {
	@Test
	void invalidPreferenceChangeLeavesAllPreferencesUntouched() {
		final AppUser user = new AppUser("user@example.test", "subject", Locale.ENGLISH, TimeFormat.H24);
		assertThatThrownBy(() -> user.updatePreferences(Locale.FRENCH, TimeFormat.H12, ZoneId.of("Europe/Paris"), null))
				.isInstanceOf(NullPointerException.class);
		assertThat(user.getLocale()).isEqualTo(Locale.ENGLISH);
		assertThat(user.getTimeFormat()).isEqualTo(TimeFormat.H24);
		assertThat(user.getDefaultTimezone()).isEqualTo(ZoneId.of("UTC"));
		assertThat(user.getTheme()).isEqualTo(Theme.DARK);
	}

	@Test
	void columnLimitsAreEnforcedBeforePersistence() {
		assertThatThrownBy(() -> new AppUser("valid@example.test", "x".repeat(256), Locale.ENGLISH, TimeFormat.H24))
				.isInstanceOf(IllegalArgumentException.class);
		final AppUser user = new AppUser("  USER@EXAMPLE.TEST  ", "x".repeat(255), Locale.ENGLISH, TimeFormat.H24);
		assertThat(user.getEmail()).isEqualTo("user@example.test");
		assertThatThrownBy(() -> user.setEmail("x".repeat(256))).isInstanceOf(IllegalArgumentException.class);
		assertThat(user.getEmail()).isEqualTo("user@example.test");
	}

	@Test
	void acceptsIdentityProviderEmailWithValidLocalPartPunctuation() {
		final AppUser user = new AppUser("flow%_!literal@example.test", "subject", Locale.ENGLISH, TimeFormat.H24);

		assertThat(user.getEmail()).isEqualTo("flow%_!literal@example.test");
	}
}
