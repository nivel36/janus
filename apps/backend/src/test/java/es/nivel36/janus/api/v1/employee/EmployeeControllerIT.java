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
package es.nivel36.janus.api.v1.employee;

import static org.springframework.http.MediaType.APPLICATION_JSON;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.jdbc.core.JdbcTemplate;

import es.nivel36.janus.api.v1.SecurityTestConfiguration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(SecurityTestConfiguration.class)
@Transactional
class EmployeeControllerIT {

	private static final String BASE = "/api/v1/employees";

	private @Autowired MockMvc mvc;
	private @Autowired JdbcTemplate jdbc;

	@BeforeEach
	void provisionActor() {
		this.jdbc.update("""
				INSERT INTO app_user(email,keycloak_subject,locale,time_format,default_timezone)
				VALUES ('mock-actor','user','en-US','H24','UTC')
				""");
	}

	@Test
	@Sql(statements = { "INSERT INTO schedule(id,code,name) VALUES(1,'STD-WH','Standard')",
			"INSERT INTO employee(id,employee_number,name,surname,email,schedule_id) VALUES(10,'EMP-0010','Alice','One','alice@internal.test',1)",
			"INSERT INTO employee(id,employee_number,name,surname,email,schedule_id) VALUES(11,'EMP-0011','Bob','Two','bob@internal.test',1)",
			"INSERT INTO app_user(email,keycloak_subject,locale,time_format,default_timezone,employee_id) VALUES('alice','11111111-1111-4111-8111-111111111111','en-US','H24','UTC',10)",
			"INSERT INTO app_user(email,keycloak_subject,locale,time_format,default_timezone,employee_id) VALUES('bob','22222222-2222-4222-8222-222222222222','en-US','H24','UTC',11)" })
	void employeeCanAccessOwnProfileButNotAnotherEmployeesProfile() throws Exception {
		this.mvc.perform(get(BASE + "/{employeeNumber}", "EMP-0010").with(verifiedJwt()
				.jwt(jwt -> jwt.subject("11111111-1111-4111-8111-111111111111").claim("email", "different@token.test"))
				.authorities(createAuthorityList("ROLE_JANUS_EMPLOYEE")))).andExpect(status().isOk());

		this.mvc.perform(get(BASE + "/{employeeNumber}", "EMP-0011")
				.with(verifiedJwt().jwt(jwt -> jwt.subject("11111111-1111-4111-8111-111111111111"))
						.authorities(createAuthorityList("ROLE_JANUS_EMPLOYEE"))))
				.andExpect(status().isForbidden());
	}

	@Test
	@Sql(statements = { "INSERT INTO schedule(id,code,name) VALUES(1,'STD-WH','Standard')",
			"INSERT INTO employee(id,employee_number,name,surname,email,schedule_id) VALUES(10,'EMP-0010','Alice','One','alice@internal.test',1)",
			"INSERT INTO employee(id,employee_number,name,surname,email,schedule_id) VALUES(11,'EMP-0011','Bob','Two','bob@internal.test',1)",
			"INSERT INTO app_user(email,keycloak_subject,locale,time_format,default_timezone,employee_id) VALUES('alice','11111111-1111-4111-8111-111111111111','en-US','H24','UTC',10)",
			"INSERT INTO app_user(email,keycloak_subject,locale,time_format,default_timezone,employee_id) VALUES('bob','22222222-2222-4222-8222-222222222222','en-US','H24','UTC',11)" })
	void employeeAuthorizationUsesSubjectLinkAndNotEmailClaim() throws Exception {
		// A matching mutable email cannot grant access when the immutable subject
		// belongs to Bob.
		this.mvc.perform(get(BASE + "/{employeeNumber}", "EMP-0010").with(verifiedJwt()
				.jwt(jwt -> jwt.subject("22222222-2222-4222-8222-222222222222").claim("email", "alice@internal.test"))
				.authorities(createAuthorityList("ROLE_JANUS_EMPLOYEE")))).andExpect(status().isForbidden());

		// A nonexistent email produces the same generic denial and cannot be
		// enumerated.
		this.mvc.perform(get(BASE + "/{employeeNumber}", "UNKNOWN")
				.with(verifiedJwt().jwt(jwt -> jwt.subject("11111111-1111-4111-8111-111111111111"))
						.authorities(createAuthorityList("ROLE_JANUS_EMPLOYEE"))))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.detail").value("You are not authorized to perform this operation"));
	}

