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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;

import es.nivel36.janus.api.v1.SecurityTestConfiguration;

/**
 * Exercises committed worksites with open-in-view disabled and no test
 * transaction.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = "spring.data.rest.max-page-size=7")
@AutoConfigureMockMvc
@Import(SecurityTestConfiguration.class)
class WorksiteFlowIT {
	private static final String BASE = "/api/v1/worksites";
	private @Autowired MockMvc mvc;
	private @Autowired JdbcTemplate jdbc;

	@BeforeEach
	void createResources() {
		this.jdbc.update("INSERT INTO schedule(id,code,name) VALUES(92001,'WORKSITE-FLOW','Worksite flow')");
		this.jdbc.update(
				"INSERT INTO employee(id,employee_number,name,surname,email,schedule_id) VALUES(92001,'WS-FLOW-EMP','Flow','Employee','worksite-flow@example.test',92001)");
		this.jdbc.update(
				"INSERT INTO app_user(email,keycloak_subject,locale,time_format,default_timezone,employee_id) VALUES('worksite-flow-admin@example.test','worksite-flow-admin','en-US','H24','UTC',NULL)");
		this.jdbc.update(
				"INSERT INTO app_user(email,keycloak_subject,locale,time_format,default_timezone,employee_id) VALUES('worksite-flow-employee@example.test','worksite-flow-employee','en-US','H24','UTC',92001)");
		this.jdbc.update(
				"INSERT INTO worksite(id,code,name,time_zone,scope,description,address) VALUES(92001,'FLOW-ASSIGNED','Assigned depot','UTC','ASSIGNED','Old description','Old address')");
		this.jdbc.update(
				"INSERT INTO worksite(id,code,name,time_zone,scope,description,address) VALUES(92002,'FLOW-GLOBAL','Global depot','UTC','GLOBAL','Old description','Old address')");
	}

	@AfterEach
	void removeResources() {
		this.jdbc.update("DELETE FROM employee_worksite WHERE employee_id=92001");
		this.jdbc.update("DELETE FROM worksite WHERE code LIKE 'FLOW-%'");
		this.jdbc.update("DELETE FROM app_user WHERE keycloak_subject LIKE 'worksite-flow-%'");
		this.jdbc.update("DELETE FROM employee WHERE id=92001");
		this.jdbc.update("DELETE FROM schedule WHERE id=92001");
	}

	private static JwtRequestPostProcessor admin() {
		return verifiedJwt().jwt(jwt -> jwt.subject("worksite-flow-admin"))
				.authorities(createAuthorityList("ROLE_JANUS_ADMIN"));
	}

	private static JwtRequestPostProcessor employee() {
		return verifiedJwt().jwt(jwt -> jwt.subject("worksite-flow-employee"))
				.authorities(createAuthorityList("ROLE_JANUS_EMPLOYEE"));
	}

	@Test
	void assignmentAndRemovalCommitAndChangeEmployeeVisibilityAcrossRequests() throws Exception {
		this.mvc.perform(get(BASE + "/FLOW-ASSIGNED").with(employee())).andExpect(status().isForbidden());
		for (int attempt = 0; attempt < 2; attempt++) {
			this.mvc.perform(put(BASE + "/FLOW-ASSIGNED/employees/WS-FLOW-EMP").with(admin()))
					.andExpect(status().isNoContent());
		}
		assertThat(
				this.jdbc.queryForObject(
						"SELECT COUNT(*) FROM employee_worksite WHERE employee_id=92001 AND worksite_id=92001",
						Integer.class))
				.isOne();
		this.mvc.perform(get(BASE + "/FLOW-ASSIGNED").with(employee())).andExpect(status().isOk());
		this.mvc.perform(delete(BASE + "/FLOW-ASSIGNED").with(admin())).andExpect(status().isConflict());
		assertThat(this.jdbc.queryForObject("SELECT deleted FROM worksite WHERE id=92001", Boolean.class)).isFalse();
		for (int attempt = 0; attempt < 2; attempt++) {
			this.mvc.perform(delete(BASE + "/FLOW-ASSIGNED/employees/WS-FLOW-EMP").with(admin()))
					.andExpect(status().isNoContent());
		}
		assertThat(
				this.jdbc.queryForObject(
						"SELECT COUNT(*) FROM employee_worksite WHERE employee_id=92001 AND worksite_id=92001",
						Integer.class))
				.isZero();
		this.mvc.perform(get(BASE + "/FLOW-ASSIGNED").with(employee())).andExpect(status().isForbidden());
		this.mvc.perform(delete(BASE + "/FLOW-ASSIGNED").with(admin())).andExpect(status().isNoContent());
		assertThat(this.jdbc.queryForObject("SELECT deleted FROM worksite WHERE id=92001", Boolean.class)).isTrue();
	}

	@Test
	void rejectedScopeTransitionRollsBackDescriptiveChanges() throws Exception {
		this.mvc.perform(
				put(BASE + "/FLOW-GLOBAL").with(admin()).contentType(APPLICATION_JSON).content(
						"""
								{"name":"Changed","timeZone":"Europe/Madrid","scope":"ASSIGNED","description":"Changed","address":"Changed"}
								"""))
				.andExpect(status().isBadRequest());
		assertThat(this.jdbc.queryForObject("SELECT name FROM worksite WHERE id=92002", String.class))
				.isEqualTo("Global depot");
		assertThat(this.jdbc.queryForObject("SELECT time_zone FROM worksite WHERE id=92002", String.class))
				.isEqualTo("UTC");
		assertThat(this.jdbc.queryForObject("SELECT description FROM worksite WHERE id=92002", String.class))
				.isEqualTo("Old description");
		assertThat(this.jdbc.queryForObject("SELECT address FROM worksite WHERE id=92002", String.class))
				.isEqualTo("Old address");
	}

	@Test
	void creationAndUpdateCommitTrimmedTextAndClearOptionalFields() throws Exception {
		this.mvc.perform(
				post(BASE).with(admin()).contentType(APPLICATION_JSON).content(
						"""
								{"code":"FLOW-CREATED","name":" New depot ","timeZone":"UTC","scope":"ASSIGNED","description":" Description ","address":" Address "}
								"""))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.name").value("New depot"))
				.andExpect(jsonPath("$.description").value("Description"))
				.andExpect(jsonPath("$.address").value("Address"));
		this.mvc.perform(put(BASE + "/FLOW-CREATED").with(admin()).contentType(APPLICATION_JSON).content("""
				{"name":"Updated depot","timeZone":"Europe/Madrid","scope":"GLOBAL"}
				""")).andExpect(status().isOk());
		this.mvc.perform(get(BASE + "/FLOW-CREATED").with(admin())).andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Updated depot")).andExpect(jsonPath("$.scope").value("GLOBAL"))
				.andExpect(jsonPath("$.timeZone").value("Europe/Madrid"))
				.andExpect(jsonPath("$.description").doesNotExist()).andExpect(jsonPath("$.address").doesNotExist());
		assertThat(this.jdbc.queryForObject("SELECT description FROM worksite WHERE code='FLOW-CREATED'", String.class))
				.isNull();
		assertThat(this.jdbc.queryForObject("SELECT address FROM worksite WHERE code='FLOW-CREATED'", String.class))
				.isNull();
	}

	@Test
	void searchHonorsConfiguredPageSizeLimit() throws Exception {
		this.mvc.perform(get(BASE).param("size", "1000").with(admin())).andExpect(status().isOk())
				.andExpect(jsonPath("$.page.size").value(7)).andExpect(jsonPath("$.page.totalElements").value(2));
	}
}
