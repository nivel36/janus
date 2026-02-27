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
	@Sql(statements = { //
			"INSERT INTO worksite(code,name,time_zone) VALUES('BCN-HQ','Barcelona Headquarters','UTC+2')"//
	})
	void testListShouldReturnSeededWorksite() throws Exception {
		mvc.perform(get(BASE).with(jwt())) //
				.andExpect(status().isOk()) //
				.andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON)) //
				.andExpect(jsonPath("$[?(@.code=='BCN-HQ')]").exists());
	}

	@Test
	@Sql(statements = { //
			"INSERT INTO worksite(code,name,time_zone) VALUES('BCN-HQ','Barcelona Headquarters','UTC+2')"//
	})
	void testFindByCodeShouldReturnWorksite() throws Exception {
		mvc.perform(get(BASE + "/{code}", "BCN-HQ").with(jwt())).andExpect(status().isOk()) //
				.andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON)) //
				.andExpect(jsonPath("$.code").value("BCN-HQ")) //
				.andExpect(jsonPath("$.name").value("Barcelona Headquarters")) //
				.andExpect(jsonPath("$.timeZone").value("UTC+02:00"));
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
	@Sql(statements = { //
			"INSERT INTO worksite(code,name,time_zone) VALUES('BCN-HQ','Barcelona Headquarters','UTC+2')"//
	})
	void testCreateAlreadyExistsShouldReturn400() throws Exception {
		String code = "BCN-HQ";
		String body = """
				  {"code":"%s","name":"Barcelona Headquarters","timeZone":"Europe/Madrid"}
				""".formatted(code);

		mvc.perform(post(BASE).contentType(APPLICATION_JSON).content(body).with(jwt())) //
				.andExpect(status().isBadRequest());
	}

	@Test
	void testCreateShouldReturn201AndBody() throws Exception {
		String code = "MAD-HUB";
		String body = """
				  {"code":"%s","name":"Madrid Hub","timeZone":"Europe/Madrid"}
				""".formatted(code);

		mvc.perform(post(BASE).contentType(APPLICATION_JSON).content(body).with(jwt())) //
				.andExpect(status().isCreated()) //
				.andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON)) //
				.andExpect(jsonPath("$.code").value(code)) //
				.andExpect(jsonPath("$.name").value("Madrid Hub")) //
				.andExpect(jsonPath("$.timeZone").value("Europe/Madrid"));

		mvc.perform(get(BASE).with(jwt())).andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.code=='%s')]".formatted(code)).exists());
	}

	@Test
	@Sql(statements = { //
			"INSERT INTO worksite(code,name,time_zone) VALUES('BCN-HQ','Barcelona Headquarters','UTC+2')"//
	})
	void testUpdateShouldReturn200AndUpdatedBody() throws Exception {
		String body = """
				  {"name":"Barcelona","timeZone":"UTC+1"}
				""";

		mvc.perform(put(BASE + "/{code}", "BCN-HQ").contentType(APPLICATION_JSON).content(body).with(jwt())) //
				.andExpect(status().isOk()) //
				.andExpect(jsonPath("$.code").value("BCN-HQ")) //
				.andExpect(jsonPath("$.name").value("Barcelona")) //
				.andExpect(jsonPath("$.timeZone").value("UTC+01:00"));
	}

	@Test
	@Sql(statements = { //
			"INSERT INTO worksite(code,name,time_zone) VALUES('BCN-HQ','Barcelona Headquarters','UTC+2')"//
	})
	void testDeleteShouldReturn204AndRemoveFromList() throws Exception {
		mvc.perform(delete(BASE + "/{code}", "BCN-HQ").with(jwt())) //
				.andExpect(status().isNoContent());

		mvc.perform(get(BASE).with(jwt())) //
				.andExpect(status().isOk()) //
				.andExpect(jsonPath("$[?(@.code=='BCN-HQ')]").doesNotExist()); //
	}
}
