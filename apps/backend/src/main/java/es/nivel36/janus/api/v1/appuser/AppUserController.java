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

import java.time.ZoneId;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.RestController;

import es.nivel36.janus.api.Mapper;
import es.nivel36.janus.service.TimeFormat;
import es.nivel36.janus.service.appuser.AppUser;
import es.nivel36.janus.service.appuser.AppUserService;
import es.nivel36.janus.service.appuser.Theme;

/**
 * Spring MVC implementation of {@link AppUserResource}.
 */
@RestController
public class AppUserController implements AppUserResource {

	private static final Logger logger = LoggerFactory.getLogger(AppUserController.class);

	private final AppUserService appUserService;
	private final Mapper<AppUser, AppUserResponse> appUserResponseMapper;

	/**
	 * Creates a controller ready to delegate resource operations without accessing
	 * persistence during construction.
	 *
	 * @param  appUserService        nonnull application-user service
	 * @param  appUserResponseMapper nonnull response mapper
	 * @throws NullPointerException  if either dependency is null
	 */
	public AppUserController(
		final AppUserService appUserService,
		final @Qualifier("appUserResponseMapper") Mapper<AppUser, AppUserResponse> appUserResponseMapper) {
		this.appUserService = Objects.requireNonNull(appUserService, "appUserService can't be null");
		this.appUserResponseMapper = Objects
				.requireNonNull(appUserResponseMapper, "appUserResponseMapper can't be null");
	}

	@Override
	public ResponseEntity<AppUserResponse> findCurrentAppUser(final JwtAuthenticationToken authentication) {
		logger.debug("Find app user ACTION performed");
		final Jwt token = authentication.getToken();
		final String email = token.getClaimAsString("email");
		final String subject = token.getSubject();
		final String employeeNumber = token.getClaimAsString("employeeNumber");
		final AppUser appUser = this.appUserService.findOrCreateAppUser(subject, email, employeeNumber);
		final AppUserResponse appUserResponse = this.appUserResponseMapper.map(appUser);
		return ResponseEntity.ok(appUserResponse);
	}

	@Override
	public ResponseEntity<Page<AppUserResponse>> searchAppUsers(
			final String emailFilter,
			final String employeeNumber,
			final Pageable pageable) {
		final Page<AppUser> appUsers = this.appUserService.searchAppUsers(emailFilter, employeeNumber, pageable);
		final Page<AppUserResponse> response = appUsers.map(this.appUserResponseMapper::map);
		return ResponseEntity.ok(response);
	}

	@Override
	public ResponseEntity<AppUserResponse> updateAppUser(final UUID id, final UpdateAppUserRequest request) {
		logger.debug("Update app user ACTION performed");
		final Locale forLanguageTag = Locale.forLanguageTag(request.locale().trim());
		final TimeFormat timeFormat = request.timeFormat();
		final ZoneId zoneId = ZoneId.of(request.defaultTimezone().trim());
		final Theme theme = request.theme();
		final AppUser updated = this.appUserService.updatePreferences(id, forLanguageTag, timeFormat, zoneId, theme);
		final AppUserResponse appUserResponse = this.appUserResponseMapper.map(updated);
		return ResponseEntity.ok(appUserResponse);
	}

	@Override
	public ResponseEntity<Void> deleteAppUser(final UUID id) {
		logger.debug("Delete app user ACTION performed");

		this.appUserService.deleteAppUser(id);
		return ResponseEntity.noContent().build();
	}

}
