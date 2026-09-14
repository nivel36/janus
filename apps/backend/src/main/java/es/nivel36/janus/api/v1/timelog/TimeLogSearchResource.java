/*
 * Copyright 2026 Abel Ferrer Jiménez
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package es.nivel36.janus.api.v1.timelog;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.validation.constraints.Pattern;

@RequestMapping("/api/v1/timelogs")
public interface TimeLogSearchResource {

	@PreAuthorize("@timeLogAuthorization.canSearch(authentication)")
	@GetMapping({ "", "/" })
	ResponseEntity<Page<TimeLogResponse>> searchTimeLogs(
			@RequestParam(value = "employeeEmail", required = false) @Pattern(regexp = "^(?=.{1,254}$)[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "must be a valid and safe email address (max 254)") String employeeEmail,
			@RequestParam(value = "fromInstant", required = false) Instant fromInstant,
			@RequestParam(value = "toInstant", required = false) Instant toInstant,
			@PageableDefault(sort = "entryTime", direction = Sort.Direction.DESC) Pageable pageable,
			Authentication authentication);
}
