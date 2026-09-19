/*
 * Copyright 2026 Abel Ferrer Jiménez
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package es.nivel36.janus.api.v1.timelog;

import java.time.Instant;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;

import es.nivel36.janus.api.Mapper;
import es.nivel36.janus.policy.timelog.TimeLogAuthorizationAdapter;
import es.nivel36.janus.service.timelog.TimeLog;
import es.nivel36.janus.service.timelog.TimeLogSearchCriteria;
import es.nivel36.janus.service.timelog.TimeLogService;

/**
 * REST controller exposing paginated time log search operations.
 */
@RestController
public class TimeLogSearchController implements TimeLogSearchResource {

	private final TimeLogService timeLogs;
	private final TimeLogAuthorizationAdapter authorization;
	private final Mapper<TimeLog, TimeLogResponse> mapper;

	/**
	 * Creates a controller for searching time logs.
	 *
	 * @param timeLogs      service used to search time logs; must not be {@code null}
	 * @param authorization component that determines the authenticated user's search
	 *                      scope; must not be {@code null}
	 * @param mapper        mapper converting time logs to API responses; must not be
	 *                      {@code null}
	 */
	public TimeLogSearchController(final TimeLogService timeLogs, final TimeLogAuthorizationAdapter authorization,
			@Qualifier("timeLogResponseMapper") final Mapper<TimeLog, TimeLogResponse> mapper) {
		this.timeLogs = Objects.requireNonNull(timeLogs);
		this.authorization = Objects.requireNonNull(authorization);
		this.mapper = Objects.requireNonNull(mapper);
	}

	/**
	 * Searches time logs using the requested filters and the authenticated user's
	 * authorization scope.
	 *
	 * @param employeeEmail   optional employee email filter
	 * @param fromInstant     optional lower bound for the time range
	 * @param toInstant       optional upper bound for the time range
	 * @param pageable        pagination and sorting information; must not be
	 *                        {@code null}
	 * @param authentication current authentication; must not be {@code null}
	 * @return a page of matching time log responses
	 */
	@Override
	public ResponseEntity<Page<TimeLogResponse>> searchTimeLogs(final String employeeEmail, final Instant fromInstant,
			final Instant toInstant, final Pageable pageable, final Authentication authentication) {
		final TimeLogSearchCriteria criteria = new TimeLogSearchCriteria(employeeEmail, fromInstant, toInstant);
		return ResponseEntity.ok(this.timeLogs.searchTimeLogs(criteria, this.authorization.searchScope(authentication),
				pageable).map(this.mapper::map));
	}
}
