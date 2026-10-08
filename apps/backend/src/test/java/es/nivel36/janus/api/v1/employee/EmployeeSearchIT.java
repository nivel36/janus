/*
 * Copyright 2026 Abel Ferrer Jiménez Licensed under the Apache License, Version
 * 2.0 (the "License");
 */
package es.nivel36.janus.api.v1.employee;

import static es.nivel36.janus.api.v1.SecurityTestConfiguration.verifiedJwt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.core.authority.AuthorityUtils.createAuthorityList;
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
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import es.nivel36.janus.api.v1.SecurityTestConfiguration;
import es.nivel36.janus.service.employee.EmployeeService;

/**
 * Exercises search through MVC, authorization, validation and the real
 * repository.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(SecurityTestConfiguration.class)
@Transactional
@Sql("/sql/employee-search.sql")
class EmployeeSearchIT {
	private static final String BASE = "/api/v1/employees";
	private @Autowired MockMvc mvc;
	private @MockitoSpyBean EmployeeService employees;

	@ParameterizedTest
	@CsvSource({ "emp-0003,EMP-0003", "CAROL,EMP-0003", "clark,EMP-0003", "CAROL@TEST,EMP-0003", "%_!,EMP-0002",
			"%,EMP-0002", "_,EMP-0002", "!,EMP-0002" })
	void matchesAllTextFieldsCaseInsensitivelyAndTreatsWildcardsLiterally(final String query, final String number)
			throws Exception {
		this.mvc.perform(
				get(BASE).param("query", query).with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_USER"))))
				.andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].employeeNumber").value(number));
	}

	@Test
	void absentFiltersIncludeUnassignedEmployeesWithoutDuplicateRows() throws Exception {
		this.mvc.perform(get(BASE).with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_ADMIN"))))
				.andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements").value(4))
				.andExpect(jsonPath("$.content.length()").value(4)).andExpect(jsonPath("$.page.size").value(20))
				.andExpect(jsonPath("$.content[3].employeeNumber").value("EMP-0004"));
	}

	@Test
	void worksiteFilterDoesNotDuplicateEmployeesWithMultipleAssignments() throws Exception {
		this.mvc.perform(
				get(BASE).param("worksiteCode", "HQ").param("size", "1").param("page", "1")
						.with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_ADMIN"))))
				.andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements").value(2))
				.andExpect(jsonPath("$.page.totalPages").value(2))
				.andExpect(jsonPath("$.content[0].employeeNumber").value("EMP-0003"));
	}

	@ParameterizedTest
	@CsvSource({ "scheduleCode,DAY,3", "scheduleCode,day,0", "scheduleCode,MISSING,0", "worksiteCode,HQ,2",
			"worksiteCode,hq,0", "worksiteCode,MISSING,0" })
	void codeFiltersMatchExactlyAndIndependently(final String filter, final String code, final int count)
			throws Exception {
		this.mvc.perform(
				get(BASE).param(filter, code).with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_USER"))))
				.andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements").value(count));
	}

	@Test
	void textFiltersAreNotTrimmed() throws Exception {
		this.mvc.perform(
				get(BASE).param("query", " Alice ")
						.with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_USER"))))
				.andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements").value(0));
	}

	@Test
	void equalNamesHaveStableOrderAcrossPages() throws Exception {
		for (int page = 0; page < 2; page++) {
			this.mvc.perform(
					get(BASE).param("sort", "name,asc").param("size", "1").param("page", Integer.toString(page))
							.with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_USER"))))
					.andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements").value(4))
					.andExpect(jsonPath("$.content[0].employeeNumber").value(page == 0 ? "EMP-0001" : "EMP-0002"));
		}
	}

	@Test
	void publicScheduleSortAndExplicitEmployeeNumberDirectionAreApplied() throws Exception {
		this.mvc.perform(
				get(BASE).param("sort", "scheduleCode,desc", "employeeNumber,desc")
						.with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_ADMIN"))))
				.andExpect(status().isOk()).andExpect(jsonPath("$.content[0].employeeNumber").value("EMP-0003"))
				.andExpect(jsonPath("$.content[1].employeeNumber").value("EMP-0004"));
	}

	@Test
	void pageSizeIsCapped() throws Exception {
		this.mvc.perform(
				get(BASE).param("size", "500").with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_ADMIN"))))
				.andExpect(status().isOk()).andExpect(jsonPath("$.page.size").value(100));
	}

	@ParameterizedTest
	@ValueSource(strings = { "id", "schedule.code", "appUser.email", "unknown" })
	void unsupportedSortReturnsBadRequest(final String field) throws Exception {
		this.mvc.perform(
				get(BASE).param("sort", field).with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_ADMIN"))))
				.andExpect(status().isBadRequest());
	}

	@ParameterizedTest
	@CsvSource({ "query,''", "scheduleCode,''", "worksiteCode,bad code" })
	void invalidFiltersReturnBadRequest(final String filter, final String value) throws Exception {
		this.mvc.perform(
				get(BASE).param(filter, value).with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_ADMIN"))))
				.andExpect(status().isBadRequest());
	}

	@ParameterizedTest
	@ValueSource(strings = { "ROLE_JANUS_EMPLOYEE", "ROLE_UNKNOWN" })
	void deniedRolesNeverExecuteSearch(final String role) throws Exception {
		this.mvc.perform(get(BASE).with(verifiedJwt().authorities(createAuthorityList(role))))
				.andExpect(status().isForbidden());
		verify(this.employees, never()).searchEmployees(any(), any(), any(), any());
	}

	@Test
	void unprovisionedAdministratorCannotSearch() throws Exception {
		this.mvc.perform(
				get(BASE).with(
						verifiedJwt().jwt(jwt -> jwt.subject("unprovisioned"))
								.authorities(createAuthorityList("ROLE_JANUS_ADMIN"))))
				.andExpect(status().isForbidden());
		verify(this.employees, never()).searchEmployees(any(), any(), any(), any());
	}

	@Test
	void unauthenticatedCallerIsRejectedBeforeSearch() throws Exception {
		this.mvc.perform(get(BASE)).andExpect(status().isUnauthorized());
		verify(this.employees, never()).searchEmployees(any(), any(), any(), any());
	}
}
