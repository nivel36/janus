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
package es.nivel36.janus.api.v1.appuser;

import static es.nivel36.janus.api.v1.SecurityTestConfiguration.verifiedJwt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.core.authority.AuthorityUtils.createAuthorityList;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;

import es.nivel36.janus.api.v1.SecurityTestConfiguration;
import es.nivel36.janus.service.appuser.Role;

/**
 * Exercises committed profiles with open-in-view disabled, without a test
 * transaction.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = "spring.data.rest.max-page-size=7")
@AutoConfigureMockMvc
@Import(SecurityTestConfiguration.class)
class AppUserFlowIT {
	private static final String BASE = "/api/v1/app-users";
	private static final String ADMIN = "appuser-flow-admin";
	private static final String OWNER = "appuser-flow-owner";
	private static final UUID ADMIN_ID = UUID.fromString("90000000-0000-4000-8000-000000000001");
	private static final UUID OWNER_ID = UUID.fromString("90000000-0000-4000-8000-000000000002");
	private static final UUID OTHER_ID = UUID.fromString("90000000-0000-4000-8000-000000000003");
	private static final String PREFERENCES = """
			{"locale":"fr-FR","timeFormat":"H12","defaultTimezone":"Europe/Paris","theme":"LIGHT"}
			""";
	private @Autowired MockMvc mvc;
	private @Autowired JdbcTemplate jdbc;

	@BeforeEach
	void createProfiles() {
		this.jdbc.update("INSERT INTO schedule(id,code,name) VALUES(91001,'APPUSER-FLOW','AppUser flow')");
		this.jdbc.update(
				"INSERT INTO employee(id,employee_number,name,surname,email,schedule_id) VALUES(91001,'FLOW-001','Flow','Owner','flow-employee@example.test',91001)");
		insert(ADMIN_ID, "flow-admin@example.test", ADMIN, null);
		insert(OWNER_ID, "flow-shared@example.test", OWNER, 91001L);
		insert(OTHER_ID, "flow-shared@example.test", "appuser-flow-other", null);
		insert(
				UUID.fromString("90000000-0000-4000-8000-000000000004"),
				"flow%_!literal@example.test",
				"appuser-flow-literal",
				null);
	}

	@AfterEach
	void removeProfiles() {
		this.jdbc.update("DELETE FROM app_user WHERE keycloak_subject LIKE 'appuser-flow-%'");
		this.jdbc.update("DELETE FROM employee WHERE id = 91001");
		this.jdbc.update("DELETE FROM schedule WHERE id = 91001");
	}

	private void insert(final UUID id, final String email, final String subject, final Long employeeId) {
		this.jdbc.update(
				"INSERT INTO app_user(id,email,keycloak_subject,locale,time_format,default_timezone,theme,employee_id) VALUES(?,?,?,'en-US','H24','UTC','DARK',?)",
				id,
				email,
				subject,
				employeeId);
	}

	private static JwtRequestPostProcessor actor(final String subject, final Role role) {
		return verifiedJwt().jwt(token -> token.subject(subject).claim("email", "copied@example.test"))
				.authorities(createAuthorityList("ROLE_" + role.name()));
	}

	@Test
	void listsAllProfilesIncludingUnlinkedOnes() throws Exception {
		final Integer count = this.jdbc.queryForObject("SELECT COUNT(*) FROM app_user", Integer.class);
		this.mvc.perform(get(BASE).param("size", "100").with(actor(ADMIN, Role.JANUS_ADMIN))).andExpect(status().isOk())
				.andExpect(jsonPath("$.page.totalElements").value(count));
		this.mvc.perform(get(BASE).param("email", "flow-admin").with(actor(ADMIN, Role.JANUS_ADMIN)))
				.andExpect(status().isOk()).andExpect(jsonPath("$.content[0].employeeNumber").doesNotExist())
				.andExpect(jsonPath("$.content[0].keycloakSubject").doesNotExist());
	}

	@Test
	void combinesPartialEmailAndExactEmployeeNumber() throws Exception {
		this.mvc.perform(get(BASE).param("email", "FLOW-SHARED").with(actor(ADMIN, Role.JANUS_ADMIN)))
				.andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements").value(2));
		this.mvc.perform(get(BASE).param("employeeNumber", "FLOW-001").with(actor(ADMIN, Role.JANUS_ADMIN)))
				.andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(OWNER_ID.toString()))
				.andExpect(jsonPath("$.content[0].employeeNumber").value("FLOW-001"));
		this.mvc.perform(
				get(BASE).param("email", "SHARED").param("employeeNumber", "FLOW-001")
						.with(actor(ADMIN, Role.JANUS_ADMIN)))
				.andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements").value(1));
		this.mvc.perform(
				get(BASE).param("email", "flow-admin").param("employeeNumber", "FLOW-001")
						.with(actor(ADMIN, Role.JANUS_ADMIN)))
				.andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty());
	}

	@Test
	void emailFilterLengthIsEnforcedOverHttp() throws Exception {
		this.mvc.perform(get(BASE).param("email", "a".repeat(254)).with(actor(ADMIN, Role.JANUS_ADMIN)))
				.andExpect(status().isOk());
		this.mvc.perform(get(BASE).param("email", "a".repeat(255)).with(actor(ADMIN, Role.JANUS_ADMIN)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void emptyEmailDisablesFilterAndWhitespaceIsNotTrimmed() throws Exception {
		final Integer count = this.jdbc.queryForObject("SELECT COUNT(*) FROM app_user", Integer.class);
		this.mvc.perform(get(BASE).param("email", "").with(actor(ADMIN, Role.JANUS_ADMIN))).andExpect(status().isOk())
				.andExpect(jsonPath("$.page.totalElements").value(count));
		this.mvc.perform(get(BASE).param("email", "  FLOW-SHARED  ").with(actor(ADMIN, Role.JANUS_ADMIN)))
				.andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty());
		this.mvc.perform(get(BASE).param("employeeNumber", "  FLOW-001  ").with(actor(ADMIN, Role.JANUS_ADMIN)))
				.andExpect(status().isBadRequest());
	}

	@ParameterizedTest
	@ValueSource(strings = { " ", "bad\nline" })
	void arbitraryEmailFragmentsAreAcceptedLiterally(final String fragment) throws Exception {
		this.mvc.perform(get(BASE).param("email", fragment).with(actor(ADMIN, Role.JANUS_ADMIN)))
				.andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty());
	}

	@ParameterizedTest
	@ValueSource(strings = { "%", "_", "!", "%_!" })
	void emailWildcardsAreLiteral(final String fragment) throws Exception {
		this.mvc.perform(get(BASE).param("email", fragment).with(actor(ADMIN, Role.JANUS_ADMIN)))
				.andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].email").value("flow%_!literal@example.test"));
	}

	@Test
	void pagingHasStableUuidTieBreakerAndAllowsPublicSortFields() throws Exception {
		this.mvc.perform(
				get(BASE).param("email", "flow-shared").param("size", "1").with(actor(ADMIN, Role.JANUS_ADMIN)))
				.andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(OWNER_ID.toString()))
				.andExpect(jsonPath("$.page.totalPages").value(2));
		this.mvc.perform(
				get(BASE).param("email", "flow-shared").param("size", "1").param("page", "1")
						.with(actor(ADMIN, Role.JANUS_ADMIN)))
				.andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(OTHER_ID.toString()));
		this.mvc.perform(
				get(BASE).param("email", "flow-shared").param("sort", "id,desc").with(actor(ADMIN, Role.JANUS_ADMIN)))
				.andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(OTHER_ID.toString()));
		this.mvc.perform(
				get(BASE).param("employeeNumber", "FLOW-001").param("sort", "employeeNumber,desc")
						.with(actor(ADMIN, Role.JANUS_ADMIN)))
				.andExpect(status().isOk()).andExpect(jsonPath("$.content[0].employeeNumber").value("FLOW-001"));
		this.mvc.perform(
				get(BASE).param("email", "flow").param("sort", "employeeNumber,asc")
						.with(actor(ADMIN, Role.JANUS_ADMIN)))
				.andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(4));
		this.mvc.perform(get(BASE).param("size", "1000").with(actor(ADMIN, Role.JANUS_ADMIN)))
				.andExpect(status().isOk()).andExpect(jsonPath("$.page.size").value(7));
	}

	@Test
	void rejectsInvalidFiltersAndInternalSortFields() throws Exception {
		this.mvc.perform(get(BASE).param("employeeNumber", "bad number").with(actor(ADMIN, Role.JANUS_ADMIN)))
				.andExpect(status().isBadRequest());
		this.mvc.perform(get(BASE).param("sort", "keycloakSubject,asc").with(actor(ADMIN, Role.JANUS_ADMIN)))
				.andExpect(status().isBadRequest());
	}

	@ParameterizedTest
	@EnumSource(Role.class)
	void searchAndDeleteAreAdministrative(final Role role) throws Exception {
		final int expectedSearch = role == Role.JANUS_ADMIN ? 200 : 403;
		final int expectedDelete = role == Role.JANUS_ADMIN ? 204 : 403;
		this.mvc.perform(get(BASE).with(actor(ADMIN, role))).andExpect(status().is(expectedSearch));
		this.mvc.perform(delete(BASE + "/" + OTHER_ID).with(actor(ADMIN, role))).andExpect(status().is(expectedDelete));
		assertThat(this.jdbc.queryForObject("SELECT COUNT(*) FROM app_user WHERE id = ?", Integer.class, OTHER_ID))
				.isEqualTo(role == Role.JANUS_ADMIN ? 0 : 1);
	}

	@ParameterizedTest
	@EnumSource(Role.class)
	void editingUsesPersistentOwnershipAndAdminOverride(final Role role) throws Exception {
		this.mvc.perform(
				put(BASE + "/" + OTHER_ID).with(actor(OWNER, role)).contentType(MediaType.APPLICATION_JSON)
						.content(PREFERENCES))
				.andExpect(status().is(role == Role.JANUS_ADMIN ? 200 : 403));
		assertThat(this.jdbc.queryForObject("SELECT theme FROM app_user WHERE id = ?", String.class, OTHER_ID))
				.isEqualTo(role == Role.JANUS_ADMIN ? "LIGHT" : "DARK");
		this.mvc.perform(
				put(BASE + "/" + OWNER_ID).with(actor(OWNER, role)).contentType(MediaType.APPLICATION_JSON)
						.content(PREFERENCES))
				.andExpect(status().isOk()).andExpect(jsonPath("$.employeeNumber").value("FLOW-001"))
				.andExpect(jsonPath("$.email").value("flow-shared@example.test"));
		assertThat(
				this.jdbc.queryForObject("SELECT keycloak_subject FROM app_user WHERE id = ?", String.class, OWNER_ID))
				.isEqualTo(OWNER);
		assertThat(this.jdbc.queryForObject("SELECT employee_id FROM app_user WHERE id = ?", Long.class, OWNER_ID))
				.isEqualTo(91001L);
	}

	@Test
	void anonymousAndUnprovisionedAdministratorsCannotOperate() throws Exception {
		this.mvc.perform(get(BASE)).andExpect(status().isUnauthorized());
		this.mvc.perform(get(BASE).with(actor("appuser-flow-missing", Role.JANUS_ADMIN)))
				.andExpect(status().isForbidden());
		this.mvc.perform(delete(BASE + "/" + OWNER_ID).with(actor("appuser-flow-missing", Role.JANUS_ADMIN)))
				.andExpect(status().isForbidden());
		this.mvc.perform(
				put(BASE + "/" + OWNER_ID).with(actor("appuser-flow-missing", Role.JANUS_ADMIN))
						.contentType(MediaType.APPLICATION_JSON).content(PREFERENCES))
				.andExpect(status().isForbidden());
		assertThat(this.jdbc.queryForObject("SELECT theme FROM app_user WHERE id = ?", String.class, OWNER_ID))
				.isEqualTo("DARK");
	}

	@Test
	void deletionPreservesEmployeeAndMissingProfilesReturn404() throws Exception {
		this.mvc.perform(delete(BASE + "/" + OWNER_ID).with(actor(ADMIN, Role.JANUS_ADMIN)))
				.andExpect(status().isNoContent());
		assertThat(this.jdbc.queryForObject("SELECT COUNT(*) FROM employee WHERE id = 91001", Integer.class)).isOne();
		assertThat(this.jdbc.queryForObject("SELECT COUNT(*) FROM app_user WHERE id = ?", Integer.class, OWNER_ID))
				.isZero();
		this.mvc.perform(delete(BASE + "/" + OWNER_ID).with(actor(ADMIN, Role.JANUS_ADMIN)))
				.andExpect(status().isNotFound());
		this.mvc.perform(
				put(BASE + "/" + OWNER_ID).with(actor(ADMIN, Role.JANUS_ADMIN)).contentType(MediaType.APPLICATION_JSON)
						.content(PREFERENCES))
				.andExpect(status().isNotFound());
	}

	@Test
	void invalidUpdateAndLaterClaimsPreservePreferencesAndAssociation() throws Exception {
		this.mvc.perform(
				put(BASE + "/" + OWNER_ID).with(actor(OWNER, Role.JANUS_USER)).contentType(MediaType.APPLICATION_JSON)
						.content(PREFERENCES.replace("Europe/Paris", "invalid/zone")))
				.andExpect(status().isBadRequest());
		assertThat(this.jdbc.queryForObject("SELECT locale FROM app_user WHERE id = ?", String.class, OWNER_ID))
				.isEqualTo("en-US");
		this.mvc.perform(
				get(BASE + "/me").with(
						verifiedJwt()
								.jwt(
										token -> token.subject(OWNER).claim("email", "UPDATED@EXAMPLE.TEST")
												.claim("employeeNumber", "UNKNOWN"))
								.authorities(createAuthorityList("ROLE_JANUS_USER"))))
				.andExpect(status().isOk()).andExpect(jsonPath("$.employeeNumber").value("FLOW-001"))
				.andExpect(jsonPath("$.email").value("flow-shared@example.test"));
	}
}
