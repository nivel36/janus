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
package es.nivel36.janus.api.v1.worksite;

import static es.nivel36.janus.api.v1.SecurityTestConfiguration.verifiedJwt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.core.authority.AuthorityUtils.createAuthorityList;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import es.nivel36.janus.api.v1.EmployeeIdentityTestExecutionListener;
import es.nivel36.janus.api.v1.SecurityTestConfiguration;

import jakarta.persistence.EntityManager;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(SecurityTestConfiguration.class)
@Transactional
@TestExecutionListeners(listeners = EmployeeIdentityTestExecutionListener.class, mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
class WorksiteControllerIT {

	private @Autowired MockMvc mvc;
	private @Autowired ObjectMapper objectMapper;
	private @Autowired JdbcTemplate jdbc;
	private @Autowired EntityManager entityManager;

	private static final String BASE = "/api/v1/worksites";

	@Test
	void searchShouldEnforceSearchQueryContract() throws Exception {
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
				get(BASE).queryParam("query", "Madrid\nNorte")
						.with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_ADMIN"))))
				.andExpect(status().isBadRequest());
	}

	@Test
	void testElevatedRolesWithoutVerifiedEmailAreUnauthorized() throws Exception {
		this.mvc.perform(get(BASE).header("Authorization", "Bearer email-unverified"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@Sql(statements = {
			"INSERT INTO application_settings (id, days_until_locked, employee_workplace_creation_allowed, worksite_change_during_shift_allowed, employee_manual_timelog_entry_allowed, default_timezone) VALUES (1, 7, true, false, false, 'Europe/Madrid')",
			"INSERT INTO worksite(code,name,time_zone,scope) VALUES('BCN-HQ','Barcelona Headquarters','UTC+2','GLOBAL')" })
	void testListShouldReturnSeededWorksite() throws Exception {
		this.mvc.perform(get(BASE).with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_ADMIN"))))
				.andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
				.andExpect(jsonPath("$.content[?(@.code=='BCN-HQ')]").exists())
				.andExpect(jsonPath("$.content[?(@.code=='BCN-HQ' && @.scope=='GLOBAL')]").exists())
				.andExpect(jsonPath("$.content[?(@.code=='BCN-HQ' && @.active==true)]").exists());
	}

	@Test
	@Sql(statements = {
			"INSERT INTO application_settings (id, days_until_locked, employee_workplace_creation_allowed, worksite_change_during_shift_allowed, employee_manual_timelog_entry_allowed, default_timezone) VALUES (1, 7, true, false, false, 'Europe/Madrid')",
			"INSERT INTO worksite(code,name,time_zone,scope) VALUES "
					+ "('WS-20','Worksite 20','UTC','GLOBAL'),('WS-19','Worksite 19','UTC','GLOBAL'),('WS-18','Worksite 18','UTC','GLOBAL'),('WS-17','Worksite 17','UTC','GLOBAL'),('WS-16','Worksite 16','UTC','GLOBAL'),"
					+ "('WS-15','Worksite 15','UTC','GLOBAL'),('WS-14','Worksite 14','UTC','GLOBAL'),('WS-13','Worksite 13','UTC','GLOBAL'),('WS-12','Worksite 12','UTC','GLOBAL'),('WS-11','Worksite 11','UTC','GLOBAL'),"
					+ "('WS-10','Worksite 10','UTC','GLOBAL'),('WS-09','Worksite 09','UTC','GLOBAL'),('WS-08','Worksite 08','UTC','GLOBAL'),('WS-07','Worksite 07','UTC','GLOBAL'),('WS-06','Worksite 06','UTC','GLOBAL'),"
					+ "('WS-05','Worksite 05','UTC','GLOBAL'),('WS-04','Worksite 04','UTC','GLOBAL'),('WS-03','Worksite 03','UTC','GLOBAL'),('WS-02','Worksite 02','UTC','GLOBAL'),('WS-01','Worksite 01','UTC','GLOBAL'),('WS-00','Worksite 00','UTC','GLOBAL')" })
	void searchWithoutPaginationUsesStableDefaults() throws Exception {
		this.mvc.perform(get(BASE).with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_ADMIN"))))
				.andExpect(status().isOk()).andExpect(jsonPath("$.page.number").value(0))
				.andExpect(jsonPath("$.page.size").value(20)).andExpect(jsonPath("$.content.length()").value(20))
				.andExpect(jsonPath("$.content[0].code").value("WS-00"))
				.andExpect(jsonPath("$.content[19].code").value("WS-19"));
	}

	@ParameterizedTest
	@ValueSource(strings = { "Centro logístico", "logístico-norte", "material frágil", "Avenida de la Constitución",
			"50%", "MAD_NORTE", "C:\\Depot", "entrada!sur" })
	@Sql(statements = {
			"INSERT INTO application_settings (id, days_until_locked, employee_workplace_creation_allowed, worksite_change_during_shift_allowed, employee_manual_timelog_entry_allowed, default_timezone) VALUES (1, 7, true, false, false, 'Europe/Madrid')",
			"INSERT INTO worksite(code,name,time_zone,scope,description,address) VALUES('MAD_NORTE','Centro logístico-norte','Europe/Madrid','GLOBAL','Almacén de material frágil al 50%','Avenida de la Constitución, 24; C:\\Depot; entrada!sur')",
			"INSERT INTO worksite(code,name,time_zone,scope,description) VALUES('MADXNORTE','Centro secundario','Europe/Madrid','GLOBAL','Almacén al 500 por cien')" })
	void searchAcceptsFreeTextFromWorksiteFields(final String query) throws Exception {
		this.mvc.perform(
				get(BASE).param("query", query)
						.with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_ADMIN"))))
				.andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1))
				.andExpect(jsonPath("$.content[0].code").value("MAD_NORTE"));
	}

	@Test
	@Sql(statements = {
			"INSERT INTO application_settings (id, days_until_locked, employee_workplace_creation_allowed, worksite_change_during_shift_allowed, employee_manual_timelog_entry_allowed, default_timezone) VALUES (1, 7, true, false, false, 'Europe/Madrid')",
			"INSERT INTO schedule(id,code,name) VALUES(1,'STD-WH', 'Standard Work Hours')",
			"INSERT INTO employee(id,employee_number,name,surname,email,schedule_id) VALUES(1,'EMP-0001','Abel','Ferrer','aferrer@nivel36.es',1)",
			"INSERT INTO employee(id,employee_number,name,surname,email,schedule_id) VALUES(2,'EMP-0002','Berta','Person','bperson@nivel36.es',1)",
			"INSERT INTO worksite(code,name,time_zone,scope) VALUES('BCN-HQ','Barcelona Headquarters','UTC+2','GLOBAL')" })
	void testListAsEmployeeShouldRejectSearchingOtherEmployee() throws Exception {
		this.mvc.perform(
				get(BASE).param("employeeNumber", "EMP-0002").with(
						verifiedJwt().jwt(jwt -> jwt.claim("realm_access", Map.of("roles", List.of("janus_employee"))))
								.jwt(
										jwt -> jwt.subject("employee-EMP-0001").claim("email", "aferrer@nivel36.es")
												.claim("email_verified", true))
								.authorities(createAuthorityList("ROLE_JANUS_EMPLOYEE", "SCOPE_read"))))
				.andExpect(status().isForbidden());
	}

	@ParameterizedTest
	@CsvSource({ "ROLE_JANUS_EMPLOYEE,employee-EMP-0001", "ROLE_JANUS_USER,user" })
	@Sql(statements = {
			"INSERT INTO application_settings (id, days_until_locked, employee_workplace_creation_allowed, worksite_change_during_shift_allowed, employee_manual_timelog_entry_allowed, default_timezone) VALUES (1, 7, true, false, false, 'Europe/Madrid')",
			"INSERT INTO schedule(id,code,name) VALUES(1,'STD-WH', 'Standard Work Hours')",
			"INSERT INTO employee(id,employee_number,name,surname,email,schedule_id) VALUES(1,'EMP-0001','Abel','Ferrer','aferrer@nivel36.es',1)",
			"INSERT INTO worksite(id,code,name,time_zone,scope) VALUES(1,'BCN-HQ','Barcelona Headquarters','UTC+2','ASSIGNED')",
			"INSERT INTO employee_worksite(employee_id,worksite_id) VALUES(1,1)" })
	void employeeNumberFilterWorksForRestrictedAndPrivilegedUsers(final String role, final String subject)
			throws Exception {
		this.mvc.perform(
				get(BASE).param("employeeNumber", "EMP-0001")
						.with(verifiedJwt().jwt(jwt -> jwt.subject(subject)).authorities(createAuthorityList(role))))
				.andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1))
				.andExpect(jsonPath("$.content[0].code").value("BCN-HQ"));
	}

	@Test
	@Sql(statements = {
			"INSERT INTO application_settings (id, days_until_locked, employee_workplace_creation_allowed, worksite_change_during_shift_allowed, employee_manual_timelog_entry_allowed, default_timezone) VALUES (1, 7, true, false, false, 'Europe/Madrid')",
			"INSERT INTO worksite(code,name,time_zone,scope) VALUES('GLOBAL-1','Global Worksite','UTC+2','GLOBAL')" })
	void employeeWithoutPersistentEmployeeLinkCannotSearch() throws Exception {
		this.mvc.perform(
				get(BASE).with(
						verifiedJwt().jwt(jwt -> jwt.claim("realm_access", Map.of("roles", List.of("janus_employee"))))
								.authorities(createAuthorityList("ROLE_JANUS_EMPLOYEE"))))
				.andExpect(status().isForbidden());
	}

	@Test
	@Sql(statements = {
			"INSERT INTO application_settings (id, days_until_locked, employee_workplace_creation_allowed, worksite_change_during_shift_allowed, employee_manual_timelog_entry_allowed, default_timezone) VALUES (1, 7, true, false, false, 'Europe/Madrid')",
			"INSERT INTO worksite(code,name,time_zone,scope) VALUES('BCN-HQ','Barcelona Headquarters','UTC+2','GLOBAL')" })
	void testFindByCodeShouldReturnWorksite() throws Exception {
		final MvcResult result = this.mvc
				.perform(
						get(BASE + "/{code}", "BCN-HQ")
								.with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_ADMIN"))))
				.andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
				.andExpect(jsonPath("$.code").value("BCN-HQ"))
				.andExpect(jsonPath("$.name").value("Barcelona Headquarters"))
				.andExpect(jsonPath("$.timeZone").value("UTC+02:00")).andExpect(jsonPath("$.scope").value("GLOBAL"))
				.andExpect(jsonPath("$.active").value(true)).andReturn();

		final JsonNode response = this.objectMapper.readTree(result.getResponse().getContentAsByteArray());
		assertThat(response.propertyNames()).as("JSON properties must match the public WorksiteResponse model")
				.containsExactlyInAnyOrder("code", "name", "timeZone", "scope", "description", "address", "active");
	}

	@Test
	@Sql(statements = {
			"INSERT INTO application_settings (id, days_until_locked, employee_workplace_creation_allowed, worksite_change_during_shift_allowed, employee_manual_timelog_entry_allowed, default_timezone) VALUES (1, 7, true, false, false, 'Europe/Madrid')",
			"INSERT INTO schedule(id,code,name) VALUES(1,'STD-WH', 'Standard Work Hours')",
			"INSERT INTO employee(id,employee_number,name,surname,email,schedule_id) VALUES(1,'EMP-0001','Abel','Ferrer','aferrer@nivel36.es',1)",
			"INSERT INTO worksite(id,code,name,time_zone,scope) VALUES(1,'VISIBLE','Visible Worksite','UTC','ASSIGNED')",
			"INSERT INTO worksite(id,code,name,time_zone,scope) VALUES(2,'OUTSIDE','Outside Worksite','UTC','ASSIGNED')",
			"INSERT INTO employee_worksite(employee_id,worksite_id) VALUES(1,1)" })
	void employeeCanFindVisibleWorksiteButNotOneOutsideTheirScope() throws Exception {
		final JwtRequestPostProcessor employeeJwt = verifiedJwt().jwt(jwt -> jwt.subject("employee-EMP-0001"))
				.authorities(createAuthorityList("ROLE_JANUS_EMPLOYEE"));

		this.mvc.perform(get(BASE + "/{code}", "VISIBLE").with(employeeJwt)).andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value("VISIBLE"));

		this.mvc.perform(get(BASE + "/{code}", "OUTSIDE").with(employeeJwt)).andExpect(status().isForbidden());

		this.mvc.perform(get(BASE + "/{code}", "MISSING").with(employeeJwt)).andExpect(status().isForbidden());
	}

	@Test
	void testFindByUnknownCodeShouldReturn404() throws Exception {
		this.mvc.perform(
				get(BASE + "/{code}", "BCN-HQ")
						.with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_ADMIN"))))
				.andExpect(status().isNotFound());
	}

	@Test
	void testFindByCodeWithInvalidPatternShouldFail400() throws Exception {
		this.mvc.perform(get(BASE + "/{code}", "BAD CODE WITH SPACE").with(verifiedJwt()))
				.andExpect(status().isBadRequest());
	}

	@ParameterizedTest
	@ValueSource(strings = { " ", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "bad employee!" })
	void assignmentWithInvalidEmployeeNumberShouldFail400(final String employeeNumber) throws Exception {
		this.mvc.perform(
				put(BASE + "/{code}/employees/{employeeNumber}", "BCN-HQ", employeeNumber)
						.with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_ADMIN"))))
				.andExpect(status().isBadRequest());
	}

	@Test
	@Sql(statements = {
			"INSERT INTO application_settings (id, days_until_locked, employee_workplace_creation_allowed, worksite_change_during_shift_allowed, employee_manual_timelog_entry_allowed, default_timezone) VALUES (1, 7, true, false, false, 'Europe/Madrid')",
			"INSERT INTO worksite(code,name,time_zone,scope) VALUES('BCN-HQ','Barcelona Headquarters','UTC+2','GLOBAL')" })
	void testCreateAlreadyExistsShouldReturn409() throws Exception {
		final String code = "BCN-HQ";
		final String body = """
				  {"code":"%s","name":"Barcelona Headquarters","timeZone":"Europe/Madrid","scope":"GLOBAL"}
				""".formatted(code);

		this.mvc.perform(
				post(BASE).contentType(APPLICATION_JSON).content(body)
						.with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_ADMIN"))))
				.andExpect(status().isConflict());
	}

	@Test
	void testCreateShouldReturn201AndBody() throws Exception {
		final String code = "MAD-HUB";
		final String body = """
				  {"code":"%s","name":"Madrid Hub","timeZone":"Europe/Madrid","scope":"GLOBAL"}
				""".formatted(code);

		this.mvc.perform(
				post(BASE).contentType(APPLICATION_JSON).content(body)
						.with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_ADMIN"))))
				.andExpect(status().isCreated()).andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
				.andExpect(jsonPath("$.code").value(code)).andExpect(jsonPath("$.name").value("Madrid Hub"))
				.andExpect(jsonPath("$.timeZone").value("Europe/Madrid"))
				.andExpect(jsonPath("$.scope").value("GLOBAL"));

		this.mvc.perform(get(BASE).with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_ADMIN"))))
				.andExpect(status().isOk()).andExpect(jsonPath("$.content[?(@.code=='%s')]".formatted(code)).exists());
	}

	@Test
	@Sql(statements = {
			"INSERT INTO application_settings (id, days_until_locked, employee_workplace_creation_allowed, worksite_change_during_shift_allowed, employee_manual_timelog_entry_allowed, default_timezone) VALUES (1, 7, true, false, false, 'Europe/Madrid')",
			"INSERT INTO worksite(code,name,time_zone,scope) VALUES('BCN-HQ','Barcelona Headquarters','UTC+2','GLOBAL')" })
	void testUpdateShouldReturn200AndUpdatedBody() throws Exception {
		final String body = """
				  {"name":"Barcelona","timeZone":"UTC+1","scope":"GLOBAL"}
				""";

		this.mvc.perform(
				put(BASE + "/{code}", "BCN-HQ").contentType(APPLICATION_JSON).content(body)
						.with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_ADMIN"))))
				.andExpect(status().isOk()).andExpect(jsonPath("$.code").value("BCN-HQ"))
				.andExpect(jsonPath("$.name").value("Barcelona")).andExpect(jsonPath("$.timeZone").value("UTC+01:00"))
				.andExpect(jsonPath("$.scope").value("GLOBAL"));
	}

	@Test
	@Sql(statements = {
			"INSERT INTO application_settings (id, days_until_locked, employee_workplace_creation_allowed, worksite_change_during_shift_allowed, employee_manual_timelog_entry_allowed, default_timezone) VALUES (1, 7, true, false, false, 'Europe/Madrid')",
			"INSERT INTO schedule(id,code,name) VALUES(1,'STD-WH', 'Standard Work Hours')",
			"INSERT INTO employee(id,employee_number,name,surname,email,schedule_id) VALUES(1,'EMP-0001','Abel','Ferrer','aferrer@nivel36.es',1)",
			"INSERT INTO worksite(code,name,time_zone,scope) VALUES('BCN-HQ','Barcelona Headquarters','UTC+2','ASSIGNED')" })
	void updateAssignedScopeToGlobalShouldReturnUpdatedBody() throws Exception {
		final String body = """
				  {"name":"Barcelona Home","timeZone":"UTC+1","scope":"GLOBAL"}
				""";

		this.mvc.perform(
				put(BASE + "/{code}", "BCN-HQ").contentType(APPLICATION_JSON).content(body)
						.with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_ADMIN"))))
				.andExpect(status().isOk()).andExpect(jsonPath("$.scope").value("GLOBAL"));
	}

	@Test
	@Sql(statements = {
			"INSERT INTO application_settings (id, days_until_locked, employee_workplace_creation_allowed, worksite_change_during_shift_allowed, employee_manual_timelog_entry_allowed, default_timezone) VALUES (1, 7, true, false, false, 'Europe/Madrid')",
			"INSERT INTO schedule(id,code,name) VALUES(1,'STD-WH', 'Standard Work Hours')",
			"INSERT INTO employee(id,employee_number,name,surname,email,schedule_id) VALUES(1,'EMP-0001','Abel','Ferrer','aferrer@nivel36.es',1)",
			"INSERT INTO employee(id,employee_number,name,surname,email,schedule_id) VALUES(2,'EMP-0002','Berta','Person','bperson@nivel36.es',1)",
			"INSERT INTO worksite(id,code,name,time_zone,scope) VALUES(1,'BCN-HQ','Barcelona Headquarters','UTC+2','ASSIGNED')",
			"INSERT INTO employee_worksite(employee_id,worksite_id) VALUES(1,1)" })
	void testUpdateAssignedWorksiteAsEmployeeShouldAllowAssignedAndRejectUnassignedEmployee() throws Exception {
		final String body = """
				  {"name":"Barcelona Assigned","timeZone":"UTC+1","scope":"ASSIGNED"}
				""";

		this.mvc.perform(
				put(BASE + "/{code}", "BCN-HQ").contentType(APPLICATION_JSON).content(body).with(
						verifiedJwt().jwt(jwt -> jwt.claim("realm_access", Map.of("roles", List.of("janus_employee"))))
								.jwt(
										jwt -> jwt.subject("employee-EMP-0001").claim("email", "aferrer@nivel36.es")
												.claim("email_verified", true))
								.authorities(createAuthorityList("ROLE_JANUS_EMPLOYEE", "SCOPE_read"))))
				.andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Barcelona Assigned"))
				.andExpect(jsonPath("$.scope").value("ASSIGNED"));

		this.mvc.perform(
				put(BASE + "/{code}", "BCN-HQ").contentType(APPLICATION_JSON).content(body).with(
						verifiedJwt().jwt(jwt -> jwt.claim("realm_access", Map.of("roles", List.of("janus_employee"))))
								.jwt(
										jwt -> jwt.subject("employee-EMP-0002").claim("email", "bperson@nivel36.es")
												.claim("email_verified", true))
								.authorities(createAuthorityList("ROLE_JANUS_EMPLOYEE", "SCOPE_read"))))
				.andExpect(status().isForbidden());
	}

	@Test
	@Sql(statements = {
			"INSERT INTO application_settings (id, days_until_locked, employee_workplace_creation_allowed, worksite_change_during_shift_allowed, employee_manual_timelog_entry_allowed, default_timezone) VALUES (1, 7, true, false, false, 'Europe/Madrid')",
			"INSERT INTO worksite(code,name,time_zone,scope) VALUES('BCN-HQ','Barcelona Headquarters','UTC+2','GLOBAL')" })
	void testDeleteShouldReturn204AndRemoveFromList() throws Exception {
		this.mvc.perform(
				delete(BASE + "/{code}", "BCN-HQ")
						.with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_ADMIN"))))
				.andExpect(status().isNoContent());

		this.mvc.perform(get(BASE).with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_ADMIN"))))
				.andExpect(status().isOk()).andExpect(jsonPath("$.content[?(@.code=='BCN-HQ')]").doesNotExist());
	}

	@Test
	@Sql(statements = {
			"INSERT INTO application_settings (id, days_until_locked, employee_workplace_creation_allowed, worksite_change_during_shift_allowed, employee_manual_timelog_entry_allowed, default_timezone) VALUES (1, 7, true, false, false, 'Europe/Madrid')",
			"INSERT INTO schedule(id,code,name) VALUES(1,'STD-WH', 'Standard Work Hours')",
			"INSERT INTO employee(id,employee_number,name,surname,email,schedule_id) VALUES(1,'EMP-0001','Abel','Ferrer','aferrer@nivel36.es',1)",
			"INSERT INTO worksite(id,code,name,time_zone,scope) VALUES(1,'BCN-HQ','Barcelona Headquarters','UTC+2','ASSIGNED')",
			"INSERT INTO employee_worksite(employee_id,worksite_id) VALUES(1,1)" })
	void testRemoveEmployeeFromWorksiteShouldReturn204WithoutContent() throws Exception {
		this.mvc.perform(
				delete(BASE + "/{code}/employees/{employeeNumber}", "BCN-HQ", "EMP-0001")
						.with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_ADMIN"))))
				.andExpect(status().isNoContent()).andExpect(header().doesNotExist("Content-Type"))
				.andExpect(content().string(""));
	}

	private static JwtRequestPostProcessor actor(final String subject, final String role) {
		return verifiedJwt().jwt(jwt -> jwt.subject(subject)).authorities(createAuthorityList(role));
	}

	@Test
	@Sql("/sql/worksite-contract.sql")
	void searchCombinesTextWithGlobalAndAssignedVisibilityAndExcludesDeletedWorksites() throws Exception {
		final JwtRequestPostProcessor employee = actor("employee-EMP-0201", "ROLE_JANUS_EMPLOYEE");
		this.mvc.perform(get(BASE).with(employee)).andExpect(status().isOk())
				.andExpect(jsonPath("$.page.totalElements").value(2))
				.andExpect(jsonPath("$.content[0].code").value("CONTRACT-A"))
				.andExpect(jsonPath("$.content[1].code").value("CONTRACT-G"));
		this.mvc.perform(get(BASE).param("query", "DEPOT").with(employee)).andExpect(status().isOk())
				.andExpect(jsonPath("$.page.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].code").value("CONTRACT-A"));
		this.mvc.perform(get(BASE).param("query", "depot").with(actor("user", "ROLE_JANUS_ADMIN")))
				.andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements").value(2));
		this.mvc.perform(get(BASE).param("query", " depot ").with(actor("user", "ROLE_JANUS_ADMIN")))
				.andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty());
		this.mvc.perform(get(BASE).param("employeeNumber", "EMP-0202").with(actor("user", "ROLE_JANUS_ADMIN")))
				.andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements").value(2))
				.andExpect(jsonPath("$.content[0].code").value("CONTRACT-B"))
				.andExpect(jsonPath("$.content[1].code").value("CONTRACT-G"));
		this.mvc.perform(
				get(BASE).param("query", "CONTRACT-A").param("employeeNumber", "EMP-0202")
						.with(actor("user", "ROLE_JANUS_ADMIN")))
				.andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty());
	}

	@Test
	@Sql("/sql/worksite-contract.sql")
	void searchUsesCodeTieBreakerAcrossPagesAndPreservesExplicitDirection() throws Exception {
		this.mvc.perform(
				get(BASE).param("query", "depot").param("sort", "name,desc").param("size", "1")
						.with(actor("user", "ROLE_JANUS_ADMIN")))
				.andExpect(status().isOk()).andExpect(jsonPath("$.content[0].code").value("CONTRACT-A"))
				.andExpect(jsonPath("$.page.totalElements").value(2)).andExpect(jsonPath("$.page.totalPages").value(2));
		this.mvc.perform(
				get(BASE).param("query", "depot").param("sort", "name,desc").param("size", "1").param("page", "1")
						.with(actor("user", "ROLE_JANUS_ADMIN")))
				.andExpect(status().isOk()).andExpect(jsonPath("$.content[0].code").value("CONTRACT-B"));
		this.mvc.perform(
				get(BASE).param("query", "depot").param("sort", "code,desc").with(actor("user", "ROLE_JANUS_ADMIN")))
				.andExpect(status().isOk()).andExpect(jsonPath("$.content[0].code").value("CONTRACT-B"));
		this.mvc.perform(get(BASE).param("size", "1000").with(actor("user", "ROLE_JANUS_ADMIN")))
				.andExpect(status().isOk()).andExpect(jsonPath("$.page.size").value(100));
	}

	@ParameterizedTest
	@ValueSource(strings = { "code", "name", "timeZone", "scope", "description", "address" })
	@Sql("/sql/worksite-contract.sql")
	void searchAcceptsSupportedPublicSortFields(final String property) throws Exception {
		this.mvc.perform(get(BASE).param("sort", property + ",desc").with(actor("user", "ROLE_JANUS_ADMIN")))
				.andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements").value(3));
	}

	@ParameterizedTest
	@ValueSource(strings = { "id", "deleted", "employees.employeeNumber", "timeLogs.entryTime", "unknown" })
	void searchRejectsUnsupportedSortFieldsWith400(final String property) throws Exception {
		this.mvc.perform(get(BASE).param("sort", property + ",asc").with(actor("user", "ROLE_JANUS_ADMIN")))
				.andExpect(status().isBadRequest());
	}

	@ParameterizedTest
	@ValueSource(strings = { "", " ", " EMP-0201 ", "bad employee!" })
	void searchRejectsInvalidEmployeeFilterWith400(final String number) throws Exception {
		this.mvc.perform(get(BASE).param("employeeNumber", number).with(actor("user", "ROLE_JANUS_ADMIN")))
				.andExpect(status().isBadRequest());
	}

	@Test
	@Sql("/sql/worksite-contract.sql")
	void searchRequiresAuthenticationAndProvisionedActor() throws Exception {
		this.mvc.perform(get(BASE)).andExpect(status().isUnauthorized());
		this.mvc.perform(get(BASE).with(actor("missing-worksite-actor", "ROLE_JANUS_ADMIN")))
				.andExpect(status().isForbidden());
		this.mvc.perform(get(BASE).with(actor("user", "ROLE_JANUS_EMPLOYEE"))).andExpect(status().isForbidden());
	}

	@Test
	@Sql("/sql/worksite-contract.sql")
	void updatePersistsTrimmedFieldsAndPreservesAssignments() throws Exception {
		this.mvc.perform(
				put(BASE + "/CONTRACT-A").with(actor("user", "ROLE_JANUS_ADMIN")).contentType(APPLICATION_JSON).content(
						"""
								{"name":" Updated depot ","timeZone":"Europe/Madrid","scope":"GLOBAL","description":" Description ","address":" Address "}
								"""))
				.andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Updated depot"))
				.andExpect(jsonPath("$.description").value("Description"))
				.andExpect(jsonPath("$.address").value("Address"));
		this.entityManager.flush();
		this.entityManager.clear();
		assertThat(this.jdbc.queryForObject("SELECT name FROM worksite WHERE code='CONTRACT-A'", String.class))
				.isEqualTo("Updated depot");
		assertThat(
				this.jdbc.queryForObject("SELECT COUNT(*) FROM employee_worksite WHERE worksite_id=201", Integer.class))
				.isOne();
		this.mvc.perform(get(BASE + "/CONTRACT-A").with(actor("user", "ROLE_JANUS_ADMIN"))).andExpect(status().isOk())
				.andExpect(jsonPath("$.timeZone").value("Europe/Madrid")).andExpect(jsonPath("$.scope").value("GLOBAL"))
				.andExpect(jsonPath("$.description").value("Description"))
				.andExpect(jsonPath("$.address").value("Address"));
	}

	@ParameterizedTest
	@ValueSource(strings = { "{\"name\": \" \", \"timeZone\": \"UTC\", \"scope\": \"ASSIGNED\"}",
			"{\"name\": \"Changed\", \"timeZone\": \"invalid/zone\", \"scope\": \"ASSIGNED\"}",
			"{\"name\": \"Changed\", \"timeZone\": \"UTC\"}" })
	@Sql("/sql/worksite-contract.sql")
	void invalidUpdateDoesNotChangeWorksite(final String body) throws Exception {
		this.mvc.perform(
				put(BASE + "/CONTRACT-A").with(actor("user", "ROLE_JANUS_ADMIN")).contentType(APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest());
		this.entityManager.flush();
		assertThat(this.jdbc.queryForObject("SELECT name FROM worksite WHERE code='CONTRACT-A'", String.class))
				.isEqualTo("Shared depot");
		assertThat(this.jdbc.queryForObject("SELECT description FROM worksite WHERE code='CONTRACT-A'", String.class))
				.isEqualTo("Old description");
	}

	@Test
	@Sql("/sql/worksite-contract.sql")
	void repeatedAssignmentAndRemovalPersistOneLinkThenNoLinks() throws Exception {
		for (int attempt = 0; attempt < 2; attempt++) {
			this.mvc.perform(put(BASE + "/CONTRACT-A/employees/EMP-0202").with(actor("user", "ROLE_JANUS_ADMIN")))
					.andExpect(status().isNoContent());
		}
		this.entityManager.flush();
		this.entityManager.clear();
		assertThat(
				this.jdbc.queryForObject(
						"SELECT COUNT(*) FROM employee_worksite WHERE employee_id=202 AND worksite_id=201",
						Integer.class))
				.isOne();
		for (int attempt = 0; attempt < 2; attempt++) {
			this.mvc.perform(delete(BASE + "/CONTRACT-A/employees/EMP-0202").with(actor("user", "ROLE_JANUS_ADMIN")))
					.andExpect(status().isNoContent());
		}
		this.entityManager.flush();
		assertThat(
				this.jdbc.queryForObject(
						"SELECT COUNT(*) FROM employee_worksite WHERE employee_id=202 AND worksite_id=201",
						Integer.class))
				.isZero();
	}

	@Test
	@Sql("/sql/worksite-contract.sql")
	void logicalDeletionPreservesHistoricalTimeLogsAndHidesWorksite() throws Exception {
		this.mvc.perform(delete(BASE + "/CONTRACT-A/employees/EMP-0201").with(actor("user", "ROLE_JANUS_ADMIN")))
				.andExpect(status().isNoContent());
		this.mvc.perform(delete(BASE + "/CONTRACT-A").with(actor("user", "ROLE_JANUS_ADMIN")))
				.andExpect(status().isNoContent());
		this.entityManager.flush();
		this.entityManager.clear();
		assertThat(this.jdbc.queryForObject("SELECT deleted FROM worksite WHERE code='CONTRACT-A'", Boolean.class))
				.isTrue();
		assertThat(this.jdbc.queryForObject("SELECT COUNT(*) FROM time_log WHERE worksite_id=201", Integer.class))
				.isEqualTo(6);
		this.mvc.perform(get(BASE + "/CONTRACT-A").with(actor("user", "ROLE_JANUS_ADMIN")))
				.andExpect(status().isNotFound());
		this.mvc.perform(get(BASE).param("query", "CONTRACT-A").with(actor("user", "ROLE_JANUS_ADMIN")))
				.andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty());
	}

	@Test
	@Sql("/sql/worksite-contract.sql")
	void statisticsCountDistinctEmployeesAndSchedulesWithinHalfOpenInterval() throws Exception {
		this.mvc.perform(
				get(BASE + "/CONTRACT-A/stats").param("start", "2026-10-01T08:00:00Z")
						.param("end", "2026-10-01T12:00:00Z").with(actor("user", "ROLE_JANUS_ADMIN")))
				.andExpect(status().isOk()).andExpect(jsonPath("$.worksiteCode").value("CONTRACT-A"))
				.andExpect(jsonPath("$.start").value("2026-10-01T08:00:00Z"))
				.andExpect(jsonPath("$.end").value("2026-10-01T12:00:00Z"))
				.andExpect(jsonPath("$.employeesWhoClockedIn").value(2))
				.andExpect(jsonPath("$.erroneousTimeLogs").value(1)).andExpect(jsonPath("$.totalTimeLogs").value(3))
				.andExpect(jsonPath("$.employeesAllowedToClockIn").value(1))
				.andExpect(jsonPath("$.distinctSchedulesFromEmployeesWhoClockedIn").value(2));
	}

	@ParameterizedTest
	@ValueSource(strings = { "2026-10-01T08:00:00Z", "2026-10-01T07:00:00Z" })
	@Sql("/sql/worksite-contract.sql")
	void statisticsRejectEmptyOrReversedInterval(final String end) throws Exception {
		this.mvc.perform(
				get(BASE + "/CONTRACT-A/stats").param("start", "2026-10-01T08:00:00Z").param("end", end)
						.with(actor("user", "ROLE_JANUS_ADMIN")))
				.andExpect(status().isBadRequest());
	}

	@Test
	@Sql("/sql/worksite-contract.sql")
	void statisticsRequireAssignmentForEmployeesAndExistingWorksiteForElevatedActors() throws Exception {
		this.mvc.perform(
				get(BASE + "/CONTRACT-A/stats").param("start", "2026-10-01T08:00:00Z")
						.param("end", "2026-10-01T12:00:00Z").with(actor("employee-EMP-0201", "ROLE_JANUS_EMPLOYEE")))
				.andExpect(status().isOk());
		this.mvc.perform(
				get(BASE + "/CONTRACT-A/stats").param("start", "2026-10-01T08:00:00Z")
						.param("end", "2026-10-01T12:00:00Z").with(actor("employee-EMP-0202", "ROLE_JANUS_EMPLOYEE")))
				.andExpect(status().isForbidden());
		this.mvc.perform(
				get(BASE + "/MISSING/stats").param("start", "2026-10-01T08:00:00Z").param("end", "2026-10-01T12:00:00Z")
						.with(actor("user", "ROLE_JANUS_ADMIN")))
				.andExpect(status().isNotFound());
	}
}
