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
package es.nivel36.janus.api.v1.appuser;

import java.util.UUID;

import org.springframework.stereotype.Component;

import es.nivel36.janus.api.Mapper;
import es.nivel36.janus.service.TimeFormat;
import es.nivel36.janus.service.appuser.AppUser;
import es.nivel36.janus.service.appuser.Theme;
import es.nivel36.janus.service.employee.Employee;

/**
 * Maps application profiles to immutable public snapshots under the
 * {@link es.nivel36.janus.api.Mapper} contract. A nonnull source must have
 * nonnull preferences and a readable employee association (initialized or
 * accessible in an open persistence context). Mapping leaves the entity
 * unchanged, converts locale/time zone to identifiers, and exposes only the
 * employee number, never the provider subject. A {@code null} source produces
 * {@code null}; an unlinked source produces a {@code null} employee number.
 */
@Component
public class AppUserResponseMapper implements Mapper<AppUser, AppUserResponse> {

	/**
	 * Constructs a stateless application-profile response mapper.
	 */
	public AppUserResponseMapper() {
	}

	@Override
	public AppUserResponse map(final AppUser appUser) {
		if (appUser == null) {
			return null;
		}
		final UUID id = appUser.getId();
		final String email = appUser.getEmail();
		final Employee employee = appUser.getEmployee();
		final String employeeNumber = employee == null ? null : employee.getEmployeeNumber();
		final String locale = appUser.getLocale().toLanguageTag();
		final TimeFormat timeFormat = appUser.getTimeFormat();
		final String defaultTimeZone = appUser.getDefaultTimezone().getId();
		final Theme theme = appUser.getTheme();
		return new AppUserResponse(id, email, employeeNumber, locale, timeFormat, defaultTimeZone, theme);
	}
}
