/*
 * Copyright 2026 Abel Ferrer Jiménez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */
package es.nivel36.janus.service.appuser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import es.nivel36.janus.api.v1.SecurityTestConfiguration;

@SpringBootTest
@Import(SecurityTestConfiguration.class)
class AppUserProvisioningIT {

	private static final String SUBJECT = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa";

	private @Autowired AppUserService appUserService;
	private @Autowired JdbcTemplate jdbcTemplate;

	@AfterEach
	void removeProvisionedUser() {
		this.jdbcTemplate.update("DELETE FROM app_user WHERE keycloak_subject = ?", SUBJECT);
	}

	@Test
	void concurrentFirstRequestsReturnTheSameRow() throws Exception {
		final CountDownLatch start = new CountDownLatch(1);
		try (var executor = Executors.newFixedThreadPool(2)) {
			final List<Future<AppUser>> requests = List.of(
					executor.submit(() -> provisionAfter(start, "concurrent-user-one")),
					executor.submit(() -> provisionAfter(start, "concurrent-user-two")));
			start.countDown();

			final AppUser first = requests.get(0).get();
			final AppUser second = requests.get(1).get();
			assertEquals(first.getId(), second.getId());
			assertEquals(1, this.jdbcTemplate.queryForObject(
					"SELECT COUNT(*) FROM app_user WHERE keycloak_subject = ?", Integer.class, SUBJECT));
		}
	}

	private AppUser provisionAfter(final CountDownLatch start, final String preferredUsername) throws InterruptedException {
		start.await();
		return this.appUserService.findOrCreateAppUser(SUBJECT, preferredUsername);
	}
}
