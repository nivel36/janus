/*
 * Copyright 2026 Abel Ferrer Jiménez Licensed under the Apache License, Version
 * 2.0 (the "License");
 */
package es.nivel36.janus.api.v1.timelog;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * Spring MVC implementation of {@link TimeLogSearchResource}.
 */
@RestController
public class TimeLogSearchController implements TimeLogSearchResource {

	private static final Logger logger = LoggerFactory.getLogger(TimeLogSearchController.class);

	private final TimeLogService timeLogs;
	private final TimeLogAuthorizationAdapter authorization;
	private final Mapper<TimeLog, TimeLogResponse> timeLogResponseMapper;

	/**
	 * Creates a controller ready to delegate searches without accessing
	 * persistence.
	 *
	 * @param  timeLogs              nonnull time-log service
	 * @param  authorization         nonnull component resolving the authenticated
	 *                               search scope
	 * @param  timeLogResponseMapper nonnull response mapper
	 * @throws NullPointerException  if any dependency is null
	 */
	public TimeLogSearchController(
		final TimeLogService timeLogs,
		final TimeLogAuthorizationAdapter authorization,
		final @Qualifier("timeLogResponseMapper") Mapper<TimeLog, TimeLogResponse> timeLogResponseMapper) {
		this.timeLogs = Objects.requireNonNull(timeLogs, "timeLogs can't be null");
		this.authorization = Objects.requireNonNull(authorization, "authorization can't be null");
		this.timeLogResponseMapper = Objects
				.requireNonNull(timeLogResponseMapper, "timeLogResponseMapper can't be null");
	}

	@Override
	public ResponseEntity<Page<TimeLogResponse>> searchTimeLogs(
			final TimeLogSearchRequest request,
			final Pageable pageable,
			final Authentication authentication) {
		logger.debug("Search time logs ACTION performed");
		final TimeLogSearchScope searchScope = this.authorization.searchScope(authentication);
		final TimeLogSearchCriteria criteria = new TimeLogSearchCriteria(
				request.employeeNumber(),
				request.start(),
				request.end());
		final Page<TimeLogResponse> response = this.timeLogs.searchTimeLogs(criteria, searchScope, pageable)
				.map(this.timeLogResponseMapper::map);
		return ResponseEntity.ok(response);
	}
}
