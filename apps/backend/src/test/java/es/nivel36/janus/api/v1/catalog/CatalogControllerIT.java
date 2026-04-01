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
package es.nivel36.janus.api.v1.catalog;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.core.authority.AuthorityUtils.createAuthorityList;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import es.nivel36.janus.api.v1.SecurityTestConfiguration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(SecurityTestConfiguration.class)
@Transactional
class CatalogControllerIT {

	private static final String BASE = "/api/v1/catalogs/time-zones";

	private @Autowired MockMvc mvc;

	@Test
	void testSearchTimeZonesShouldReturn200AndPageData() throws Exception {
		this.mvc.perform(get(BASE).queryParam("search", "Europe/Madrid").with(jwt()//
				.authorities(createAuthorityList("ROLE_JANUS_ADMIN")))) //
				.andExpect(status().isOk()) //
				.andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON)) //
				.andExpect(jsonPath("$.content[0].literal").value(Matchers.containsString("Europe/Madrid"))) //
				.andExpect(jsonPath("$.content[0].level1").value("Europe")) //
				.andExpect(jsonPath("$.content[0].level2").value("Madrid")) //
				.andExpect(jsonPath("$.content[0].utc").value(Matchers.matchesPattern("UTC[+-]\\d{1,2}(:\\d{2})?")));
	}

	@Test
	void testSearchTimeZonesShouldBePaginated() throws Exception {
		this.mvc.perform(get(BASE).queryParam("page", "0").queryParam("size", "5").with(jwt()//
				.authorities(createAuthorityList("ROLE_JANUS_ADMIN")))) //
				.andExpect(status().isOk()) //
				.andExpect(jsonPath("$.content.length()").value(5)) //
				.andExpect(jsonPath("$.page.size").value(5));
	}

	@Test
	void testSearchTimeZonesShouldSortByUtcWhenRequested() throws Exception {
		this.mvc.perform(get(BASE).queryParam("sortBy", "UTC").queryParam("page", "0").queryParam("size", "20")
				.with(jwt().authorities(createAuthorityList("ROLE_JANUS_ADMIN")))) //
				.andExpect(status().isOk()) //
				.andExpect(jsonPath("$.content[0].utc").value(Matchers.startsWith("UTC-")));
	}

	@Test
	void testSearchTimeZonesShouldKeepRemainingSegmentsInSecondLevel() throws Exception {
		this.mvc.perform(get(BASE).queryParam("search", "America/Argentina/Buenos_Aires").with(jwt()//
				.authorities(createAuthorityList("ROLE_JANUS_ADMIN")))) //
				.andExpect(status().isOk()) //
				.andExpect(jsonPath("$.content[0].level1").value("America")) //
				.andExpect(jsonPath("$.content[0].level2").value("Argentina/Buenos_Aires"));
	}
}
