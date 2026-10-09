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
package es.nivel36.janus.api.v1.timelog;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.core.authority.AuthorityUtils.createAuthorityList;
import static es.nivel36.janus.api.v1.SecurityTestConfiguration.verifiedJwt;
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
import org.springframework.jdbc.core.JdbcTemplate;
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

	private static final String BASE = "/api/v1/time-logs";
	private static final String OWN_EMAIL = "alice@example.test";
	private static final String OTHER_EMAIL = "bob@example.test";
	private static final String OWN_SUBJECT = "11111111-1111-4111-8111-111111111111";
	private static final String OWN_SEARCH = "/api/v1/employees/EMP-0101/time-logs/";

	private @Autowired MockMvc mvc;
	private @Autowired JdbcTemplate jdbc;

	@Test
	void employeeNumberFilterSurvivesEmployeeEmailChange() throws Exception {
		this.jdbc.update(
				"UPDATE employee SET email = ? WHERE employee_number = ?",
				"alice.changed@example.test",
				"EMP-0101");

		this.mvc.perform(
				get(BASE).param("employeeNumber", "EMP-0101")
						.with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_USER"))))
				.andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(5))
				.andExpect(jsonPath("$.content[*].employeeNumber", everyItem(is("EMP-0101"))));
	}

	@Test
	void searchWithoutFiltersReturnsOnlyVisibleRecords() throws Exception {
		this.mvc.perform(get(BASE).param("sort", "entryTime,asc").with(employee())).andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(5))
				.andExpect(jsonPath("$.content[*].employeeNumber", everyItem(is("EMP-0101"))))
				.andExpect(
						jsonPath(
								"$.content[*].entryTime",
								contains(
										"2025-07-01T08:00:00Z",
										"2025-07-02T08:00:00Z",
										"2025-07-03T08:00:00Z",
										"2025-07-04T08:00:00Z",
										"2025-07-05T08:00:00Z")))
				.andExpect(jsonPath("$.page.totalElements").value(5));
	}

	@Test
	void scopeIsAppliedBeforePaginationAndCounting() throws Exception {
		assertOwnPage(BASE, 0, "2025-07-01T08:00:00Z", "2025-07-02T08:00:00Z");
		assertOwnPage(BASE, 1, "2025-07-03T08:00:00Z", "2025-07-04T08:00:00Z");
		assertOwnPage(BASE, 2, "2025-07-05T08:00:00Z");
		assertOwnPage(BASE, 3);
	}

	@Test
	void clientCanFilterByOwnEmployeeNumberWithoutExpandingScope() throws Exception {
		this.mvc.perform(
				get(BASE).param("employeeNumber", "EMP-0101").param("page", "1").param("size", "2")
						.param("sort", "entryTime,desc").with(employee()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[*].employeeNumber", everyItem(is("EMP-0101"))))
				.andExpect(jsonPath("$.content[*].entryTime", contains("2025-07-03T08:00:00Z", "2025-07-02T08:00:00Z")))
				.andExpect(jsonPath("$.page.totalElements").value(5)).andExpect(jsonPath("$.page.totalPages").value(3));
	}

	@ParameterizedTest
	@CsvSource({ "ROLE_JANUS_EMPLOYEE," + OWN_SUBJECT, "ROLE_JANUS_USER,user" })
	void employeeNumberFilterWorksForRestrictedAndPrivilegedUsers(final String role, final String subject)
			throws Exception {
		this.mvc.perform(
				get(BASE).param("employeeNumber", "EMP-0101").with(
						verifiedJwt().jwt(token -> token.subject(subject)).authorities(createAuthorityList(role))))
				.andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(5))
				.andExpect(jsonPath("$.content[*].employeeNumber", everyItem(is("EMP-0101"))));
	}

	@Test
	void clientCannotExpandScopeByFilteringAnotherEmployee() throws Exception {
		this.mvc.perform(get(BASE).param("employeeNumber", "EMP-0102").param("size", "1").with(employee()))
				.andExpect(status().isForbidden());
	}

	@Test
	void dateRangeIsInclusiveAtStartExclusiveAtEndAndStillScoped() throws Exception {
		for (int page = 0; page < 2; page++) {
			this.mvc.perform(
					get(BASE).param("start", "2025-07-02T08:00:00Z").param("end", "2025-07-04T08:00:00Z")
							.param("page", Integer.toString(page)).param("size", "1").param("sort", "entryTime,asc")
							.with(employee()))
					.andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1))
					.andExpect(jsonPath("$.content[0].employeeNumber").value("EMP-0101"))
					.andExpect(
							jsonPath("$.content[0].entryTime")
									.value(page == 0 ? "2025-07-02T08:00:00Z" : "2025-07-03T08:00:00Z"))
					.andExpect(jsonPath("$.page.totalElements").value(2))
					.andExpect(jsonPath("$.page.totalPages").value(2));
		}
	}

	@ParameterizedTest
	@CsvSource({ "ROLE_JANUS_USER,false", "ROLE_JANUS_ADMIN,false", "ROLE_JANUS_USER,true", "ROLE_JANUS_ADMIN,true" })
	void elevatedRolesCanSeeEveryEmployeeIncludingWhenAlsoEmployee(final String role, final boolean alsoEmployee)
			throws Exception {
		final String[] roles = alsoEmployee ? new String[] { role, "ROLE_JANUS_EMPLOYEE" } : new String[] { role };
		this.mvc.perform(
				get(BASE).param("size", "20").param("sort", "entryTime,asc").with(
						verifiedJwt().jwt(token -> token.subject(OWN_SUBJECT)).authorities(createAuthorityList(roles))))
				.andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(11))
				.andExpect(jsonPath("$.content[*].employeeNumber", hasItems("EMP-0101", "EMP-0102", "EMP-0103")))
				.andExpect(jsonPath("$.page.totalElements").value(11));
	}

	@ParameterizedTest
	@ValueSource(strings = { "ROLE_JANUS_USER", "ROLE_JANUS_ADMIN" })
	void elevatedScopeStillRespectsEmployeeFilterAndPagination(final String role) throws Exception {
		this.mvc.perform(
				get(BASE).param("employeeNumber", "EMP-0102").param("page", "1").param("size", "2")
						.param("sort", "entryTime,asc").with(verifiedJwt().authorities(createAuthorityList(role))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[*].employeeNumber", everyItem(is("EMP-0102"))))
				.andExpect(jsonPath("$.content[*].entryTime", contains("2025-07-03T07:00:00Z", "2025-07-04T07:00:00Z")))
				.andExpect(jsonPath("$.page.totalElements").value(4)).andExpect(jsonPath("$.page.totalPages").value(2));
	}

	@Test
	void employeeWithoutPersistentEmployeeAssociationCannotSearch() throws Exception {
		for (int page : new int[] { 0, 3 }) {
			this.mvc.perform(
					get(BASE).param("page", Integer.toString(page)).param("size", "2").with(
							verifiedJwt().jwt(token -> token.claim("email", OWN_EMAIL).claim("email_verified", true))
									.authorities(createAuthorityList("ROLE_JANUS_EMPLOYEE"))))
					.andExpect(status().isForbidden());
		}
	}

	@Test
	void scopeUsesPersistentEmployeeIdentityInsteadOfJwtEmail() throws Exception {
		this.mvc.perform(
				get(BASE).with(
						employee().jwt(
								token -> token.subject(OWN_SUBJECT).claim("email", OTHER_EMAIL)
										.claim("email_verified", true))))
				.andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(5))
				.andExpect(jsonPath("$.content[*].employeeNumber", everyItem(is("EMP-0101"))))
				.andExpect(jsonPath("$.page.totalElements").value(5));
	}

	@Test
	void actorWithoutSearchRoleIsForbidden() throws Exception {
		this.mvc.perform(get(BASE).with(verifiedJwt().authorities(createAuthorityList())))
				.andExpect(status().isForbidden());
	}

	@Test
	void individualViewIsScopedToTheLinkedEmployee() throws Exception {
		this.mvc.perform(get(OWN_SEARCH + "2025-07-01T08:00:00Z").with(employee())).andExpect(status().isOk())
				.andExpect(jsonPath("$.employeeNumber").value("EMP-0101"));
	}

	@Test
	void incompleteOrNonIncreasingDateRangesAreRejected() throws Exception {
		this.mvc.perform(get(BASE).param("start", "2025-07-02T08:00:00Z").with(employee()))
				.andExpect(status().isBadRequest());
		this.mvc.perform(get(BASE).param("end", "2025-07-04T08:00:00Z").with(employee()))
				.andExpect(status().isBadRequest());
		this.mvc.perform(
				get(BASE).param("start", "2025-07-04T08:00:00Z").param("end", "2025-07-02T08:00:00Z").with(employee()))
				.andExpect(status().isBadRequest());
		this.mvc.perform(
				get(BASE).param("start", "2025-07-04T08:00:00Z").param("end", "2025-07-04T08:00:00Z").with(employee()))
				.andExpect(status().isBadRequest());
	}

	@Test
	void missingEmployeeFilterDoesNotDiscloseExistenceToRestrictedActors() throws Exception {
		this.mvc.perform(get(BASE).param("employeeNumber", "MISSING").with(employee()))
				.andExpect(status().isForbidden());
	}

	@ParameterizedTest
	@ValueSource(strings = { "missing", "employee.email", "deleted" })
	void unsupportedSortReturnsBadRequest(final String field) throws Exception {
		this.mvc.perform(get(BASE).param("sort", field + ",asc").with(employee())).andExpect(status().isBadRequest());
	}

	@ParameterizedTest
	@ValueSource(strings = { " EMP-0101 ", "EMP.0101" })
	void invalidEmployeeNumberReturnsBadRequest(final String employeeNumber) throws Exception {
		this.mvc.perform(
				get(BASE).param("employeeNumber", employeeNumber)
						.with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_ADMIN"))))
				.andExpect(status().isBadRequest());
	}

	@Test
	void defaultSearchOrdersNewestFirst() throws Exception {
		this.mvc.perform(get(BASE).with(employee())).andExpect(status().isOk())
				.andExpect(
						jsonPath(
								"$.content[*].entryTime",
								contains(
										"2025-07-05T08:00:00Z",
										"2025-07-04T08:00:00Z",
										"2025-07-03T08:00:00Z",
										"2025-07-02T08:00:00Z",
										"2025-07-01T08:00:00Z")))
				.andExpect(jsonPath("$.page.size").value(20));
	}

	private void assertOwnPage(final String endpoint, final int page, final String... expectedEntries)
			throws Exception {
		final MockHttpServletRequestBuilder request = get(endpoint).param("page", Integer.toString(page))
				.param("size", "2").param("sort", "entryTime,asc").with(employee());
		final ResultActions result = this.mvc.perform(request).andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(expectedEntries.length))
				.andExpect(jsonPath("$.content[*].employeeNumber", everyItem(is("EMP-0101"))))
				.andExpect(jsonPath("$.page.number").value(page)).andExpect(jsonPath("$.page.size").value(2))
				.andExpect(jsonPath("$.page.totalElements").value(5)).andExpect(jsonPath("$.page.totalPages").value(3));
		if (expectedEntries.length > 0) {
			result.andExpect(jsonPath("$.content[*].entryTime", contains(expectedEntries)));
		}
	}

	private static JwtRequestPostProcessor employee() {
		return verifiedJwt().jwt(token -> token.subject(OWN_SUBJECT))
				.authorities(createAuthorityList("ROLE_JANUS_EMPLOYEE"));
	}
}
