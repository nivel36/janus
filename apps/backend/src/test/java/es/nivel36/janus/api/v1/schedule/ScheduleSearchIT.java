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
package es.nivel36.janus.api.v1.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.core.authority.AuthorityUtils.createAuthorityList;
import static es.nivel36.janus.api.v1.SecurityTestConfiguration.verifiedJwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import es.nivel36.janus.api.v1.EmployeeIdentityTestExecutionListener;
import es.nivel36.janus.api.v1.SecurityTestConfiguration;
import es.nivel36.janus.service.schedule.Schedule;
import es.nivel36.janus.service.schedule.ScheduleService;
import jakarta.persistence.EntityManager;

/**
 * Exercises filtering, database pagination, authorization and invalid payloads.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = "spring.jpa.properties.hibernate.query.fail_on_pagination_over_collection_fetch=true")
@AutoConfigureMockMvc
@Import(SecurityTestConfiguration.class)
@Transactional
@Sql("/sql/schedule-search.sql")
@TestExecutionListeners(listeners = EmployeeIdentityTestExecutionListener.class, mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
class ScheduleSearchIT {
	private static final String BASE = "/api/v1/schedules";
	private @Autowired MockMvc mvc;
	private @Autowired ScheduleService service;
	private @Autowired ScheduleResponseMapper mapper;
	private @Autowired EntityManager entityManager;
	private @Autowired JdbcTemplate jdbc;

	@Test
	void queryIncludesSchedulesWithoutRulesOrRangesAndPagesWithoutDuplicates() throws Exception {
		this.mvc.perform(
				get(BASE).param("query", "sHiFt").param("size", "2")
						.with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_ADMIN"))))
				.andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements").value(6))
				.andExpect(jsonPath("$.page.totalPages").value(3)).andExpect(jsonPath("$.content.length()").value(2))
				.andExpect(jsonPath("$.content[0].code").value("SCH-A"))
				.andExpect(jsonPath("$.content[0].rules[0].dayOfWeekRanges.length()").value(2))
				.andExpect(jsonPath("$.content[1].code").value("SCH-B"))
				.andExpect(jsonPath("$.content[1].rules[0].dayOfWeekRanges").isEmpty());
		this.mvc.perform(
				get(BASE).param("query", "shift").param("size", "2").param("page", "1")
						.with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_ADMIN"))))
				.andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements").value(6))
				.andExpect(jsonPath("$.content[0].code").value("SCH-C"))
				.andExpect(jsonPath("$.content[0].rules").isEmpty())
				.andExpect(jsonPath("$.content[1].code").value("SCH-D"));
	}

	@Test
	void rulesAreReadableAfterLeavingThePersistenceContext() {
		final Page<Schedule> page = this.service.searchSchedules("shift", null, PageRequest.of(0, 2));
		this.entityManager.clear();
		assertThat(page.map(this.mapper::map).getContent()).hasSize(2);
		assertThat(this.mapper.map(page.getContent().getFirst()).rules().getFirst().dayOfWeekRanges()).hasSize(2);
		assertThat(this.mapper.map(page.getContent().get(1)).rules().getFirst().dayOfWeekRanges()).isEmpty();
	}

	@ParameterizedTest
	@CsvSource({ "%,SCH-D", "_,SCH-E", "!,SCH-F" })
	void wildcardAndEscapeCharactersAreLiteral(final String query, final String code) throws Exception {
		this.mvc.perform(
				get(BASE).param("query", query)
						.with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_ADMIN"))))
				.andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].code").value(code));
	}

	@Test
	void queryMatchesCodeAndUsesWhitespaceWithoutTrimming() throws Exception {
		this.mvc.perform(
				get(BASE).param("query", "sch-c")
						.with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_USER"))))
				.andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].code").value("SCH-C"));
		this.mvc.perform(
				get(BASE).param("query", " Alpha ")
						.with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_ADMIN"))))
				.andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty());
	}

	@ParameterizedTest
	@ValueSource(strings = { "ROLE_JANUS_ADMIN", "ROLE_JANUS_USER" })
	void employeeFilterIsAppliedWithoutTextAndCombinesWithText(final String role) throws Exception {
		this.mvc.perform(
				get(BASE).param("employeeNumber", "EMP-0102")
						.with(verifiedJwt().authorities(createAuthorityList(role))))
				.andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].code").value("SCH-B"));
		this.mvc.perform(
				get(BASE).param("employeeNumber", "EMP-0102").param("query", "Alpha")
						.with(verifiedJwt().authorities(createAuthorityList(role))))
				.andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty());
		this.mvc.perform(
				get(BASE).param("employeeNumber", "UNKNOWN").with(verifiedJwt().authorities(createAuthorityList(role))))
				.andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty());
	}

	@Test
	void employeeWithoutRequestedFilterOnlySeesOwnScheduleAndCannotRequestAnother() throws Exception {
		this.mvc.perform(
				get(BASE).with(
						verifiedJwt().jwt(jwt -> jwt.subject("employee-EMP-0102"))
								.authorities(createAuthorityList("ROLE_JANUS_EMPLOYEE"))))
				.andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].code").value("SCH-B"));
		this.mvc.perform(
				get(BASE).param("employeeNumber", "EMP-0101").with(
						verifiedJwt().jwt(jwt -> jwt.subject("employee-EMP-0102"))
								.authorities(createAuthorityList("ROLE_JANUS_EMPLOYEE"))))
				.andExpect(status().isForbidden());
	}

	@Test
	void employeeViewIsRestrictedToTheAssignedSchedule() throws Exception {
		this.mvc.perform(
				get(BASE + "/SCH-B").with(
						verifiedJwt().jwt(jwt -> jwt.subject("employee-EMP-0102"))
								.authorities(createAuthorityList("ROLE_JANUS_EMPLOYEE"))))
				.andExpect(status().isOk());
		this.mvc.perform(
				get(BASE + "/SCH-A").with(
						verifiedJwt().jwt(jwt -> jwt.subject("employee-EMP-0102"))
								.authorities(createAuthorityList("ROLE_JANUS_EMPLOYEE"))))
				.andExpect(status().isForbidden());
	}

	@Test
	void missingEmployeeLinkOrUnprovisionedActorCannotSearch() throws Exception {
		this.mvc.perform(get(BASE).with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_EMPLOYEE"))))
				.andExpect(status().isForbidden());
		this.mvc.perform(
				get(BASE).with(
						verifiedJwt().jwt(jwt -> jwt.subject("not-provisioned"))
								.authorities(createAuthorityList("ROLE_JANUS_ADMIN"))))
				.andExpect(status().isForbidden());
	}

	@Test
	void sizeIsCappedAndExplicitDescendingOrderIsRespected() throws Exception {
		this.mvc.perform(
				get(BASE).param("size", "500").param("sort", "code,desc")
						.with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_ADMIN"))))
				.andExpect(status().isOk()).andExpect(jsonPath("$.page.size").value(100))
				.andExpect(jsonPath("$.content[0].code").value("SCH-F"))
				.andExpect(jsonPath("$.content[5].code").value("SCH-A"));
		this.mvc.perform(
				get(BASE).param("sort", "name,desc").param("size", "1")
						.with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_ADMIN"))))
				.andExpect(status().isOk()).andExpect(jsonPath("$.content[0].code").value("SCH-E"));
	}

	@ParameterizedTest
	@ValueSource(strings = { "rules.name", "employees.employeeNumber", "id", "unknown" })
	void internalSortFieldsReturnBadRequest(final String property) throws Exception {
		this.mvc.perform(
				get(BASE).param("sort", property)
						.with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_ADMIN"))))
				.andExpect(status().isBadRequest());
	}

	@ParameterizedTest
	@ValueSource(strings = { "STD!", "bad code", " STD ", "é" })
	void invalidPathCodeIsRejectedForAllOperations(final String code) throws Exception {
		this.mvc.perform(
				get(BASE + "/{code}", code).with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_ADMIN"))))
				.andExpect(status().isBadRequest());
		this.mvc.perform(
				put(BASE + "/{code}", code).contentType(APPLICATION_JSON).content(replacement("[]"))
						.with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_ADMIN"))))
				.andExpect(status().isBadRequest());
		this.mvc.perform(
				delete(BASE + "/{code}", code).with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_ADMIN"))))
				.andExpect(status().isBadRequest());
	}

	@ParameterizedTest
	@ValueSource(strings = { "[null]", "[{\"name\":\"Rule\",\"dayOfWeekRanges\":[null]}]" })
	void nullNestedElementsAreBadRequestsAndLeavePersistedDataUntouched(final String rules) throws Exception {
		final String body = replacement(rules);
		this.mvc.perform(
				post(BASE).contentType(APPLICATION_JSON).content("{\"code\":\"NEW\"," + body.substring(1))
						.with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_ADMIN"))))
				.andExpect(status().isBadRequest());
		this.mvc.perform(
				put(BASE + "/SCH-A").contentType(APPLICATION_JSON).content(body)
						.with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_ADMIN"))))
				.andExpect(status().isBadRequest());
		assertThat(this.jdbc.queryForObject("SELECT COUNT(*) FROM schedule", Long.class)).isEqualTo(6L);
		assertThat(this.jdbc.queryForObject("SELECT name FROM schedule WHERE code='SCH-A'", String.class))
				.isEqualTo("Alpha shift");
		assertThat(this.jdbc.queryForObject("SELECT COUNT(*) FROM schedule_rule WHERE schedule_id=101", Long.class))
				.isEqualTo(1L);
	}

	@Test
	void employeeCannotMutateSchedules() throws Exception {
		final String body = replacement("[]");
		this.mvc.perform(
				post(BASE).contentType(APPLICATION_JSON).content("{\"code\":\"NEW\"," + body.substring(1)).with(
						verifiedJwt().jwt(jwt -> jwt.subject("employee-EMP-0102"))
								.authorities(createAuthorityList("ROLE_JANUS_EMPLOYEE"))))
				.andExpect(status().isForbidden());
		this.mvc.perform(
				put(BASE + "/SCH-B").contentType(APPLICATION_JSON).content(body).with(
						verifiedJwt().jwt(jwt -> jwt.subject("employee-EMP-0102"))
								.authorities(createAuthorityList("ROLE_JANUS_EMPLOYEE"))))
				.andExpect(status().isForbidden());
		this.mvc.perform(
				delete(BASE + "/SCH-B").with(
						verifiedJwt().jwt(jwt -> jwt.subject("employee-EMP-0102"))
								.authorities(createAuthorityList("ROLE_JANUS_EMPLOYEE"))))
				.andExpect(status().isForbidden());
	}

	private static String replacement(final String rules) {
		return "{\"name\":\"Replacement\",\"entryTolerance\":\"PT0S\",\"exitTolerance\":\"PT0S\",\"rules\":" + rules
				+ "}";
	}
}
