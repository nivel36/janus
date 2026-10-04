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
package es.nivel36.janus.api.v1.applicationsettings;

import static es.nivel36.janus.api.v1.SecurityTestConfiguration.verifiedJwt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.core.authority.AuthorityUtils.createAuthorityList;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;

import es.nivel36.janus.api.v1.SecurityTestConfiguration;
import es.nivel36.janus.service.appuser.Role;
import jakarta.persistence.EntityManager;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(SecurityTestConfiguration.class)
@Transactional
@Sql(statements = {
		"INSERT INTO application_settings(id, days_until_locked, employee_workplace_creation_allowed, worksite_change_during_shift_allowed, employee_manual_timelog_entry_allowed, default_timezone) VALUES (1, 7, true, false, false, 'Europe/Madrid')",
		"INSERT INTO app_user(email,keycloak_subject,locale,time_format,default_timezone) VALUES ('settings-admin@example.test','user','en-US','H24','UTC')" })
class ApplicationSettingsControllerIT {

	private static final String BASE = "/api/v1/application-settings";
	private @Autowired MockMvc mvc;
	private @Autowired ObjectMapper objectMapper;
	private @Autowired EntityManager entityManager;
	private @Autowired JdbcTemplate jdbc;

	/*
	 * GET
	 */

	@ParameterizedTest
	@MethodSource("allRoles")
	void everyRecognizedRoleCanReadApplicationSettings(final Role role) throws Exception {
		this.mvc.perform(get(BASE).with(verifiedJwt().authorities(createAuthorityList(authority(role)))))
				.andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
				.andExpect(jsonPath("$.daysUntilLocked").value(7))
				.andExpect(jsonPath("$.employeeWorksiteCreationAllowed").value(true))
				.andExpect(jsonPath("$.worksiteChangeDuringShiftAllowed").value(false))
				.andExpect(jsonPath("$.employeeManualTimeLogEntryAllowed").value(false))
				.andExpect(jsonPath("$.defaultTimezone").value("Europe/Madrid"))
				.andExpect(jsonPath("$.employeeWorkplaceCreationAllowed").doesNotExist())
				.andExpect(jsonPath("$.employeeManualTimelogEntryAllowed").doesNotExist());
	}

	@Test
	void authenticatedUserWithoutRecognizedRoleCannotReadApplicationSettings() throws Exception {
		this.mvc.perform(get(BASE).with(verifiedJwt().authorities(createAuthorityList())))
				.andExpect(status().isForbidden());
	}

	@Test
	void anonymousUserCannotReadApplicationSettings() throws Exception {
		this.mvc.perform(get(BASE)).andExpect(status().isUnauthorized());
	}

	@Test
	void unprovisionedUserCannotReadApplicationSettingsEvenWithAdminRole() throws Exception {
		this.mvc.perform(
				get(BASE).with(
						verifiedJwt().jwt(jwt -> jwt.subject("unprovisioned"))
								.authorities(createAuthorityList("ROLE_JANUS_ADMIN"))))
				.andExpect(status().isForbidden());
	}

	/*
	 * PUT
	 */

