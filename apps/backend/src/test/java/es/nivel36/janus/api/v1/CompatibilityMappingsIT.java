/*
 * Copyright 2026 Abel Ferrer Jiménez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package es.nivel36.janus.api.v1;

import static es.nivel36.janus.api.v1.SecurityTestConfiguration.verifiedJwt;
import static org.springframework.security.core.authority.AuthorityUtils.createAuthorityList;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(SecurityTestConfiguration.class)
@TestExecutionListeners(listeners = EmployeeIdentityTestExecutionListener.class,
		mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
class CompatibilityMappingsIT {

	private static final String TIME = "2026-01-02T08:00:00Z";
	private static final String EMAIL = "person@example.test";

	private @Autowired MockMvc mvc;

	@Test
	@Sql(statements = {
			"INSERT INTO schedule(id,code,name) VALUES(1,'STD-WH','Standard Work Hours')",
			"INSERT INTO employee(id,employee_number,name,surname,email,schedule_id) VALUES(1,'EMP-0001','Compatibility','User','person@example.test',1)",
			"INSERT INTO worksite(id,code,name,time_zone,scope) VALUES(1,'WS-1','Compatibility site','UTC','ASSIGNED')" })
	@Transactional
	void canonicalOperationsRejectAnEmail() throws Exception {
		this.mvc.perform(MockMvcRequestBuilders.get("/api/v1/employees/person@example.test/time-logs/" + TIME)
				.with(adminJwt()))
				.andExpect(status().isBadRequest());

		this.mvc.perform(MockMvcRequestBuilders
				.get("/api/v1/employees/person@example.test/worksites/WS-1/clock-out-without-clock-in-events/" + TIME)
				.param("worksiteCode", "WS-1").with(adminJwt()))
				.andExpect(status().isBadRequest());

		this.mvc.perform(MockMvcRequestBuilders.put("/api/v1/worksites/WS-1/employees/person@example.test")
				.with(adminJwt()))
				.andExpect(status().isBadRequest());

	}

	private static JwtRequestPostProcessor adminJwt() {
		return verifiedJwt().jwt(jwt -> jwt.claim("realm_access", Map.of("roles", List.of("janus_admin"))))
				.authorities(createAuthorityList("ROLE_JANUS_ADMIN"));
	}

	@TestFactory
	Stream<DynamicTest> everyCompatibilityMappingRoutesToItsControllerMethod() {
		return mappings().stream().map(mapping -> DynamicTest.dynamicTest(mapping.method() + " " + mapping.path(),
				() -> this.mvc.perform(request(mapping)).andExpect(handler().methodName(mapping.handlerMethod()))));
	}

	private static MockHttpServletRequestBuilder request(final Mapping mapping) {
		return MockMvcRequestBuilders.request(mapping.method(), mapping.path())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}")
				.with(verifiedJwt().authorities(createAuthorityList("ROLE_JANUS_ADMIN")));
	}

	private static List<Mapping> mappings() {
		return List.of(
				mapping(HttpMethod.GET, "/api/v1/appusers/me", "findCurrentAppUser"),
				mapping(HttpMethod.PUT, "/api/v1/appusers/00000000-0000-0000-0000-000000000001", "updateAppUser"),
				mapping(HttpMethod.DELETE, "/api/v1/appusers/00000000-0000-0000-0000-000000000001", "deleteAppUser"),
				mapping(HttpMethod.GET, "/api/v1/applicationsettings", "findApplicationSettings"),
				mapping(HttpMethod.PUT, "/api/v1/applicationsettings", "updateApplicationSettings"),
				mapping(HttpMethod.GET, "/api/v1/timelogs", "searchTimeLogs"),
				mapping(HttpMethod.POST, "/api/v1/employees/EMP-1/timelogs/clock-in", "clockIn"),
				mapping(HttpMethod.POST, "/api/v1/employees/EMP-1/timelogs/clock-out", "clockOut"),
				mapping(HttpMethod.POST, "/api/v1/employees/EMP-1/timelogs", "createTimeLog"),
				mapping(HttpMethod.GET, "/api/v1/employees/EMP-1/timelogs/" + TIME,
						"findTimeLogByEmployeeAndEntryTime"),
				mapping(HttpMethod.DELETE, "/api/v1/employees/EMP-1/timelogs/" + TIME, "deleteTimeLog"),
				mapping(HttpMethod.GET, "/api/v1/employees/EMP-1/clock-out-without-clock-in-events/" + TIME,
						"findClockOutWithoutClockInEventLegacy"),
				mapping(HttpMethod.PATCH, "/api/v1/employees/EMP-1/clock-out-without-clock-in-events/" + TIME,
						"transitionClockOutWithoutClockInEventLegacy"),
				mapping(HttpMethod.POST, "/api/v1/employees/EMP-1/clock-out-without-clock-in-events/" + TIME
						+ "/resolve", "resolveClockOutWithoutClockInEventLegacy"),
				mapping(HttpMethod.POST, "/api/v1/employees/EMP-1/clock-out-without-clock-in-events/" + TIME
						+ "/invalidate", "invalidateClockOutWithoutClockInEventLegacy"));
	}

	private static Mapping mapping(final HttpMethod method, final String path, final String handlerMethod) {
		return new Mapping(method, path, handlerMethod);
	}

	private record Mapping(HttpMethod method, String path, String handlerMethod) {
	}
}
