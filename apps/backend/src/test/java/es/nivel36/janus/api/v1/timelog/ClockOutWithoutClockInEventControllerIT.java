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
package es.nivel36.janus.api.v1.timelog;

import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import es.nivel36.janus.api.v1.SecurityTestConfiguration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(SecurityTestConfiguration.class)
@Transactional
class ClockOutWithoutClockInEventControllerIT {

	private @MockitoBean Clock clock;
	private @Autowired MockMvc mvc;
	private static final String BASE = "/api/v1/employees/{employeeEmail}/clock-out-without-clock-in-events";

	@BeforeEach
	void beforeTest() {
		final Instant fixedNow = LocalDateTime.of(2025, 8, 8, 0, 0).toInstant(ZoneOffset.UTC);
		when(this.clock.instant()).thenReturn(fixedNow);
		when(this.clock.getZone()).thenReturn(ZoneOffset.UTC);
	}

	@Test
	@Sql(statements = { //
			"INSERT INTO application_settings (id, days_until_locked, employee_workplace_creation_allowed, worksite_change_during_shift_allowed, employee_manual_timelog_entry_allowed, default_timezone) VALUES (1, 7, true, false, true, 'Europe/Madrid')",
			"INSERT INTO schedule(id,code,name) VALUES(1,'STD-WH','Standard Work Hours')",
			"INSERT INTO employee(id,name,surname,email, schedule_id) VALUES(1,'Abel','Ferrer','aferrer@nivel36.es',1)",
			"INSERT INTO employee(id,name,surname,email, schedule_id) VALUES(2,'Ada','Lovelace','ada@nivel36.es',1)",
			"INSERT INTO worksite(id,code,name,time_zone,scope) VALUES(1,'HOME-AF','Home Office Abel','UTC+2','GLOBAL')",
			"INSERT INTO clock_out_without_clock_in_event(id,employee_id,worksite_id,exit_time,detected_at,resolved,invalidated) VALUES (1,1,1,'2025-08-04T16:00:00Z'::timestamp,'2025-08-04T16:00:00Z'::timestamp,false,false)" })
	void testFindClockOutWithoutClockInEventShouldAllowTransferredPersonalWorksite() throws Exception {
		final String exit = "2025-08-04T16:00:00Z";

		this.mvc.perform(get(BASE + "/{exitTime}", "aferrer@nivel36.es", exit) //
				.param("worksiteCode", "HOME-AF").with(jwt())) //
				.andExpect(status().isOk()) //
				.andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON)) //
				.andExpect(jsonPath("$.employeeEmail").value("aferrer@nivel36.es")) //
				.andExpect(jsonPath("$.worksiteCode").value("HOME-AF")) //
				.andExpect(jsonPath("$.exitTime").value(exit)) //
				.andExpect(jsonPath("$.resolved").value(false)) //
				.andExpect(jsonPath("$.invalidated").value(false));
	}

	@Test
	@Sql(statements = { //
			"INSERT INTO application_settings (id, days_until_locked, employee_workplace_creation_allowed, worksite_change_during_shift_allowed, employee_manual_timelog_entry_allowed, default_timezone) VALUES (1, 7, true, false, true, 'Europe/Madrid')",
			"INSERT INTO schedule(id,code,name) VALUES(1,'STD-WH','Standard Work Hours')",
			"INSERT INTO employee(id,name,surname,email, schedule_id) VALUES(1,'Abel','Ferrer','aferrer@nivel36.es',1)",
			"INSERT INTO employee(id,name,surname,email, schedule_id) VALUES(2,'Ada','Lovelace','ada@nivel36.es',1)",
			"INSERT INTO worksite(id,code,name,time_zone,scope) VALUES(1,'HOME-AF','Home Office Abel','UTC+2','GLOBAL')",
			"INSERT INTO clock_out_without_clock_in_event(id,employee_id,worksite_id,exit_time,detected_at,resolved,invalidated) VALUES (1,1,1,'2025-08-04T16:00:00Z'::timestamp,'2025-08-04T16:00:00Z'::timestamp,false,false)" })
	void testResolveClockOutWithoutClockInEventShouldAllowTransferredPersonalWorksite() throws Exception {
		final String exit = "2025-08-04T16:00:00Z";
		final String entry = "2025-08-04T09:00:00Z";
		final String body = """
				  {"entryTime":"%s","reason":"Worked from home before the transfer"}
				""".formatted(entry);

		this.mvc.perform(post(BASE + "/{exitTime}/resolve", "aferrer@nivel36.es", exit) //
				.param("worksiteCode", "HOME-AF") //
				.contentType(APPLICATION_JSON).content(body).with(jwt())) //
				.andExpect(status().isOk()) //
				.andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON)) //
				.andExpect(jsonPath("$.resolved").value(true)) //
				.andExpect(jsonPath("$.invalidated").value(false)) //
				.andExpect(jsonPath("$.reason").value("Worked from home before the transfer")) //
				.andExpect(jsonPath("$.resolvedTimeLogEntry").value(entry)) //
				.andExpect(jsonPath("$.resolvedTimeLogExitTime").value(exit));
	}

	@Test
	@Sql(statements = { //
			"INSERT INTO application_settings (id, days_until_locked, employee_workplace_creation_allowed, worksite_change_during_shift_allowed, employee_manual_timelog_entry_allowed, default_timezone) VALUES (1, 7, true, false, true, 'Europe/Madrid')",
			"INSERT INTO schedule(id,code,name) VALUES(1,'STD-WH','Standard Work Hours')",
			"INSERT INTO employee(id,name,surname,email, schedule_id) VALUES(1,'Abel','Ferrer','aferrer@nivel36.es',1)",
			"INSERT INTO employee(id,name,surname,email, schedule_id) VALUES(2,'Ada','Lovelace','ada@nivel36.es',1)",
			"INSERT INTO worksite(id,code,name,time_zone,scope) VALUES(1,'HOME-AF','Home Office Abel','UTC+2','GLOBAL')",
			"INSERT INTO clock_out_without_clock_in_event(id,employee_id,worksite_id,exit_time,detected_at,resolved,invalidated) VALUES (1,1,1,'2025-08-04T16:00:00Z'::timestamp,'2025-08-04T16:00:00Z'::timestamp,false,false)" })
	void testInvalidateClockOutWithoutClockInEventShouldAllowTransferredPersonalWorksite() throws Exception {
		final String exit = "2025-08-04T16:00:00Z";
		final String body = """
				  {"reason":"Handled manually after transfer"}
				""";

		this.mvc.perform(post(BASE + "/{exitTime}/invalidate", "aferrer@nivel36.es", exit) //
				.param("worksiteCode", "HOME-AF") //
				.contentType(APPLICATION_JSON).content(body).with(jwt())) //
				.andExpect(status().isOk()) //
				.andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON)) //
				.andExpect(jsonPath("$.resolved").value(false)) //
				.andExpect(jsonPath("$.invalidated").value(true)) //
				.andExpect(jsonPath("$.reason").value("Handled manually after transfer"));
	}

	@Test
	@Sql(statements = { //
			"INSERT INTO application_settings (id, days_until_locked, employee_workplace_creation_allowed, worksite_change_during_shift_allowed, employee_manual_timelog_entry_allowed, default_timezone) VALUES (1, 7, true, false, false, 'Europe/Madrid')",
			"INSERT INTO schedule(id,code,name) VALUES(1,'STD-WH','Standard Work Hours')",
			"INSERT INTO employee(id,name,surname,email, schedule_id) VALUES(1,'Abel','Ferrer','aferrer@nivel36.es',1)",
			"INSERT INTO employee(id,name,surname,email, schedule_id) VALUES(2,'Ada','Lovelace','ada@nivel36.es',1)",
			"INSERT INTO worksite(id,code,name,time_zone,scope) VALUES(1,'HOME-AF','Home Office Abel','UTC+2','GLOBAL')",
			"INSERT INTO clock_out_without_clock_in_event(id,employee_id,worksite_id,exit_time,detected_at,resolved,invalidated) VALUES (1,1,1,'2025-08-04T16:00:00Z'::timestamp,'2025-08-04T16:00:00Z'::timestamp,false,false)" })
	void testResolveClockOutWithoutClockInEventShouldReturnForbiddenWhenManualEntryDisabled() throws Exception {
		final String exit = "2025-08-04T16:00:00Z";
		final String entry = "2025-08-04T09:00:00Z";
		final String body = """
				  {"entryTime":"%s","reason":"Worked from home before the transfer"}
				""".formatted(entry);

		this.mvc.perform(post(BASE + "/{exitTime}/resolve", "aferrer@nivel36.es", exit) //
				.param("worksiteCode", "HOME-AF") //
				.contentType(APPLICATION_JSON).content(body).with(jwt())) //
				.andExpect(status().isForbidden());
	}
}