	@Test
	void adminCanUpdateApplicationSettingsAndChangesArePersisted() throws Exception {
		this.mvc.perform(
				update(completeBody()).with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_ADMIN"))))
				.andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
				.andExpect(jsonPath("$.daysUntilLocked").value(3))
				.andExpect(jsonPath("$.employeeWorksiteCreationAllowed").value(false))
				.andExpect(jsonPath("$.worksiteChangeDuringShiftAllowed").value(true))
				.andExpect(jsonPath("$.employeeManualTimeLogEntryAllowed").value(true))
				.andExpect(jsonPath("$.defaultTimezone").value("UTC"))
				.andExpect(jsonPath("$.employeeWorkplaceCreationAllowed").doesNotExist())
				.andExpect(jsonPath("$.employeeManualTimelogEntryAllowed").doesNotExist());

		this.assertStored(3, false, true, true, "UTC");

		/*
		 * Verifies the complete write -> database -> read path rather than only
		 * checking the PUT response.
		 */
		this.mvc.perform(get(BASE).with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_EMPLOYEE"))))
				.andExpect(status().isOk()).andExpect(jsonPath("$.daysUntilLocked").value(3))
				.andExpect(jsonPath("$.employeeWorksiteCreationAllowed").value(false))
				.andExpect(jsonPath("$.worksiteChangeDuringShiftAllowed").value(true))
				.andExpect(jsonPath("$.employeeManualTimeLogEntryAllowed").value(true))
				.andExpect(jsonPath("$.defaultTimezone").value("UTC"));
	}

	@ParameterizedTest
	@MethodSource("nonAdminRoles")
	void nonAdminCannotUpdateApplicationSettings(final Role role) throws Exception {
		this.mvc.perform(update(completeBody()).with(verifiedJwt().authorities(createAuthorityList(authority(role)))))
				.andExpect(status().isForbidden());

		this.assertOriginalStored();
	}

	@Test
	void anonymousUserCannotUpdateApplicationSettings() throws Exception {
		this.mvc.perform(update(completeBody())).andExpect(status().isUnauthorized());

		this.assertOriginalStored();
	}

	@Test
	void unprovisionedUserCannotUpdateApplicationSettingsEvenWithAdminRole() throws Exception {
		this.mvc.perform(
				update(completeBody()).with(
						verifiedJwt().jwt(jwt -> jwt.subject("unprovisioned"))
								.authorities(createAuthorityList("ROLE_JANUS_ADMIN"))))
				.andExpect(status().isForbidden());

		this.assertOriginalStored();
	}

	/*
	 * Validation
	 */

	@ParameterizedTest
	@CsvSource({ "daysUntilLocked,false", "employeeWorksiteCreationAllowed,false",
			"worksiteChangeDuringShiftAllowed,false", "employeeManualTimeLogEntryAllowed,false",
			"defaultTimezone,false",

			"daysUntilLocked,true", "employeeWorksiteCreationAllowed,true", "worksiteChangeDuringShiftAllowed,true",
			"employeeManualTimeLogEntryAllowed,true", "defaultTimezone,true" })
	void missingOrNullFieldsAreRejectedWithoutChangingSettings(final String field, final boolean nullValue)
			throws Exception {

		final Map<String, Object> body = completeBody();

		if (nullValue) {
			body.put(field, null);
		} else {
			body.remove(field);
		}

		this.mvc.perform(update(body).with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_ADMIN"))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.type").value("urn:problem:validation-failed"));

		this.assertOriginalStored();
	}

	@ParameterizedTest
	@MethodSource("invalidValues")
	void invalidValuesAreRejectedWithoutChangingSettings(final String field, final Object value) throws Exception {

		final Map<String, Object> body = completeBody();
		body.put(field, value);

		this.mvc.perform(update(body).with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_ADMIN"))))
				.andExpect(status().isBadRequest());

		this.assertOriginalStored();
	}

	private static Stream<Arguments> invalidValues() {
		return Stream.of(
				/*
				 * Boundary immediately below the minimum valid value.
				 */
				Arguments.of("daysUntilLocked", -1),

				/*
				 * Whitespace is enough to exercise the blank validation; testing both "" and
				 * "   " adds little value.
				 */
				Arguments.of("defaultTimezone", "   "),

				/*
				 * Non-blank but semantically invalid ZoneId.
				 */
				Arguments.of("defaultTimezone", "Invalid/Zone"));
	}

	@ParameterizedTest
	@ValueSource(strings = { " UTC ", "+02:00" })
	void acceptsZeroDaysAndSupportedTrimmedZones(final String zone) throws Exception {
		final Map<String, Object> body = completeBody();

		body.put("daysUntilLocked", 0);
		body.put("defaultTimezone", zone);

		this.mvc.perform(update(body).with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_ADMIN"))))
				.andExpect(status().isOk()).andExpect(jsonPath("$.daysUntilLocked").value(0))
				.andExpect(jsonPath("$.defaultTimezone").value(zone.trim()));

		this.assertStored(0, false, true, true, zone.trim());
	}

	/*
	 * Application invariant
	 */

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void missingGlobalRowReturnsInternalErrorAndIsNotRecreated(final boolean write) throws Exception {

		this.jdbc.update("DELETE FROM application_settings");
		this.entityManager.clear();

		this.mvc.perform(
				(write ? update(completeBody()) : get(BASE))
						.with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_ADMIN"))))
				.andExpect(status().isInternalServerError()).andExpect(jsonPath("$.type").value("urn:problem:internal"))
				.andExpect(jsonPath("$.detail").value("An unexpected internal error occurred"));

		assertThat(this.jdbc.queryForObject("SELECT COUNT(*) FROM application_settings", Integer.class)).isZero();
	}

	/*
	 * Test data
	 */

	private static Stream<Role> allRoles() {
		return Arrays.stream(Role.values());
	}

	private static Stream<Role> nonAdminRoles() {
		return Arrays.stream(Role.values()).filter(role -> role != Role.JANUS_ADMIN);
	}

	private static String authority(final Role role) {
		return "ROLE_" + role.name();
	}

	private static Map<String, Object> completeBody() {
		return new LinkedHashMap<>(
				Map.of(
						"daysUntilLocked",
						3,
						"employeeWorksiteCreationAllowed",
						false,
						"worksiteChangeDuringShiftAllowed",
						true,
						"employeeManualTimeLogEntryAllowed",
						true,
						"defaultTimezone",
						"UTC"));
	}

	private MockHttpServletRequestBuilder update(final Map<String, Object> body) throws JsonProcessingException {

		return put(BASE).contentType(APPLICATION_JSON).content(this.objectMapper.writeValueAsString(body));
	}

	private void assertOriginalStored() {
		this.assertStored(7, true, false, false, "Europe/Madrid");
	}

	private void assertStored(
			final int days,
			final boolean creation,
			final boolean change,
			final boolean manual,
			final String zone) {

		this.entityManager.flush();
		this.entityManager.clear();

		final var rows = this.jdbc.queryForList("SELECT * FROM application_settings");

		assertThat(rows).hasSize(1);

		assertThat(rows.getFirst()).containsEntry("ID", 1L).containsEntry("DAYS_UNTIL_LOCKED", days)
				.containsEntry("EMPLOYEE_WORKPLACE_CREATION_ALLOWED", creation)
				.containsEntry("WORKSITE_CHANGE_DURING_SHIFT_ALLOWED", change)
				.containsEntry("EMPLOYEE_MANUAL_TIMELOG_ENTRY_ALLOWED", manual).containsEntry("DEFAULT_TIMEZONE", zone);
	}
}
