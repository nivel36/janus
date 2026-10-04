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
package es.nivel36.janus.policy.applicationsettings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.EnumSet;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import es.nivel36.janus.security.Actor;
import es.nivel36.janus.security.ActorResolver;
import es.nivel36.janus.service.appuser.Role;

class ApplicationSettingsAuthorizationAdapterTest {
	@ParameterizedTest
	@ValueSource(ints = { 0, 1, 2, 3, 4, 5, 6, 7 })
	void roleCombinationsAreResolvedWithoutRequiringAnEmployee(final int mask) {
		final EnumSet<Role> roles = EnumSet.noneOf(Role.class);
		for (final Role role : Role.values()) {
			if ((mask & (1 << role.ordinal())) != 0) {
				roles.add(role);
			}
		}
		final Actor actor = new Actor(UUID.randomUUID(), roles, null);
		final Authentication authentication = mock(Authentication.class);
		final ActorResolver resolver = mock(ActorResolver.class);
		when(resolver.resolve(authentication)).thenReturn(actor);
		final ApplicationSettingsAuthorizationAdapter adapter = new ApplicationSettingsAuthorizationAdapter(resolver);
		assertThat(adapter.canView(authentication)).isEqualTo(!roles.isEmpty());
		assertThat(adapter.canUpdate(authentication)).isEqualTo(roles.contains(Role.JANUS_ADMIN));
	}

	@Test
	void resolutionFailureDeniesBothOperations() {
		final Authentication authentication = mock(Authentication.class);
		final ActorResolver resolver = mock(ActorResolver.class);
		when(resolver.resolve(authentication)).thenThrow(new AccessDeniedException("unprovisioned"));
		final ApplicationSettingsAuthorizationAdapter adapter = new ApplicationSettingsAuthorizationAdapter(resolver);
		assertThatThrownBy(() -> adapter.canView(authentication)).isInstanceOf(AccessDeniedException.class);
		assertThatThrownBy(() -> adapter.canUpdate(authentication)).isInstanceOf(AccessDeniedException.class);
	}
}
