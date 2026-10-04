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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.ZoneId;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import es.nivel36.janus.service.TimeFormat;
import es.nivel36.janus.service.employee.Employee;
import es.nivel36.janus.service.schedule.Schedule;
import jakarta.persistence.EntityManager;

/**
 * Verifies that a claimed employee is preserved when provisioning rejects a
 * competing link.
 */
class AppUserCreatorTest {
	@Test
	void employeeClaimedBetweenLookupAndInsertKeepsItsExistingProfile() {
		final Employee employee = new Employee("EMP-42", "Jane", "Doe", "jane@example.test", mock(Schedule.class));
		final AppUser winner = new AppUser("jane@example.test", "winner", Locale.ENGLISH, TimeFormat.H24);
		winner.setEmployee(employee);
		final EntityManager entityManager = mock(EntityManager.class);
		when(entityManager.find(Employee.class, 42L)).thenReturn(employee);
		final AppUserRepository repository = mock(AppUserRepository.class);
		final AppUserCreator creator = new AppUserCreator(repository, entityManager);

		assertThatThrownBy(
				() -> creator
						.create("other@example.test", "loser", Locale.ENGLISH, TimeFormat.H24, ZoneId.of("UTC"), 42L))
				.isInstanceOf(AppUserCreationConflict.class);
		assertThat(employee.getAppUser()).isSameAs(winner);
		assertThat(winner.getEmployee()).isSameAs(employee);
		verifyNoInteractions(repository);
	}
}
