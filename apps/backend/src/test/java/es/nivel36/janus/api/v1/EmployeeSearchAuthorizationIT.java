package es.nivel36.janus.api.v1;

import static es.nivel36.janus.api.v1.SecurityTestConfiguration.verifiedJwt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.core.authority.AuthorityUtils.createAuthorityList;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import es.nivel36.janus.service.schedule.ScheduleService;
import es.nivel36.janus.service.timelog.TimeLogService;
import es.nivel36.janus.service.worksite.WorksiteService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(SecurityTestConfiguration.class)
@Transactional
@Sql("/sql/timelog-search.sql")
class EmployeeSearchAuthorizationIT {
	private static final String OWN_SUBJECT = "11111111-1111-4111-8111-111111111111";
	@Autowired
	private MockMvc mvc;
	@MockitoSpyBean
	private TimeLogService timeLogs;
	@MockitoSpyBean
	private ScheduleService schedules;
	@MockitoSpyBean
	private WorksiteService worksites;

	@ParameterizedTest
	@ValueSource(strings = { "/api/v1/time-logs", "/api/v1/schedules", "/api/v1/worksites" })
	void deniedEmployeeFiltersNeverExecuteSearch(final String endpoint) throws Exception {
		for (final String filter : new String[] { "EMP-0102", "MISSING" }) {
			this.mvc.perform(
					get(endpoint).param("employeeNumber", filter).with(
							verifiedJwt().jwt(token -> token.subject(OWN_SUBJECT))
									.authorities(createAuthorityList("ROLE_JANUS_EMPLOYEE"))))
					.andExpect(status().isForbidden());
		}
		assertNoSearch();
	}

	@ParameterizedTest
	@ValueSource(strings = { "/api/v1/time-logs", "/api/v1/schedules", "/api/v1/worksites" })
	void unlinkedEmployeesCannotSearch(final String endpoint) throws Exception {
		this.mvc.perform(get(endpoint).with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_EMPLOYEE"))))
				.andExpect(status().isForbidden());
		assertNoSearch();
	}

	@ParameterizedTest
	@ValueSource(strings = { "/api/v1/time-logs", "/api/v1/schedules", "/api/v1/worksites" })
	void linkedEmployeesCanSearchWithoutFilters(final String endpoint) throws Exception {
		this.mvc.perform(
				get(endpoint).param("size", "1").with(
						verifiedJwt().jwt(token -> token.subject(OWN_SUBJECT))
								.authorities(createAuthorityList("ROLE_JANUS_EMPLOYEE"))))
				.andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1));
	}

	@ParameterizedTest
	@ValueSource(strings = { "/api/v1/time-logs", "/api/v1/schedules", "/api/v1/worksites" })
	void elevatedMixedRolesCanFilterAnotherEmployee(final String endpoint) throws Exception {
		this.mvc.perform(
				get(endpoint).param("employeeNumber", "EMP-0102").with(
						verifiedJwt().jwt(token -> token.subject(OWN_SUBJECT))
								.authorities(createAuthorityList("ROLE_JANUS_EMPLOYEE", "ROLE_JANUS_USER"))))
				.andExpect(status().isOk()).andExpect(jsonPath("$.content").isNotEmpty());
	}

	private void assertNoSearch() {
		verify(this.timeLogs, never()).searchTimeLogs(any(), any(), any());
		verify(this.schedules, never()).searchSchedules(anyString(), nullable(String.class), any());
		verify(this.worksites, never()).searchWorksites(anyString(), nullable(String.class), any());
	}
}