	@Test
	@Sql(statements = { "INSERT INTO schedule(id,code,name) VALUES(1,'STD-WH','Standard')",
			"INSERT INTO employee(id,employee_number,name,surname,email,schedule_id) VALUES(10,'EMP-0010','Alice','One','alice@internal.test',1)" })
	void employeeWithoutProvisionedLinkReceivesForbidden() throws Exception {
		this.mvc.perform(get(BASE + "/{employeeNumber}", "EMP-0010").with(verifiedJwt()
				.jwt(jwt -> jwt.subject("33333333-3333-4333-8333-333333333333").claim("email", "alice@internal.test"))
				.authorities(createAuthorityList("ROLE_JANUS_EMPLOYEE")))).andExpect(status().isForbidden());
	}

	@Test
	@Sql(statements = { //
			"INSERT INTO schedule(id,code,name) VALUES(1,'STD-WH', 'Standard Work Hours')", //
			"INSERT INTO employee(employee_number,name,surname,email,schedule_id) VALUES('EMP-0001','Abel','Ferrer','aferrer@nivel36.es',1)" //
	})
	void testFindByEmailShouldReturn200() throws Exception {
		this.mvc.perform(get(BASE + "/{employeeNumber}", "EMP-0001").with(verifiedJwt()//
				.authorities(createAuthorityList("ROLE_JANUS_ADMIN")))) //
				.andExpect(status().isOk()) //
				.andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON)) //
				.andExpect(jsonPath("$.name").value("Abel")) //
				.andExpect(jsonPath("$.surname").value("Ferrer")) //
				.andExpect(jsonPath("$.employeeNumber").value("EMP-0001")) //
				.andExpect(jsonPath("$.email").value("aferrer@nivel36.es")) //
				.andExpect(jsonPath("$.scheduleCode").value("STD-WH"));
	}

	@Test
	@Sql(statements = { //
			"INSERT INTO schedule(id,code,name) VALUES(1,'STD-WH', 'Standard Work Hours')", //
			"INSERT INTO employee(employee_number,name,surname,email,schedule_id) VALUES('EMP-0001','Abel','Ferrer','aferrer@nivel36.es',1)" //
	})
	void testElevatedRolesWithoutVerifiedEmailAreUnauthorized() throws Exception {
		this.mvc.perform(get(BASE + "/{employeeNumber}", "EMP-0001").header("Authorization", "Bearer email-unverified")) //
				.andExpect(status().isUnauthorized());
	}

	@Test
	void testFindByUnknownEmailShouldReturn404() throws Exception {
		this.mvc.perform(get(BASE + "/{employeeNumber}", "EMP-0001").with(verifiedJwt()//
				.authorities(createAuthorityList("ROLE_JANUS_ADMIN")))) //
				.andExpect(status().isNotFound());
	}

	@Test
	void testFindByInvalidEmailShouldReturn400() throws Exception {
		this.mvc.perform(get(BASE + "/{employeeNumber}", "not valid!").with(verifiedJwt()//
				.authorities(createAuthorityList("ROLE_JANUS_ADMIN")))) //
				.andExpect(status().isBadRequest());
	}

	@Test
	void testEmployeeWithScopeCannotFindAnotherEmployee() throws Exception {
		this.mvc.perform(get(BASE + "/{employeeNumber}", "EMP-0001").with(verifiedJwt() //
				.jwt(jwt -> jwt.subject("user").claim("email", "employee@nivel36.es")
						.claim("email_verified", true)) //
				.authorities(createAuthorityList("ROLE_JANUS_EMPLOYEE", "SCOPE_read")))) //
				.andExpect(status().isForbidden());
	}

	@Test
	void testEmployeeWithUnverifiedEmailCannotAccessEmployeeResource() throws Exception {
		this.mvc.perform(get(BASE + "/{employeeNumber}", "EMP-0001").with(verifiedJwt() //
				.jwt(jwt -> jwt.claim("email", "employee@nivel36.es").claim("email_verified", false)) //
				.authorities(createAuthorityList("ROLE_JANUS_EMPLOYEE")))) //
				.andExpect(status().isForbidden());
	}

	@Test
	void testEmployeeWithUnknownRoleCannotFindAnotherEmployee() throws Exception {
		this.mvc.perform(get(BASE + "/{employeeNumber}", "EMP-0001").with(verifiedJwt() //
				.jwt(jwt -> jwt.subject("user").claim("email", "employee@nivel36.es")
						.claim("email_verified", true)) //
				.authorities(createAuthorityList("ROLE_JANUS_EMPLOYEE", "ROLE_UNKNOWN")))) //
				.andExpect(status().isForbidden());
	}

	@Test
	@Sql(statements = { //
			"INSERT INTO schedule(id,code,name) VALUES(1,'STD-WH', 'Standard Work Hours')", //
			"INSERT INTO employee(employee_number,name,surname,email,schedule_id) VALUES('EMP-0001','Other','Employee','other@nivel36.es',1)" //
	})
	void testEmployeeWithUserRoleCanFindAnotherEmployee() throws Exception {
		this.mvc.perform(get(BASE + "/{employeeNumber}", "EMP-0001").with(verifiedJwt() //
				.jwt(jwt -> jwt.subject("user").claim("email", "employee@nivel36.es")
						.claim("email_verified", true)) //
				.authorities(createAuthorityList("ROLE_JANUS_EMPLOYEE", "ROLE_JANUS_USER")))) //
				.andExpect(status().isOk()) //
				.andExpect(jsonPath("$.email").value("other@nivel36.es"));
	}

	@Test
	@Sql(statements = { //
			"INSERT INTO schedule(id,code,name) VALUES(1,'STD-WH', 'Standard Work Hours')", //
			"INSERT INTO employee(employee_number,name,surname,email,schedule_id) VALUES('EMP-0001','Other','Employee','other@nivel36.es',1)" //
	})
	void testEmployeeWithAdminRoleCanFindAnotherEmployee() throws Exception {
		this.mvc.perform(get(BASE + "/{employeeNumber}", "EMP-0001").with(verifiedJwt() //
				.jwt(jwt -> jwt.subject("user").claim("email", "employee@nivel36.es")
						.claim("email_verified", true)) //
				.authorities(createAuthorityList("ROLE_JANUS_EMPLOYEE", "ROLE_JANUS_ADMIN")))) //
				.andExpect(status().isOk()) //
				.andExpect(jsonPath("$.email").value("other@nivel36.es"));
	}

	@Test
	@Sql(statements = { //
			"INSERT INTO schedule(id,code,name) VALUES(1,'STD-WH', 'Standard Work Hours')" //
	})
	void testCreateShouldReturn201AndPersist() throws Exception {
		final String body = """
				{"employeeNumber":"EMP-0001","name":"Abel","surname":"Ferrer","email":"aferrer@nivel36.es","scheduleCode":"STD-WH"}
				""";
		this.mvc.perform(post(BASE).contentType(APPLICATION_JSON).content(body).with(verifiedJwt()//
				.authorities(createAuthorityList("ROLE_JANUS_ADMIN")))) //
				.andExpect(status().isCreated()) //
				.andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON)) //
				.andExpect(jsonPath("$.name").value("Abel")) //
				.andExpect(jsonPath("$.surname").value("Ferrer")) //
				.andExpect(jsonPath("$.email").value("aferrer@nivel36.es")) //
				.andExpect(jsonPath("$.scheduleCode").value("STD-WH"));

		this.mvc.perform(get(BASE + "/{employeeNumber}", "EMP-0001").with(verifiedJwt() //
				.authorities(createAuthorityList("ROLE_JANUS_ADMIN")))) //
				.andExpect(status().isOk()) //
				.andExpect(jsonPath("$.email").value("aferrer@nivel36.es"));
	}

	@Test
	@Sql(statements = { //
			"INSERT INTO schedule(id,code,name) VALUES(1,'STD-WH', 'Standard Work Hours')", //
			"INSERT INTO employee(employee_number,name,surname,email,schedule_id) VALUES('EMP-0001','Abel','Ferrer','aferrer@nivel36.es',1)" //
	})
	void testCreateAlreadyExistsShouldReturn400() throws Exception {
		final String body = """
				{"employeeNumber":"EMP-0002","name":"Abel","surname":"Ferrer","email":"AFERRER@NIVEL36.ES","scheduleCode":"STD-WH"}
				""";
		this.mvc.perform(post(BASE).contentType(APPLICATION_JSON).content(body).with(verifiedJwt()//
				.authorities(createAuthorityList("ROLE_JANUS_ADMIN")))) //
				.andExpect(status().isBadRequest());
	}

	@Test
	void testCreateInvalidPayloadShouldReturn400() throws Exception {
		final String body = """
				{"employeeNumber":"", "name":"", "surname":"", "email":"bad", "scheduleCode":null}
				""";
		this.mvc.perform(post(BASE).contentType(APPLICATION_JSON).content(body).with(verifiedJwt())) //
				.andExpect(status().isBadRequest());
	}

	@Test
	@Sql(statements = { //
			"INSERT INTO schedule(id,code,name) VALUES(1,'STD-WH', 'Standard Work Hours')", //
			"INSERT INTO schedule(id,code,name) VALUES(2,'STD-WH-AUG-VAR','Standard Work Hours with August Variation')",
			"INSERT INTO employee(employee_number,name,surname,email,schedule_id) VALUES('EMP-0001','Abel','Ferrer','aferrer@nivel36.es',1)" //
	})
	void testUpdateShouldReturn200AndUpdatedBody() throws Exception {
		final String body = """
				{"name":"Abel","surname":"Ferrer Jiménez","email":"new@nivel36.es","scheduleCode":"STD-WH"}
				""";
		this.mvc.perform(put(BASE + "/{employeeNumber}", "EMP-0001").contentType(APPLICATION_JSON)
				.content(body).with(verifiedJwt() //
						.authorities(createAuthorityList("ROLE_JANUS_ADMIN")))) //
				.andExpect(status().isOk()) //
				.andExpect(jsonPath("$.email").value("new@nivel36.es")) //
				.andExpect(jsonPath("$.name").value("Abel")) //
				.andExpect(jsonPath("$.surname").value("Ferrer Jiménez")) //
				.andExpect(jsonPath("$.scheduleCode").value("STD-WH"));
	}

	@Test
	@Sql(statements = {
			"INSERT INTO schedule(id,code,name) VALUES(1,'STD-WH','Standard')",
			"INSERT INTO employee(id,employee_number,name,surname,email,schedule_id) VALUES(10,'EMP-0010','Alice','One','alice@internal.test',1)",
			"INSERT INTO worksite(id,code,name,time_zone,scope) VALUES(20,'HQ','Headquarters','UTC','ASSIGNED')",
			"INSERT INTO employee_worksite(employee_id,worksite_id) VALUES(10,20)",
			"INSERT INTO work_shift(id,employee_id,date,total_pause_time,total_work_time) VALUES(30,10,'2026-09-20',0,28800)",
			"INSERT INTO time_log(id,employee_id,worksite_id,workshift_id,entry_time,exit_time) VALUES(40,10,20,30,'2026-09-20T08:00:00Z','2026-09-20T16:00:00Z')",
			"INSERT INTO app_user(email,keycloak_subject,locale,time_format,default_timezone,employee_id) VALUES('alice-login@internal.test','33333333-3333-4333-8333-333333333333','en-US','H24','UTC',10)" })
	void changingEmailPreservesAllEmployeeRelationships() throws Exception {
		final String body = """
				{"name":"Alice","surname":"One","email":"alice.new@internal.test","scheduleCode":"STD-WH"}
				""";

		this.mvc.perform(put(BASE + "/{employeeNumber}", "EMP-0010").contentType(APPLICATION_JSON).content(body)
				.with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_ADMIN"))))
				.andExpect(status().isOk()).andExpect(jsonPath("$.email").value("alice.new@internal.test"));

		assertRelationshipCount("SELECT COUNT(*) FROM time_log WHERE employee_id=10", 1);
		assertRelationshipCount("SELECT COUNT(*) FROM work_shift WHERE employee_id=10", 1);
		assertRelationshipCount("SELECT COUNT(*) FROM employee WHERE id=10 AND schedule_id=1", 1);
		assertRelationshipCount("SELECT COUNT(*) FROM employee_worksite WHERE employee_id=10 AND worksite_id=20", 1);
		assertRelationshipCount("SELECT COUNT(*) FROM app_user WHERE employee_id=10", 1);
	}

	private void assertRelationshipCount(final String sql, final int expected) {
		org.assertj.core.api.Assertions.assertThat(this.jdbc.queryForObject(sql, Integer.class)).isEqualTo(expected);
	}

	@Test
	void testEmployeeWithScopeCannotUpdateAnotherEmployee() throws Exception {
		final String body = """
				{"name":"Other","surname":"Employee","email":"other@nivel36.es","scheduleCode":"STD-WH"}
				""";
		this.mvc.perform(put(BASE + "/{employeeNumber}", "EMP-0001").contentType(APPLICATION_JSON).content(body)
				.with(verifiedJwt()
						.jwt(jwt -> jwt.subject("user").claim("email", "employee@nivel36.es")
								.claim("email_verified", true)) //
						.authorities(createAuthorityList("ROLE_JANUS_EMPLOYEE", "SCOPE_read")))) //
				.andExpect(status().isForbidden());
	}

	@Test
	void testEmployeeWithUnknownRoleCannotUpdateAnotherEmployee() throws Exception {
		final String body = """
				{"name":"Other","surname":"Employee","email":"other@nivel36.es","scheduleCode":"STD-WH"}
				""";
		this.mvc.perform(put(BASE + "/{employeeNumber}", "EMP-0001").contentType(APPLICATION_JSON).content(body)
				.with(verifiedJwt()
						.jwt(jwt -> jwt.subject("user").claim("email", "employee@nivel36.es")
								.claim("email_verified", true)) //
						.authorities(createAuthorityList("ROLE_JANUS_EMPLOYEE", "ROLE_UNKNOWN")))) //
				.andExpect(status().isForbidden());
	}

	@Test
	@Sql(statements = { //
			"INSERT INTO schedule(id,code,name) VALUES(1,'STD-WH', 'Standard Work Hours')", //
			"INSERT INTO employee(employee_number,name,surname,email,schedule_id) VALUES('EMP-0001','Abel','Ferrer','aferrer@nivel36.es',1)" //
	})
	void testDeleteShouldReturn204AndDisappearFromFind() throws Exception {
		this.mvc.perform(delete(BASE + "/{employeeNumber}", "EMP-0001").with(verifiedJwt()//
				.authorities(createAuthorityList("ROLE_JANUS_ADMIN")))) //
				.andExpect(status().isNoContent());

		this.mvc.perform(get(BASE + "/{employeeNumber}", "EMP-0001").with(verifiedJwt()//
				.authorities(createAuthorityList("ROLE_JANUS_ADMIN"))))//
				.andExpect(status().isNotFound());
	}
}
