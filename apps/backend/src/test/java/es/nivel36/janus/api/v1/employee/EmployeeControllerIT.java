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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class EmployeeControllerIT {

	private static final String BASE = "/api/v1/employees";

	private @Autowired MockMvc mvc;

	@Test
	@Sql(statements = { //
			"INSERT INTO schedule(id,code,name) VALUES(1,'STD-WH', 'Standard Work Hours')", //
			"INSERT INTO employee(name,surname,email,schedule_id) VALUES('Abel','Ferrer','aferrer@nivel36.es',1)" //
	})
	void testFindByEmailShouldReturn200() throws Exception {
		mvc.perform(get(BASE + "/by-email/{email}", "aferrer@nivel36.es")) //
				.andExpect(status().isOk()) //
				.andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON)) //
				.andExpect(jsonPath("$.name").value("Abel")) //
				.andExpect(jsonPath("$.surname").value("Ferrer")) //
				.andExpect(jsonPath("$.email").value("aferrer@nivel36.es")) //
				.andExpect(jsonPath("$.scheduleCode").value("STD-WH"));
	}

	@Test
	void testFindByUnknownEmailShouldReturn404() throws Exception {
		mvc.perform(get(BASE + "/by-email/{email}", "aferrer@nivel36.es")) //
				.andExpect(status().isNotFound());
	}

	@Test
	void testFindByInvalidEmailShouldReturn400() throws Exception {
		mvc.perform(get(BASE + "/by-email/{email}", "not-an-email")) //
				.andExpect(status().isBadRequest());
	}

	@Test
	@Sql(statements = { //
			"INSERT INTO schedule(id,code,name) VALUES(1,'STD-WH', 'Standard Work Hours')" //
	})
	void testCreateShouldReturn201AndPersist() throws Exception {
		String body = """
				{"name":"Abel","surname":"Ferrer","email":"aferrer@nivel36.es","scheduleCode":"STD-WH"}
				""";
		mvc.perform(post(BASE).contentType(APPLICATION_JSON).content(body)) //
				.andExpect(status().isCreated()) //
				.andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON)) //
				.andExpect(jsonPath("$.name").value("Abel")) //
				.andExpect(jsonPath("$.surname").value("Ferrer")) //
				.andExpect(jsonPath("$.email").value("aferrer@nivel36.es")) //
				.andExpect(jsonPath("$.scheduleCode").value("STD-WH"));

		mvc.perform(get(BASE + "/by-email/{email}", "aferrer@nivel36.es")) //
				.andExpect(status().isOk()) //
				.andExpect(jsonPath("$.email").value("aferrer@nivel36.es"));
	}

	@Test
	@Sql(statements = { //
			"INSERT INTO schedule(id,code,name) VALUES(1,'STD-WH', 'Standard Work Hours')", //
			"INSERT INTO employee(name,surname,email,schedule_id) VALUES('Abel','Ferrer','aferrer@nivel36.es',1)" //
	})
	void testCreateAlreadyExistsShouldReturn400() throws Exception {
		String body = """
				{"name":"Abel","surname":"Ferrer","email":"aferrer@nivel36.es","scheduleCode":"STD-WH"}
				""";
		mvc.perform(post(BASE).contentType(APPLICATION_JSON).content(body)) //
				.andExpect(status().isBadRequest());
	}

	@Test
	void testCreateInvalidPayloadShouldReturn400() throws Exception {
		String body = """
				{"name":"", "surname":"", "email":"bad", "scheduleCode":null}
				""";
		mvc.perform(post(BASE).contentType(APPLICATION_JSON).content(body)) //
				.andExpect(status().isBadRequest());
	}

	@Test
	@Sql(statements = { //
			"INSERT INTO schedule(id,code,name) VALUES(1,'STD-WH', 'Standard Work Hours')", //
			"INSERT INTO schedule(id,code,name) VALUES(2,'STD-WH-AUG-VAR','Standard Work Hours with August Variation')",
			"INSERT INTO employee(name,surname,email,schedule_id) VALUES('Abel','Ferrer','aferrer@nivel36.es',1)" //
	})
	void testUpdateShouldReturn200AndUpdatedBody() throws Exception {
		String body = """
				{"name":"Abel","surname":"Ferrer Jiménez","scheduleCode":"STD-WH"}
				""";
		mvc.perform(put(BASE + "/{employeeEmail}", "aferrer@nivel36.es").contentType(APPLICATION_JSON).content(body)) //
				.andExpect(status().isOk()) //
				.andExpect(jsonPath("$.email").value("aferrer@nivel36.es")) //
				.andExpect(jsonPath("$.name").value("Abel")) //
				.andExpect(jsonPath("$.surname").value("Ferrer Jiménez")) //
				.andExpect(jsonPath("$.scheduleCode").value("STD-WH"));
	}

	@Test
	@Sql(statements = { //
			"INSERT INTO schedule(id,code,name) VALUES(1,'STD-WH', 'Standard Work Hours')", //
			"INSERT INTO employee(id,name,surname,email,schedule_id) VALUES(1,'Abel','Ferrer','aferrer@nivel36.es',1)", //
			"INSERT INTO worksite(id,code,name,time_zone) VALUES(1,'BCN-HQ','Barcelona Headquarters','UTC+2')", //
			"INSERT INTO employee_worksite(employee_id,worksite_id) VALUES(1,1)" //
	})
	void testAddExistingWorksiteShouldReturn200() throws Exception {
		mvc.perform(post(BASE + "/{employeeEmail}/worksites/{worksiteCode}", "aferrer@nivel36.es", "BCN-HQ")) //
				.andExpect(status().isOk()) //
				.andExpect(jsonPath("$.email").value("aferrer@nivel36.es"));
	}

	@Test
	@Sql(statements = { //
			"INSERT INTO schedule(id,code,name) VALUES(1,'STD-WH', 'Standard Work Hours')", //
			"INSERT INTO employee(name,surname,email,schedule_id) VALUES('Abel','Ferrer','aferrer@nivel36.es',1)", //
			"INSERT INTO worksite(id,code,name,time_zone) VALUES(1,'BCN-HQ','Barcelona Headquarters','UTC+2')" //
	})
	void testAddWorksiteShouldReturn200AndBody() throws Exception {
		mvc.perform(post(BASE + "/{employeeEmail}/worksites/{worksiteCode}", "aferrer@nivel36.es", "BCN-HQ")) //
				.andExpect(status().isOk()) //
				.andExpect(jsonPath("$.email").value("aferrer@nivel36.es"));
	}

	@Test
	@Sql(statements = { //
			"INSERT INTO schedule(id,code,name) VALUES(1,'STD-WH', 'Standard Work Hours')", //
			"INSERT INTO employee(name,surname,email,schedule_id) VALUES('Abel','Ferrer','aferrer@nivel36.es',1)", //
			"INSERT INTO worksite(id,code,name,time_zone) VALUES(1,'BCN-HQ','Barcelona Headquarters','UTC+2')" //
	})
	void testRemoveUnassignedWorksiteShouldReturn200AndBody() throws Exception {
		mvc.perform(delete(BASE + "/{employeeEmail}/worksites/{worksiteCode}", "aferrer@nivel36.es", "BCN-HQ")) //
				.andExpect(status().isOk()) //
				.andExpect(jsonPath("$.email").value("aferrer@nivel36.es"));
	}

	@Test
	@Sql(statements = { //
			"INSERT INTO schedule(id,code,name) VALUES(1,'STD-WH', 'Standard Work Hours')", //
			"INSERT INTO employee(id,name,surname,email,schedule_id) VALUES(1,'Abel','Ferrer','aferrer@nivel36.es',1)", //
			"INSERT INTO worksite(id,code,name,time_zone) VALUES(1,'BCN-HQ','Barcelona Headquarters','UTC+2')", //
			"INSERT INTO employee_worksite(employee_id,worksite_id) VALUES(1,1)" //
	})
	void testRemoveWorksiteShouldReturn200AndBody() throws Exception {
		mvc.perform(delete(BASE + "/{employeeEmail}/worksites/{worksiteCode}", "aferrer@nivel36.es", "BCN-HQ")) //
				.andExpect(status().isOk()) //
				.andExpect(jsonPath("$.email").value("aferrer@nivel36.es"));
	}

	@Test
	@Sql(statements = { //
			"INSERT INTO schedule(id,code,name) VALUES(1,'STD-WH', 'Standard Work Hours')", //
			"INSERT INTO employee(name,surname,email,schedule_id) VALUES('Abel','Ferrer','aferrer@nivel36.es',1)" //
	})
	void testDeleteShouldReturn204AndDisappearFromFind() throws Exception {
		mvc.perform(delete(BASE + "/{employeeEmail}", "aferrer@nivel36.es")) //
				.andExpect(status().isNoContent());

		mvc.perform(get(BASE + "/by-email/{email}", "aferrer@nivel36.es")) //
				.andExpect(status().isNotFound());
	}
}
