/*
 * Copyright 2026 Abel Ferrer Jiménez
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package es.nivel36.janus.api.v1.timelog;

import io.swagger.v3.oas.annotations.responses.ApiResponse;

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

import jakarta.validation.Valid;

@RequestMapping({ "/api/v1/time-logs" })
public interface TimeLogSearchResource {

	@ApiResponse(responseCode = "200", description = "Search results within the authorized employee scope")
	@ApiResponse(responseCode = "403", description = "Employee association missing or employee filter outside the authorized scope")
	@PreAuthorize("@timeLogAuthorization.canSearch(authentication, #request.employeeNumber())")
	@GetMapping
	ResponseEntity<Page<TimeLogResponse>> searchTimeLogs( //
			@Valid @ModelAttribute TimeLogSearchRequest request, //
			@PageableDefault(sort = "entryTime", direction = Sort.Direction.DESC) Pageable pageable, //
			Authentication authentication);
}
