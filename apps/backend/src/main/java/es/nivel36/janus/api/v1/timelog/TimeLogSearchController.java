/*
 * Copyright 2026 Abel Ferrer Jiménez
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package es.nivel36.janus.api.v1.timelog;

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
import es.nivel36.janus.service.timelog.TimeLogSearchScope;
import es.nivel36.janus.service.timelog.TimeLogService;

/**
 * REST controller exposing paginated time log search operations.
 */
@RestController
public class TimeLogSearchController implements TimeLogSearchResource {

	private final TimeLogService timeLogs;
	private final TimeLogAuthorizationAdapter authorization;
	private final Mapper<TimeLog, TimeLogResponse> timeLogResponseMapper;

	/**
	 * Creates a controller for searching time logs.
	 *
	 * @param timeLogs              service used to search time logs; must not be
	 *                              {@code null}
	 * @param authorization         component that determines the authenticated
	 *                              user's search scope; must not be {@code null}
	 * @param timeLogResponseMapper timeLogResponseMapper converting time logs to
	 *                              API responses; must not be {@code null}
	 */
	public TimeLogSearchController( //
			final TimeLogService timeLogs, //
			final TimeLogAuthorizationAdapter authorization, //
			@Qualifier("timeLogResponseMapper") final Mapper<TimeLog, TimeLogResponse> timeLogResponseMapper) {
		this.timeLogs = Objects.requireNonNull(timeLogs);
		this.authorization = Objects.requireNonNull(authorization);
		this.timeLogResponseMapper = Objects.requireNonNull(timeLogResponseMapper);
	}

	/**
	 * Searches time logs using the requested filters and the authenticated user's
	 * authorization scope.
	 *
	 * @param request        validated optional filters; start and end must either
	 *                       both be present or both be absent
	 * @param pageable       pagination and sorting information; must not be
	 *                       {@code null}
	 * @param authentication current authentication; must not be {@code null}
	 * @return a page of matching time log responses
	 */
	@Override
	public ResponseEntity<Page<TimeLogResponse>> searchTimeLogs(//
			final TimeLogSearchRequest request, //
			final Pageable pageable, //
			final Authentication authentication) {
		final TimeLogSearchScope searchScope = this.authorization.searchScope(authentication);
		final TimeLogSearchCriteria criteria = new TimeLogSearchCriteria(request.employeeNumber(), request.start(),
				request.end());
		final Page<TimeLogResponse> response = this.timeLogs //
				.searchTimeLogs(criteria, searchScope, pageable) //
				.map(this.timeLogResponseMapper::map);
		return ResponseEntity.ok(response);
	}
}
