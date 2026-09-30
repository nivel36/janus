/*
 * Copyright 2026 Abel Ferrer Jiménez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package es.nivel36.janus.api.v1;

import static es.nivel36.janus.api.v1.SecurityTestConfiguration.verifiedJwt;
import static org.springframework.security.core.authority.AuthorityUtils.createAuthorityList;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
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

}
