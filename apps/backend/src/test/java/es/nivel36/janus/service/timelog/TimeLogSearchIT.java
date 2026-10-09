/*
 * Copyright 2026 Abel Ferrer Jiménez
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 */
package es.nivel36.janus.service.timelog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import jakarta.validation.ConstraintViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;

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
	private @Autowired JdbcTemplate jdbc;
	private @Value("${spring.data.rest.max-page-size}") int maxPageSize;

	@Test
	void searchRequiresExplicitScope() {
		assertThatThrownBy(
				() -> this.service
						.searchTimeLogs(new TimeLogSearchCriteria(null, null, null), null, PageRequest.of(0, 2)))
				.isInstanceOf(ConstraintViolationException.class);
	}

	@Test
	void noneScopeReturnsNoRecordsOrCountEvenBeyondFirstPage() {
		for (final int page : new int[] { 0, 3 }) {
			final Page<TimeLog> result = this.service.searchTimeLogs(
					new TimeLogSearchCriteria(null, null, null),
					new TimeLogSearchScope.None(),
					PageRequest.of(page, 2));
			assertThat(result.getContent()).isEmpty();
			assertThat(result.getTotalElements()).isZero();
			assertThat(result.getTotalPages()).isZero();
		}
	}

	@Test
	void scopeAndClientEmployeeFilterAreCombined() {
		final TimeLogSearchCriteria criteria = new TimeLogSearchCriteria("EMP-0102", null, null);
		final PageRequest page = PageRequest.of(0, 2, Sort.by("entryTime"));

		final Page<TimeLog> allowed = this.service.searchTimeLogs(criteria, new TimeLogSearchScope.All(), page);
		assertThat(allowed.getContent()).hasSize(2)
				.allSatisfy(timeLog -> assertThat(timeLog.getEmployee().getId()).isEqualTo(102L));
		assertThat(allowed.getTotalElements()).isEqualTo(4);

		final Page<TimeLog> denied = this.service.searchTimeLogs(criteria, new TimeLogSearchScope.Employee(101L), page);
		assertThat(denied.getContent()).isEmpty();
		assertThat(denied.getTotalElements()).isZero();
	}

	@Test
	void employeeScopeAndDateCriteriaRestrictContentAndCountBeforePagination() {
		final TimeLogSearchCriteria criteria = new TimeLogSearchCriteria(
				null,
				Instant.parse("2025-07-02T08:00:00Z"),
				Instant.parse("2025-07-04T08:00:00Z"));
		final Page<TimeLog> result = this.service.searchTimeLogs(
				criteria,
				new TimeLogSearchScope.Employee(101L),
				PageRequest.of(1, 1, Sort.by("entryTime")));

		assertThat(result.getContent()).extracting(TimeLog::getEntryTime)
				.containsExactly(Instant.parse("2025-07-03T08:00:00Z"));
		assertThat(result.getTotalElements()).isEqualTo(2);
		assertThat(result.getTotalPages()).isEqualTo(2);
	}

	@Test
	void searchCapsSizeAndDefaultsToDescendingEntryTime() {
		final Page<TimeLog> result = this.service.searchTimeLogs(
				new TimeLogSearchCriteria(null, null, null),
				new TimeLogSearchScope.All(),
				PageRequest.of(0, this.maxPageSize + 1));
		assertThat(result.getSize()).isEqualTo(this.maxPageSize);
		assertThat(result.getContent()).extracting(TimeLog::getEntryTime)
				.isSortedAccordingTo(java.util.Comparator.reverseOrder());
		assertThat(result.getTotalElements()).isEqualTo(11);
	}

	@Test
	void equalEntryTimesHaveStableIdOrderingAcrossPages() {
		this.jdbc.update(
				"UPDATE time_log SET entry_time = (SELECT entry_time FROM time_log WHERE id = 102) WHERE id IN (101, 103)");
		for (int page = 0; page < 3; page++) {
			final Page<TimeLog> result = this.service.searchTimeLogs(
					new TimeLogSearchCriteria(null, null, null),
					new TimeLogSearchScope.All(),
					PageRequest.of(page, 1, Sort.by("entryTime")));
			assertThat(result.getContent()).extracting(TimeLog::getId).containsExactly(101L + page);
			assertThat(result.getTotalElements()).isEqualTo(11);
		}
	}

	@Test
	void publicAssociationSortFieldsAreResolved() {
		final Page<TimeLog> result = this.service.searchTimeLogs(
				new TimeLogSearchCriteria(null, null, null),
				new TimeLogSearchScope.All(),
				PageRequest.of(0, 20, Sort.by("employeeNumber", "worksiteCode")));
		assertThat(result.getContent()).extracting(log -> log.getEmployee().getEmployeeNumber()).isSorted();
		assertThat(result.getContent()).extracting(TimeLog::getId)
				.containsExactly(102L, 105L, 107L, 110L, 111L, 101L, 104L, 106L, 109L, 103L, 108L);
	}

	@Test
	void unpagedAndUnsupportedSortsAreRejected() {
		final TimeLogSearchCriteria criteria = new TimeLogSearchCriteria(null, null, null);
		for (final Pageable pageable : new Pageable[] { Pageable.unpaged(),
				PageRequest.of(0, 2, Sort.by("employee.email")), PageRequest.of(0, 2, Sort.by("missing")) }) {
			assertThatThrownBy(() -> this.service.searchTimeLogs(criteria, new TimeLogSearchScope.All(), pageable))
					.isInstanceOf(IllegalArgumentException.class);
		}
	}

	@Test
	void directServiceCallsRejectIncompleteAndNonIncreasingRanges() {
		final Instant start = Instant.parse("2025-07-02T08:00:00Z");
		for (final TimeLogSearchCriteria criteria : new TimeLogSearchCriteria[] {
				new TimeLogSearchCriteria(null, start, null), new TimeLogSearchCriteria(null, null, start),
				new TimeLogSearchCriteria(null, start, start),
				new TimeLogSearchCriteria(null, start, start.minusSeconds(1)) }) {
			assertThatThrownBy(
					() -> this.service.searchTimeLogs(criteria, new TimeLogSearchScope.All(), PageRequest.of(0, 2)))
					.isInstanceOf(IllegalArgumentException.class);
		}
	}

	@Test
	void directServiceCallsValidateEmployeeNumber() {
		assertThatThrownBy(
				() -> this.service.searchTimeLogs(
						new TimeLogSearchCriteria(" EMP-0101 ", null, null),
						new TimeLogSearchScope.All(),
						PageRequest.of(0, 2)))
				.isInstanceOf(ConstraintViolationException.class);
	}

}
