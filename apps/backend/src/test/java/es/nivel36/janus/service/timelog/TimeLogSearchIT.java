/*
 * Copyright 2026 Abel Ferrer Jiménez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */
package es.nivel36.janus.service.timelog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import es.nivel36.janus.api.v1.SecurityTestConfiguration;

@SpringBootTest
@Import(SecurityTestConfiguration.class)
@Transactional
@Sql("/sql/timelog-search.sql")
class TimeLogSearchIT {

	private @Autowired TimeLogService service;

	@Test
	void searchRequiresExplicitScope() {
		assertThatNullPointerException().isThrownBy(() -> this.service.searchTimeLogs(
				new TimeLogSearchCriteria(null, null, null), null, PageRequest.of(0, 2)));
	}

	@Test
	void noneScopeReturnsNoRecordsOrCountEvenBeyondFirstPage() {
		for (int page : new int[] { 0, 3 }) {
			final Page<TimeLog> result = this.service.searchTimeLogs(new TimeLogSearchCriteria(null, null, null),
					new TimeLogSearchScope.None(), PageRequest.of(page, 2));
			assertThat(result.getContent()).isEmpty();
			assertThat(result.getTotalElements()).isZero();
			assertThat(result.getTotalPages()).isZero();
		}
	}

	@Test
	void scopeAndClientEmployeeFilterAreCombined() {
		final TimeLogSearchCriteria criteria = new TimeLogSearchCriteria("bob@example.test", null, null);
		final PageRequest page = PageRequest.of(0, 2, Sort.by("entryTime"));

		final Page<TimeLog> allowed = this.service.searchTimeLogs(criteria, new TimeLogSearchScope.All(), page);
		assertThat(allowed.getContent()).hasSize(2)
				.allSatisfy(timeLog -> assertThat(timeLog.getEmployee().getId()).isEqualTo(102L));
		assertThat(allowed.getTotalElements()).isEqualTo(4);

		final Page<TimeLog> denied = this.service.searchTimeLogs(criteria, new TimeLogSearchScope.Employee(101L),
				page);
		assertThat(denied.getContent()).isEmpty();
		assertThat(denied.getTotalElements()).isZero();
	}

	@Test
	void employeeScopeAndDateCriteriaRestrictContentAndCountBeforePagination() {
		final TimeLogSearchCriteria criteria = new TimeLogSearchCriteria(null,
				Instant.parse("2025-07-02T08:00:00Z"), Instant.parse("2025-07-04T08:00:00Z"));
		final Page<TimeLog> result = this.service.searchTimeLogs(criteria, new TimeLogSearchScope.Employee(101L),
				PageRequest.of(1, 1, Sort.by("entryTime")));

		assertThat(result.getContent()).extracting(TimeLog::getEntryTime)
				.containsExactly(Instant.parse("2025-07-03T08:00:00Z"));
		assertThat(result.getTotalElements()).isEqualTo(2);
		assertThat(result.getTotalPages()).isEqualTo(2);
	}
}
