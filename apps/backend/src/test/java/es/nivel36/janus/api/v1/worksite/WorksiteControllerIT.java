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
package es.nivel36.janus.api.v1.worksite;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
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

import es.nivel36.janus.api.v1.SecurityTestConfiguration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(SecurityTestConfiguration.class)
@Transactional
class WorksiteControllerIT {

	private @Autowired MockMvc mvc;

	private static final String BASE = "/api/v1/worksites";

	@Test
	@Sql(statements = {
			"INSERT INTO application_settings (days_until_locked, employee_workplace_creation_allowed, worksite_change_during_shift_allowed) VALUES (7, true, false)",
			"INSERT INTO worksite(code,name,time_zone,scope) VALUES('BCN-HQ','Barcelona Headquarters','UTC+2','GLOBAL')"
	})
	void testListShouldReturnSeededWorksite() throws Exception {
		mvc.perform(get(BASE).with(jwt()))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
				.andExpect(jsonPath("$[?(@.code=='BCN-HQ')]").exists())
				.andExpect(jsonPath("$[?(@.code=='BCN-HQ' && @.scope=='GLOBAL')]").exists());
	}

	@Test
	@Sql(statements = {
			"INSERT INTO application_settings (days_until_locked, employee_workplace_creation_allowed, worksite_change_during_shift_allowed) VALUES (7, true, false)",
			"INSERT INTO worksite(code,name,time_zone,scope) VALUES('BCN-HQ','Barcelona Headquarters','UTC+2','GLOBAL')"
	})
	void testFindByCodeShouldReturnWorksite() throws Exception {
		mvc.perform(get(BASE + "/{code}", "BCN-HQ").with(jwt())).andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
				.andExpect(jsonPath("$.code").value("BCN-HQ"))
				.andExpect(jsonPath("$.name").value("Barcelona Headquarters"))
				.andExpect(jsonPath("$.timeZone").value("UTC+02:00"))
				.andExpect(jsonPath("$.scope").value("GLOBAL"))
				.andExpect(jsonPath("$.ownerEmployeeEmail").isEmpty());
	}

	@Test
	void testFindByUnknownCodeShouldReturn404() throws Exception {
		mvc.perform(get(BASE + "/{code}", "BCN-HQ").with(jwt())).andExpect(status().isNotFound());
	}

	@Test
	void testFindByCodeWithInvalidPatternShouldFail400() throws Exception {
		mvc.perform(get(BASE + "/{code}", "BAD CODE WITH SPACE").with(jwt())).andExpect(status().isBadRequest());
	}

	@Test
	@Sql(statements = {
			"INSERT INTO application_settings (days_until_locked, employee_workplace_creation_allowed, worksite_change_during_shift_allowed) VALUES (7, true, false)",
			"INSERT INTO worksite(code,name,time_zone,scope) VALUES('BCN-HQ','Barcelona Headquarters','UTC+2','GLOBAL')"
	})
	void testCreateAlreadyExistsShouldReturn400() throws Exception {
		String code = "BCN-HQ";
		String body = """
				  {"code":"%s","name":"Barcelona Headquarters","timeZone":"Europe/Madrid","scope":"GLOBAL"}
				""".formatted(code);

		mvc.perform(post(BASE).contentType(APPLICATION_JSON).content(body).with(jwt()))
				.andExpect(status().isBadRequest());
	}

