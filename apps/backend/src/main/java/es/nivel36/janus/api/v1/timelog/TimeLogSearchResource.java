/*
 * Copyright 2026 Abel Ferrer Jiménez Licensed under the Apache License, Version
 * 2.0 (the "License");
 */
package es.nivel36.janus.api.v1.timelog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.security.access.AccessDeniedException;
import jakarta.validation.Valid;

/**
 * HTTP contract for paginated, authorized time-log searches. Requires a
 * validated bearer JWT and an existing local actor; searching never provisions
 * an account.
 */
@RequestMapping({ "/api/v1/time-logs" })
public interface TimeLogSearchResource {

	/**
	 * Returns active time logs within the caller's authorized employee scope.
	 * <p>
	 * JANUS_ADMIN and JANUS_USER may search all employees. JANUS_EMPLOYEE requires
	 * a persistent employee association and may only filter by that employee.
	 * Employee numbers match exactly without trimming and must satisfy
	 * {@code [A-Za-z0-9_-]{1,50}}. Start and end must both be omitted or supplied
	 * with start strictly before end. Entry times include start and exclude end.
	 * </p>
	 * <p>
	 * Returns HTTP 200, possibly empty. Filters combine with AND and restrict both
	 * content and totals before pagination. Page size is capped at configured
	 * {@code spring.data.rest.max-page-size} (100 by default). Public sort fields
	 * are id, entryTime, exitTime, employeeNumber and worksiteCode. Default
	 * ordering is descending entryTime with ascending id as a tie-breaker unless
	 * explicitly sorted. No records are modified.
	 * </p>
	 *
	 * @param  request                  validated optional filters
	 * @param  pageable                 requested page and ordering; HTTP defaults
	 *                                  are page 0, size 20
	 * @param  authentication           trusted authentication used to resolve
	 *                                  persistent scope
	 * @return                          HTTP 200 containing time-log responses and
	 *                                  page metadata
	 * @throws IllegalArgumentException if paging or a sort field is unsupported
	 * @throws AccessDeniedException    if actor is unprovisioned, lacks a search
	 *                                  role, has no required employee association
	 *                                  or filters outside the authorized scope
	 */
	@GetMapping
	@PreAuthorize("@timeLogAuthorization.canSearch(authentication, #request.employeeNumber())")
	ResponseEntity<Page<TimeLogResponse>> searchTimeLogs(
			@ModelAttribute
			@Valid
			TimeLogSearchRequest request,
			@PageableDefault(size = 20, sort = "entryTime", direction = Sort.Direction.DESC)
			Pageable pageable,
			Authentication authentication);
}
