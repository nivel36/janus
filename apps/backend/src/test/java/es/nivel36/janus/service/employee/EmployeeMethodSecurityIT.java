/*
 * Copyright 2026 Abel Ferrer Jiménez
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package es.nivel36.janus.service.employee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import es.nivel36.janus.api.v1.SecurityTestConfiguration;

@SpringBootTest
@Import({ SecurityTestConfiguration.class, EmployeeMethodSecurityIT.CallerConfiguration.class })
@Transactional
@Sql(statements = {
		"INSERT INTO schedule(id,code,name) VALUES(1,'STD-WH','Standard')",
		"INSERT INTO employee(id,name,surname,email,schedule_id) VALUES(10,'Alice','One','alice@internal.test',1)",
		"INSERT INTO employee(id,name,surname,email,schedule_id) VALUES(11,'Bob','Two','bob@internal.test',1)",
		"INSERT INTO app_user(username,keycloak_subject,locale,time_format,default_timezone,employee_id) VALUES('alice','alice-subject','en-US','H24','UTC',10)" })
class EmployeeMethodSecurityIT {

	private @Autowired EmployeeService employeeService;
	private @Autowired EmployeeServiceCaller caller;

	@AfterEach
	void clearAuthentication() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void permitsOwnProfileThroughTheSpringProxyWhenCalledFromAnotherBean() {
		authenticate("alice-subject", "ROLE_JANUS_EMPLOYEE");

		assertThat(AopUtils.isAopProxy(this.employeeService)).isTrue();
		assertThat(this.caller.findByEmail("alice@internal.test").getId()).isEqualTo(10L);
	}

	@Test
	void deniesAnotherProfileBeforeTheServiceOperationRuns() {
		authenticate("alice-subject", "ROLE_JANUS_EMPLOYEE");

		assertThatThrownBy(() -> this.caller.findByEmail("bob@internal.test"))
				.isInstanceOf(AccessDeniedException.class);
	}

	private static void authenticate(final String subject, final String authority) {
		final Instant now = Instant.now();
		final Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject(subject).issuedAt(now)
				.expiresAt(now.plusSeconds(300)).build();
		SecurityContextHolder.getContext().setAuthentication(
				new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority(authority))));
	}

	@TestConfiguration
	static class CallerConfiguration {
		@Bean
		EmployeeServiceCaller employeeServiceCaller(final EmployeeService employeeService) {
			return new EmployeeServiceCaller(employeeService);
		}
	}

	static class EmployeeServiceCaller {
		private final EmployeeService employeeService;

		EmployeeServiceCaller(final EmployeeService employeeService) {
			this.employeeService = employeeService;
		}

		Employee findByEmail(final String email) {
			return this.employeeService.findEmployeeByEmail(email);
		}
	}
}
