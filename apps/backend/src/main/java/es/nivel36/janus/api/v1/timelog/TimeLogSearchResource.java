/*
 * Copyright 2026 Abel Ferrer Jiménez
 * Licensed under the Apache License, Version 2.0 (the "License");
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

import es.nivel36.janus.service.timelog.TimeLogSearchCriteria;
import jakarta.validation.Valid;

@RequestMapping({ "/api/v1/time-logs", "/api/v1/timelogs" })
public interface TimeLogSearchResource {

	@PreAuthorize("@timeLogAuthorization.canSearch(authentication)")
	@GetMapping({ "", "/" })
	ResponseEntity<Page<TimeLogResponse>> searchTimeLogs(
			@Valid @ModelAttribute TimeLogSearchCriteria criteria,
			@PageableDefault(sort = "entryTime", direction = Sort.Direction.DESC) Pageable pageable,
			Authentication authentication);
}
