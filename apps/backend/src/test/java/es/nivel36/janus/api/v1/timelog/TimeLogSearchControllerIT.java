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
package es.nivel36.janus.api.v1.timelog;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.core.authority.AuthorityUtils.createAuthorityList;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import es.nivel36.janus.api.v1.EmployeeIdentityTestExecutionListener;
import es.nivel36.janus.api.v1.SecurityTestConfiguration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(SecurityTestConfiguration.class)
@Transactional
@Sql("/sql/timelog-search.sql")
@TestExecutionListeners(listeners = EmployeeIdentityTestExecutionListener.class, mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
class TimeLogSearchControllerIT {

	private static final String BASE = "/api/v1/timelogs";
	private static final String OWN_EMAIL = "alice@example.test";
	private static final String OTHER_EMAIL = "bob@example.test";
	private static final String OWN_SUBJECT = "11111111-1111-4111-8111-111111111111";
	private static final String OTHER_SEARCH = "/api/v1/employees/" + OTHER_EMAIL + "/timelogs/";

	private @Autowired MockMvc mvc;

	@ParameterizedTest
	@ValueSource(strings = { BASE, BASE + "/" })
	void searchWithoutFiltersReturnsOnlyVisibleRecords(final String endpoint) throws Exception {
		this.mvc.perform(get(endpoint).param("sort", "entryTime,asc").with(employee()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(5))
				.andExpect(jsonPath("$.content[*].employeeEmail", everyItem(is(OWN_EMAIL))))
				.andExpect(jsonPath("$.content[*].entryTime", contains("2025-07-01T08:00:00Z",
						"2025-07-02T08:00:00Z", "2025-07-03T08:00:00Z", "2025-07-04T08:00:00Z",
						"2025-07-05T08:00:00Z")))
				.andExpect(jsonPath("$.page.totalElements").value(5));
	}

	@ParameterizedTest
	@ValueSource(strings = { BASE, BASE + "/" })
	void scopeIsAppliedBeforePaginationAndCounting(final String endpoint) throws Exception {
		assertOwnPage(endpoint, 0, "2025-07-01T08:00:00Z", "2025-07-02T08:00:00Z");
		assertOwnPage(endpoint, 1, "2025-07-03T08:00:00Z", "2025-07-04T08:00:00Z");
		assertOwnPage(endpoint, 2, "2025-07-05T08:00:00Z");
		assertOwnPage(endpoint, 3);
	}

	@Test
	void clientCanFilterByOwnEmailWithoutExpandingScope() throws Exception {
		this.mvc.perform(get(BASE).param("employeeEmail", OWN_EMAIL).param("page", "1")
				.param("size", "2").param("sort", "entryTime,desc").with(employee()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[*].employeeEmail", everyItem(is(OWN_EMAIL))))
				.andExpect(jsonPath("$.content[*].entryTime", contains("2025-07-03T08:00:00Z",
						"2025-07-02T08:00:00Z")))
				.andExpect(jsonPath("$.page.totalElements").value(5))
				.andExpect(jsonPath("$.page.totalPages").value(3));
	}

	@ParameterizedTest
	@ValueSource(strings = { BASE })
	void clientCannotExpandScopeByFilteringAnotherEmployee(final String endpoint) throws Exception {
		assertEmpty(this.mvc.perform(get(endpoint).param("employeeEmail", OTHER_EMAIL)
				.param("size", "1").with(employee())));
	}

	@ParameterizedTest
	@ValueSource(strings = { BASE })
	void dateRangeIsInclusiveAtStartExclusiveAtEndAndStillScoped(final String endpoint) throws Exception {
		for (int page = 0; page < 2; page++) {
			this.mvc.perform(get(endpoint).param("fromInstant", "2025-07-02T08:00:00Z")
					.param("toInstant", "2025-07-04T08:00:00Z").param("page", Integer.toString(page))
					.param("size", "1").param("sort", "entryTime,asc").with(employee()))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.content.length()").value(1))
					.andExpect(jsonPath("$.content[0].employeeEmail").value(OWN_EMAIL))
					.andExpect(jsonPath("$.content[0].entryTime").value(
							page == 0 ? "2025-07-02T08:00:00Z" : "2025-07-03T08:00:00Z"))
					.andExpect(jsonPath("$.page.totalElements").value(2))
					.andExpect(jsonPath("$.page.totalPages").value(2));
		}
	}

	@ParameterizedTest
	@CsvSource({ "ROLE_JANUS_USER,false", "ROLE_JANUS_ADMIN,false", "ROLE_JANUS_USER,true",
			"ROLE_JANUS_ADMIN,true" })
	void elevatedRolesCanSeeEveryEmployeeIncludingWhenAlsoEmployee(final String role,
			final boolean alsoEmployee) throws Exception {
		final String[] roles = alsoEmployee ? new String[] { role, "ROLE_JANUS_EMPLOYEE" }
				: new String[] { role };
		this.mvc.perform(get(BASE).param("size", "20").param("sort", "entryTime,asc")
				.with(jwt().jwt(token -> token.subject(OWN_SUBJECT)).authorities(createAuthorityList(roles))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(11))
				.andExpect(jsonPath("$.content[*].employeeEmail",
						hasItems(OWN_EMAIL, OTHER_EMAIL, "carol@example.test")))
				.andExpect(jsonPath("$.page.totalElements").value(11));
	}

	@ParameterizedTest
	@ValueSource(strings = { "ROLE_JANUS_USER", "ROLE_JANUS_ADMIN" })
	void elevatedScopeStillRespectsEmployeeFilterAndPagination(final String role) throws Exception {
		this.mvc.perform(get(BASE).param("employeeEmail", OTHER_EMAIL).param("page", "1")
				.param("size", "2").param("sort", "entryTime,asc")
				.with(jwt().authorities(createAuthorityList(role))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[*].employeeEmail", everyItem(is(OTHER_EMAIL))))
				.andExpect(jsonPath("$.content[*].entryTime", contains("2025-07-03T07:00:00Z",
						"2025-07-04T07:00:00Z")))
				.andExpect(jsonPath("$.page.totalElements").value(4))
				.andExpect(jsonPath("$.page.totalPages").value(2));
	}

	@ParameterizedTest
	@ValueSource(strings = { BASE })
	void employeeWithoutPersistentEmployeeAssociationHasEmptyScope(final String endpoint) throws Exception {
		for (int page : new int[] { 0, 3 }) {
			assertEmpty(this.mvc.perform(get(endpoint).param("page", Integer.toString(page)).param("size", "2")
					.with(jwt().jwt(token -> token.claim("email", OWN_EMAIL).claim("email_verified", true))
							.authorities(createAuthorityList("ROLE_JANUS_EMPLOYEE")))));
		}
	}

	@Test
	void scopeUsesPersistentEmployeeIdentityInsteadOfJwtEmail() throws Exception {
		this.mvc.perform(get(BASE)
				.with(employee().jwt(token -> token.subject(OWN_SUBJECT).claim("email", OTHER_EMAIL)
						.claim("email_verified", true))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(5))
				.andExpect(jsonPath("$.content[*].employeeEmail", everyItem(is(OWN_EMAIL))))
				.andExpect(jsonPath("$.page.totalElements").value(5));
	}

	@ParameterizedTest
	@ValueSource(strings = { BASE })
	void actorWithoutSearchRoleIsForbidden(final String endpoint) throws Exception {
		this.mvc.perform(get(endpoint).with(jwt().authorities(createAuthorityList())))
				.andExpect(status().isForbidden());
	}

	@Test
	void individualViewRemainsForbiddenForAnotherEmployee() throws Exception {
		this.mvc.perform(get(OTHER_SEARCH + "2025-07-01T07:00:00Z").with(employee()))
				.andExpect(status().isForbidden());
	}

	@ParameterizedTest
	@ValueSource(strings = { BASE })
	void incompleteOrReversedDateRangesAreRejected(final String endpoint) throws Exception {
		this.mvc.perform(get(endpoint).param("fromInstant", "2025-07-02T08:00:00Z").with(employee()))
				.andExpect(status().isBadRequest());
		this.mvc.perform(get(endpoint).param("toInstant", "2025-07-04T08:00:00Z").with(employee()))
				.andExpect(status().isBadRequest());
		this.mvc.perform(get(endpoint).param("fromInstant", "2025-07-04T08:00:00Z")
				.param("toInstant", "2025-07-02T08:00:00Z").with(employee()))
				.andExpect(status().isBadRequest());
	}

	private void assertOwnPage(final String endpoint, final int page, final String... expectedEntries)
			throws Exception {
		final MockHttpServletRequestBuilder request = get(endpoint).param("page", Integer.toString(page))
				.param("size", "2").param("sort", "entryTime,asc").with(employee());
		final ResultActions result = this.mvc.perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(expectedEntries.length))
				.andExpect(jsonPath("$.content[*].employeeEmail", everyItem(is(OWN_EMAIL))))
				.andExpect(jsonPath("$.page.number").value(page))
				.andExpect(jsonPath("$.page.size").value(2))
				.andExpect(jsonPath("$.page.totalElements").value(5))
				.andExpect(jsonPath("$.page.totalPages").value(3));
		if (expectedEntries.length > 0) {
			result.andExpect(jsonPath("$.content[*].entryTime", contains(expectedEntries)));
		}
	}

	private static void assertEmpty(final ResultActions result) throws Exception {
		result.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isEmpty())
				.andExpect(jsonPath("$.page.totalElements").value(0))
				.andExpect(jsonPath("$.page.totalPages").value(0));
	}

	private static JwtRequestPostProcessor employee() {
		return jwt().jwt(token -> token.subject(OWN_SUBJECT))
				.authorities(createAuthorityList("ROLE_JANUS_EMPLOYEE"));
	}
}
