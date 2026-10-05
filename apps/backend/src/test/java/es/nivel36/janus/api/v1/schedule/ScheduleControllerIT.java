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
package es.nivel36.janus.api.v1.schedule;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;
import static org.springframework.security.core.authority.AuthorityUtils.createAuthorityList;
import static es.nivel36.janus.api.v1.SecurityTestConfiguration.verifiedJwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import es.nivel36.janus.api.v1.EmployeeIdentityTestExecutionListener;
import es.nivel36.janus.api.v1.SecurityTestConfiguration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(SecurityTestConfiguration.class)
@Transactional
@TestExecutionListeners(listeners = EmployeeIdentityTestExecutionListener.class, mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
class ScheduleControllerIT {

	private static final String BASE = "/api/v1/schedules";

	private @Autowired MockMvc mvc;

	@Test
	void searchSchedulesShouldEnforceSearchQueryContract() throws Exception {
		this.mvc.perform(
				get(BASE).queryParam("query", "a".repeat(100))
						.with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_ADMIN"))))
				.andExpect(status().isOk());
		this.mvc.perform(
				get(BASE).queryParam("query", "")
						.with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_ADMIN"))))
				.andExpect(status().isBadRequest());
		this.mvc.perform(
				get(BASE).queryParam("query", "a".repeat(101))
						.with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_ADMIN"))))
				.andExpect(status().isBadRequest());
		this.mvc.perform(
				get(BASE).queryParam("query", "Turno\nMañana")
						.with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_ADMIN"))))
				.andExpect(status().isBadRequest());
	}

	@Test
	void testElevatedRolesWithoutVerifiedEmailAreUnauthorized() throws Exception {
		this.mvc.perform(get(BASE).header("Authorization", "Bearer email-unverified"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@Sql(statements = { "INSERT INTO schedule(id,code,name) VALUES "
			+ "(21,'SCH-20','Schedule 20'),(20,'SCH-19','Schedule 19'),(19,'SCH-18','Schedule 18'),(18,'SCH-17','Schedule 17'),(17,'SCH-16','Schedule 16'),"
			+ "(16,'SCH-15','Schedule 15'),(15,'SCH-14','Schedule 14'),(14,'SCH-13','Schedule 13'),(13,'SCH-12','Schedule 12'),(12,'SCH-11','Schedule 11'),"
			+ "(11,'SCH-10','Schedule 10'),(10,'SCH-09','Schedule 09'),(9,'SCH-08','Schedule 08'),(8,'SCH-07','Schedule 07'),(7,'SCH-06','Schedule 06'),"
			+ "(6,'SCH-05','Schedule 05'),(5,'SCH-04','Schedule 04'),(4,'SCH-03','Schedule 03'),(3,'SCH-02','Schedule 02'),(2,'SCH-01','Schedule 01'),(1,'SCH-00','Schedule 00')" })
	void searchWithoutPaginationUsesStableDefaults() throws Exception {
		this.mvc.perform(get(BASE).with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_ADMIN"))))
				.andExpect(status().isOk()).andExpect(jsonPath("$.page.number").value(0))
				.andExpect(jsonPath("$.page.size").value(20)).andExpect(jsonPath("$.content.length()").value(20))
				.andExpect(jsonPath("$.content[0].code").value("SCH-00"))
				.andExpect(jsonPath("$.content[19].code").value("SCH-19"));
	}

	@Test
	void testCreateScheduleShouldReturn201AndBody() throws Exception {
		final String body = """
				{
				  "code": "STD-WH",
				  "name": "Standard Work Hours",
				  "entryTolerance": "PT1H",
				  "exitTolerance": "PT1H",
				  "rules": [
				    {
				      "name": "Weekday Rule",
				      "dayOfWeekRanges": [
				        {
				          "dayOfWeek": "MONDAY",
				          "effectiveWorkHours": "PT8H",
				          "timeRange": {
				            "startTime": "09:00",
				            "endTime": "17:00"
				          }
				        }
				      ]
				    }
				  ]
				}
				""";

		this.mvc.perform(
				post(BASE).contentType(APPLICATION_JSON).content(body)
						.with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_ADMIN"))))
				.andExpect(status().isCreated()).andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
				.andExpect(jsonPath("$.code").value("STD-WH")).andExpect(jsonPath("$.entryTolerance").value("PT1H"))
				.andExpect(jsonPath("$.exitTolerance").value("PT1H"))
				.andExpect(jsonPath("$.rules[0].dayOfWeekRanges[0].effectiveWorkHours").value("PT8H"))
				.andExpect(jsonPath("$.rules[0].dayOfWeekRanges[0].timeRange.startTime").value("09:00:00"));
	}

	@Test
	void createScheduleAcceptsAnOvernightTimeRange() throws Exception {
		final String body = scheduleCreateBody("NIGHT", "PT8H", "22:00", "06:00");

		this.mvc.perform(
				post(BASE).contentType(APPLICATION_JSON).content(body)
						.with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_ADMIN"))))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.rules[0].dayOfWeekRanges[0].timeRange.startTime").value("22:00:00"))
				.andExpect(jsonPath("$.rules[0].dayOfWeekRanges[0].timeRange.endTime").value("06:00:00"));
	}

	@Test
	void createScheduleRejectsEqualTimeRangeBounds() throws Exception {
		final String body = scheduleCreateBody("ZERO", "PT0S", "22:00", "22:00");

		this.mvc.perform(
				post(BASE).contentType(APPLICATION_JSON).content(body)
						.with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_ADMIN"))))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON));
	}

	@Test
	void testCreateDuplicatedScheduleShouldReturn409() throws Exception {
		final String body = """
				{
				  "code": "STD-WH",
				  "name": "Standard Work Hours",
				  "entryTolerance": "PT1H",
				  "exitTolerance": "PT1H",
				  "rules": [
				    {
				      "name": "Weekday Rule",
				      "dayOfWeekRanges": [
				        {
				          "dayOfWeek": "MONDAY",
				          "effectiveWorkHours": "PT8H",
				          "timeRange": {
				            "startTime": "09:00",
				            "endTime": "17:00"
				          }
				        }
				      ]
				    }
				  ]
				}
				""";

		this.mvc.perform(
				post(BASE).contentType(APPLICATION_JSON).content(body)
						.with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_ADMIN"))))
				.andExpect(status().isCreated());

		this.mvc.perform(
				post(BASE).contentType(APPLICATION_JSON).content(body)
						.with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_ADMIN"))))
				.andExpect(status().isConflict())
				.andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON));
	}

	@Test
	void testSearchSchedulesShouldReturn200AndBody() throws Exception {
		final String body = """
				{
				  "code": "STD-WH",
				  "name": "Standard Work Hours",
				  "entryTolerance": "PT1H",
				  "exitTolerance": "PT1H",
				  "rules": [
				    {
				      "name": "Weekday Rule",
				      "dayOfWeekRanges": [
				        {
				          "dayOfWeek": "MONDAY",
				          "effectiveWorkHours": "PT8H",
				          "timeRange": {
				            "startTime": "09:00",
				            "endTime": "17:00"
				          }
				        }
				      ]
				    }
				  ]
				}
				""";

		this.mvc.perform(
				post(BASE).contentType(APPLICATION_JSON).content(body)
						.with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_ADMIN"))))
				.andExpect(status().isCreated());

		this.mvc.perform(get(BASE).with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_ADMIN"))))
				.andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
				.andExpect(jsonPath("$.page.totalElements").value(1)).andExpect(jsonPath("$.page.size").value(20))
				.andExpect(jsonPath("$.page.number").value(0)).andExpect(jsonPath("$.content[0].code").value("STD-WH"));
	}

	@ParameterizedTest
	@ValueSource(strings = { "Turno estándar", "estándar-mañana", "TURNO_MANANA" })
	void searchSchedulesAcceptsSpacesAccentsAndHyphens(final String query) throws Exception {
		final String body = """
				{
				  "code": "TURNO_MANANA",
				  "name": "Turno estándar-mañana",
				  "entryTolerance": "PT1H",
				  "exitTolerance": "PT1H",
				  "rules": [{
				    "name": "Laborables",
				    "dayOfWeekRanges": [{
				      "dayOfWeek": "MONDAY",
				      "effectiveWorkHours": "PT8H",
				      "timeRange": { "startTime": "09:00", "endTime": "17:00" }
				    }]
				  }]
				}
				""";

		this.mvc.perform(
				post(BASE).contentType(APPLICATION_JSON).content(body)
						.with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_ADMIN"))))
				.andExpect(status().isCreated());

		this.mvc.perform(
				get(BASE).param("query", query)
						.with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_ADMIN"))))
				.andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1))
				.andExpect(jsonPath("$.content[0].code").value("TURNO_MANANA"));

		this.mvc.perform(
				get(BASE).param("query", "%").with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_ADMIN"))))
				.andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty());
	}

	@ParameterizedTest
	@CsvSource({ "ROLE_JANUS_EMPLOYEE,employee-EMP-0001", "ROLE_JANUS_USER,user" })
	@Sql(statements = { "INSERT INTO schedule(id,code,name) VALUES(1,'STD-WH', 'Standard Work Hours')",
			"INSERT INTO employee(id,employee_number,name,surname,email,schedule_id) VALUES(1,'EMP-0001','Abel','Ferrer','aferrer@nivel36.es',1)" })
	void employeeNumberFilterIsAppliedForRestrictedAndPrivilegedUsers(final String role, final String subject)
			throws Exception {
		this.mvc.perform(
				get(BASE).param("employeeNumber", "EMP-0001")
						.with(verifiedJwt().jwt(jwt -> jwt.subject(subject)).authorities(createAuthorityList(role))))
				.andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1))
				.andExpect(jsonPath("$.content[0].code").value("STD-WH"));
	}

	@Test
	void testFindScheduleShouldReturn200AndBody() throws Exception {
		final String body = """
				{
				  "code": "STD-WH",
				  "name": "Standard Work Hours",
				  "entryTolerance": "PT1H",
				  "exitTolerance": "PT1H",
				  "rules": [
				    {
				      "name": "Weekday Rule",
				      "dayOfWeekRanges": [
				        {
				          "dayOfWeek": "MONDAY",
				          "effectiveWorkHours": "PT8H",
				          "timeRange": {
				            "startTime": "09:00",
				            "endTime": "17:00"
				          }
				        }
				      ]
				    }
				  ]
				}
				""";

		this.mvc.perform(
				post(BASE).contentType(APPLICATION_JSON).content(body)
						.with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_ADMIN"))))
				.andExpect(status().isCreated());

		this.mvc.perform(
				get(BASE + "/{code}", "STD-WH")
						.with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_ADMIN"))))
				.andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
				.andExpect(jsonPath("$.name").value("Standard Work Hours"))
				.andExpect(jsonPath("$.rules[0].dayOfWeekRanges[0].dayOfWeek").value("MONDAY"));
	}

	@Test
	void testUpdateScheduleShouldReturn200AndBody() throws Exception {
		final String createBody = """
				{
				  "code": "STD-WH",
				  "name": "Standard Work Hours",
				  "entryTolerance": "PT1H",
				  "exitTolerance": "PT1H",
				  "rules": [
				    {
				      "name": "Weekday Rule",
				      "dayOfWeekRanges": [
				        {
				          "dayOfWeek": "MONDAY",
				          "effectiveWorkHours": "PT8H",
				          "timeRange": {
				            "startTime": "09:00",
				            "endTime": "17:00"
				          }
				        }
				      ]
				    }
				  ]
				}
				""";

		this.mvc.perform(
				post(BASE).contentType(APPLICATION_JSON).content(createBody)
						.with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_ADMIN"))))
				.andExpect(status().isCreated());

		final String updateBody = """
				{
				  "name": "Updated Work Hours",
				  "entryTolerance": "PT1H",
				  "exitTolerance": "PT1H",
				  "rules": [
				    {
				      "name": "Weekend Rule",
				      "dayOfWeekRanges": [
				        {
				          "dayOfWeek": "SATURDAY",
				          "effectiveWorkHours": "PT6H",
				          "timeRange": {
				            "startTime": "10:00",
				            "endTime": "16:00"
				          }
				        }
				      ]
				    }
				  ]
				}
				""";

		this.mvc.perform(
				put(BASE + "/{code}", "STD-WH").contentType(APPLICATION_JSON).content(updateBody)
						.with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_ADMIN"))))
				.andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
				.andExpect(jsonPath("$.name").value("Updated Work Hours"))
				.andExpect(jsonPath("$.rules[0].dayOfWeekRanges[0].dayOfWeek").value("SATURDAY"));
	}

	@Test
	void updateScheduleAcceptsAnOvernightTimeRange() throws Exception {
		this.mvc.perform(
				post(BASE).contentType(APPLICATION_JSON).content(scheduleCreateBody("NIGHT", "PT8H", "09:00", "17:00"))
						.with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_ADMIN"))))
				.andExpect(status().isCreated());

		this.mvc.perform(
				put(BASE + "/{code}", "NIGHT").contentType(APPLICATION_JSON)
						.content(scheduleUpdateBody("PT8H", "22:00", "06:00"))
						.with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_ADMIN"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.rules[0].dayOfWeekRanges[0].effectiveWorkHours").value("PT8H"))
				.andExpect(jsonPath("$.rules[0].dayOfWeekRanges[0].timeRange.startTime").value("22:00:00"))
				.andExpect(jsonPath("$.rules[0].dayOfWeekRanges[0].timeRange.endTime").value("06:00:00"));
	}

	@Test
	void updateScheduleRejectsEqualTimeRangeBounds() throws Exception {
		this.mvc.perform(
				post(BASE).contentType(APPLICATION_JSON).content(scheduleCreateBody("ZERO", "PT8H", "09:00", "17:00"))
						.with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_ADMIN"))))
				.andExpect(status().isCreated());

		this.mvc.perform(
				put(BASE + "/{code}", "ZERO").contentType(APPLICATION_JSON)
						.content(scheduleUpdateBody("PT0S", "22:00", "22:00"))
						.with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_ADMIN"))))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON));
	}

	@Test
	void testDeleteScheduleShouldReturn204() throws Exception {
		final String body = """
				{
				  "code": "STD-WH",
				  "name": "Standard Work Hours",
				  "entryTolerance": "PT1H",
				  "exitTolerance": "PT1H",
				  "rules": [
				    {
				      "name": "Weekday Rule",
				      "dayOfWeekRanges": [
				        {
				          "dayOfWeek": "MONDAY",
				          "effectiveWorkHours": "PT8H",
				          "timeRange": {
				            "startTime": "09:00",
				            "endTime": "17:00"
				          }
				        }
				      ]
				    }
				  ]
				}
				""";

		this.mvc.perform(
				post(BASE).contentType(APPLICATION_JSON).content(body)
						.with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_ADMIN"))))
				.andExpect(status().isCreated());

		this.mvc.perform(
				delete(BASE + "/{code}", "STD-WH")
						.with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_ADMIN"))))
				.andExpect(status().isNoContent());
	}

	@Test
	@Sql(statements = { "INSERT INTO schedule(id,code,name) VALUES (1,'IN-USE','In Use Schedule')",
			"INSERT INTO employee(id,employee_number,name,surname,email,schedule_id) VALUES(1,'EMP-0001','Abel','Ferrer','aferrer@nivel36.es',1)" })
	void testDeleteScheduleWithAssignedEmployeesShouldReturn409() throws Exception {
		this.mvc.perform(
				delete(BASE + "/{code}", "IN-USE")
						.with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_ADMIN"))))
				.andExpect(status().isConflict())
				.andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON));
	}

	private static String scheduleCreateBody(
			final String code,
			final String effectiveWorkHours,
			final String startTime,
			final String endTime) {
		return """
				{
				  "code": "%s",
				  "name": "Schedule %s",
				  "entryTolerance": "PT1H",
				  "exitTolerance": "PT1H",
				  "rules": [{
				    "name": "Rule",
				    "dayOfWeekRanges": [{
				      "dayOfWeek": "MONDAY",
				      "effectiveWorkHours": "%s",
				      "timeRange": { "startTime": "%s", "endTime": "%s" }
				    }]
				  }]
				}
				""".formatted(code, code, effectiveWorkHours, startTime, endTime);
	}

	private static String scheduleUpdateBody(
			final String effectiveWorkHours,
			final String startTime,
			final String endTime) {
		return """
				{
				  "name": "Updated schedule",
				  "entryTolerance": "PT1H",
				  "exitTolerance": "PT1H",
				  "rules": [{
				    "name": "Updated rule",
				    "dayOfWeekRanges": [{
				      "dayOfWeek": "MONDAY",
				      "effectiveWorkHours": "%s",
				      "timeRange": { "startTime": "%s", "endTime": "%s" }
				    }]
				  }]
				}
				""".formatted(effectiveWorkHours, startTime, endTime);
	}
}
