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
package es.nivel36.janus.api.v1.applicationsettings;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.core.authority.AuthorityUtils.createAuthorityList;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import es.nivel36.janus.api.v1.SecurityTestConfiguration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(SecurityTestConfiguration.class)
@Transactional
class ApplicationSettingsControllerIT {

	private static final String BASE = "/api/v1/applicationsettings";

	private @Autowired MockMvc mvc;

	@Test
	@Sql(statements = "INSERT INTO application_settings(id, days_until_locked, employee_workplace_creation_allowed, worksite_change_during_shift_allowed) VALUES (1, 7, true, false)")
	void testFindShouldReturnCurrentSettingsForEmployeeRole() throws Exception {
		this.mvc.perform(get(BASE).with(jwt().authorities(createAuthorityList("ROLE_JANUS_EMPLOYEE"))))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
				.andExpect(jsonPath("$.daysUntilLocked").isNumber())
				.andExpect(jsonPath("$.employeeWorkplaceCreationAllowed").isBoolean())
				.andExpect(jsonPath("$.worksiteChangeDuringShiftAllowed").isBoolean());
	}

	@Test
	@Sql(statements = "INSERT INTO application_settings(id, days_until_locked, employee_workplace_creation_allowed, worksite_change_during_shift_allowed) VALUES (1, 7, true, false)")
	void testUpdateShouldReturnForbiddenForNonAdmin() throws Exception {
		final String body = """
				{"daysUntilLocked":5,"employeeWorkplaceCreationAllowed":true,"worksiteChangeDuringShiftAllowed":false}
				""";

		this.mvc.perform(put(BASE).contentType(APPLICATION_JSON).content(body)
				.with(jwt().authorities(createAuthorityList("ROLE_JANUS_USER")))).andExpect(status().isUnauthorized());
	}

	@Test
	@Sql(statements = "INSERT INTO application_settings(id, days_until_locked, employee_workplace_creation_allowed, worksite_change_during_shift_allowed) VALUES (1, 7, true, false)")
	void testUpdateShouldReturnUpdatedSettingsForAdmin() throws Exception {
		final String body = """
				{"daysUntilLocked":3,"employeeWorkplaceCreationAllowed":false,"worksiteChangeDuringShiftAllowed":true}
				""";

		this.mvc.perform(put(BASE).contentType(APPLICATION_JSON).content(body)
				.with(jwt().authorities(createAuthorityList("ROLE_JANUS_ADMIN"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.daysUntilLocked").value(3))
				.andExpect(jsonPath("$.employeeWorkplaceCreationAllowed").value(false))
				.andExpect(jsonPath("$.worksiteChangeDuringShiftAllowed").value(true));
	}
}