	@Test
	void testCreateShouldReturn201AndBody() throws Exception {
		String code = "MAD-HUB";
		String body = """
				  {"code":"%s","name":"Madrid Hub","timeZone":"Europe/Madrid","scope":"GLOBAL"}
				""".formatted(code);

		mvc.perform(post(BASE).contentType(APPLICATION_JSON).content(body).with(jwt()))
				.andExpect(status().isCreated())
				.andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
				.andExpect(jsonPath("$.code").value(code))
				.andExpect(jsonPath("$.name").value("Madrid Hub"))
				.andExpect(jsonPath("$.timeZone").value("Europe/Madrid"))
				.andExpect(jsonPath("$.scope").value("GLOBAL"))
				.andExpect(jsonPath("$.ownerEmployeeEmail").isEmpty());

		mvc.perform(get(BASE).with(jwt())).andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.code=='%s')]".formatted(code)).exists());
	}

	@Test
	@Sql(statements = {
			"INSERT INTO application_settings (days_until_locked, employee_workplace_creation_allowed, worksite_change_during_shift_allowed) VALUES (7, true, false)",
			"INSERT INTO schedule(id,code,name) VALUES(1,'STD-WH', 'Standard Work Hours')",
			"INSERT INTO employee(id,name,surname,email,schedule_id) VALUES(1,'Abel','Ferrer','aferrer@nivel36.es',1)"
	})
	void testCreatePersonalShouldReturn201AndBody() throws Exception {
		String body = """
				  {"code":"ABEL-HOME","name":"Home Office","timeZone":"Europe/Madrid","scope":"PERSONAL","ownerEmployeeEmail":"aferrer@nivel36.es"}
				""";

		mvc.perform(post(BASE).contentType(APPLICATION_JSON).content(body).with(jwt()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.code").value("ABEL-HOME"))
				.andExpect(jsonPath("$.scope").value("PERSONAL"))
				.andExpect(jsonPath("$.ownerEmployeeEmail").value("aferrer@nivel36.es"));
	}

	@Test
	@Sql(statements = {
			"INSERT INTO application_settings (days_until_locked, employee_workplace_creation_allowed, worksite_change_during_shift_allowed) VALUES (7, true, false)",
			"INSERT INTO worksite(code,name,time_zone,scope) VALUES('BCN-HQ','Barcelona Headquarters','UTC+2','GLOBAL')"
	})
	void testUpdateShouldReturn200AndUpdatedBody() throws Exception {
		String body = """
				  {"name":"Barcelona","timeZone":"UTC+1","scope":"GLOBAL"}
				""";

		mvc.perform(put(BASE + "/{code}", "BCN-HQ").contentType(APPLICATION_JSON).content(body).with(jwt()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value("BCN-HQ"))
				.andExpect(jsonPath("$.name").value("Barcelona"))
				.andExpect(jsonPath("$.timeZone").value("UTC+01:00"))
				.andExpect(jsonPath("$.scope").value("GLOBAL"));
	}

	@Test
	@Sql(statements = {
			"INSERT INTO application_settings (days_until_locked, employee_workplace_creation_allowed, worksite_change_during_shift_allowed) VALUES (7, true, false)",
			"INSERT INTO schedule(id,code,name) VALUES(1,'STD-WH', 'Standard Work Hours')",
			"INSERT INTO employee(id,name,surname,email,schedule_id) VALUES(1,'Abel','Ferrer','aferrer@nivel36.es',1)",
			"INSERT INTO worksite(code,name,time_zone,scope) VALUES('BCN-HQ','Barcelona Headquarters','UTC+2','GLOBAL')"
	})
	void testUpdateToPersonalShouldReturn200AndUpdatedBody() throws Exception {
		String body = """
				  {"name":"Barcelona Home","timeZone":"UTC+1","scope":"PERSONAL","ownerEmployeeEmail":"aferrer@nivel36.es"}
				""";

		mvc.perform(put(BASE + "/{code}", "BCN-HQ").contentType(APPLICATION_JSON).content(body).with(jwt()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.scope").value("PERSONAL"))
				.andExpect(jsonPath("$.ownerEmployeeEmail").value("aferrer@nivel36.es"));
	}

	@Test
	@Sql(statements = {
			"INSERT INTO application_settings (days_until_locked, employee_workplace_creation_allowed, worksite_change_during_shift_allowed) VALUES (7, true, false)",
			"INSERT INTO worksite(code,name,time_zone,scope) VALUES('BCN-HQ','Barcelona Headquarters','UTC+2','GLOBAL')"
	})
	void testDeleteShouldReturn204AndRemoveFromList() throws Exception {
		mvc.perform(delete(BASE + "/{code}", "BCN-HQ").with(jwt()))
				.andExpect(status().isNoContent());

		mvc.perform(get(BASE).with(jwt()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.code=='BCN-HQ')]").doesNotExist());
	}
}
