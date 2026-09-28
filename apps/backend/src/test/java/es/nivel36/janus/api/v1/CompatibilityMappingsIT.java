/*
 * Copyright 2026 Abel Ferrer Jiménez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package es.nivel36.janus.api.v1;

import static es.nivel36.janus.api.v1.SecurityTestConfiguration.verifiedJwt;
import static org.springframework.security.core.authority.AuthorityUtils.createAuthorityList;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(SecurityTestConfiguration.class)
class CompatibilityMappingsIT {

	private static final String TIME = "2026-01-02T08:00:00Z";
	private static final String EMAIL = "person@example.test";

	private @Autowired MockMvc mvc;

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
				mapping(HttpMethod.POST, "/api/v1/employees/by-email/" + EMAIL + "/time-logs/clock-in", "clockInByEmail"),
				mapping(HttpMethod.POST, "/api/v1/employees/by-email/" + EMAIL + "/time-logs/clock-out", "clockOutByEmail"),
				mapping(HttpMethod.POST, "/api/v1/employees/by-email/" + EMAIL + "/time-logs", "createTimeLogByEmail"),
				mapping(HttpMethod.GET, "/api/v1/employees/by-email/" + EMAIL + "/time-logs/" + TIME, "findTimeLogByEmail"),
				mapping(HttpMethod.DELETE, "/api/v1/employees/by-email/" + EMAIL + "/time-logs/" + TIME, "deleteTimeLogByEmail"),
				mapping(HttpMethod.GET, "/api/v1/employees/by-email/" + EMAIL + "/clock-out-without-clock-in-events/" + TIME,
						"findClockOutWithoutClockInEventByEmail"),
				mapping(HttpMethod.POST, "/api/v1/employees/by-email/" + EMAIL + "/clock-out-without-clock-in-events/" + TIME + "/resolve",
						"resolveClockOutWithoutClockInEventByEmail"),
				mapping(HttpMethod.POST, "/api/v1/employees/by-email/" + EMAIL + "/clock-out-without-clock-in-events/" + TIME + "/invalidate",
						"invalidateClockOutWithoutClockInEventByEmail"),
				mapping(HttpMethod.PUT, "/api/v1/worksites/WS-1/employees/by-email/" + EMAIL,
						"assignEmployeeToWorksiteByEmail"),
				mapping(HttpMethod.DELETE, "/api/v1/worksites/WS-1/employees/by-email/" + EMAIL,
						"removeEmployeeFromWorksiteByEmail"));
	}

	private static Mapping mapping(final HttpMethod method, final String path, final String handlerMethod) {
		return new Mapping(method, path, handlerMethod);
	}

	private record Mapping(HttpMethod method, String path, String handlerMethod) {
	}
}
