/*
 * Copyright 2025 Abel Ferrer Jiménez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package es.nivel36.janus.api.v1.worksite;

import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.nivel36.janus.api.Mapper;
import es.nivel36.janus.service.worksite.Worksite;
import es.nivel36.janus.service.worksite.WorksiteService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;

/**
 * REST controller responsible for exposing worksite CRUD operations.
 */
@RestController
@RequestMapping("/api/v1/workplace")
public class WorksiteController {

	private static final Logger logger = LoggerFactory.getLogger(WorksiteController.class);

	private final WorksiteService worksiteService;
	private final Mapper<Worksite, WorksiteResponse> worksiteResponseMapper;

        /**
         * Builds a controller for managing {@link Worksite} resources.
         *
         * @param worksiteService         application service that provides worksite
         *                                operations; must not be {@code null}
         * @param worksiteResponseMapper  mapper translating {@link Worksite} entities
         *                                into {@link WorksiteResponse} DTOs; must not be
         *                                {@code null}
         */
        public WorksiteController(final WorksiteService worksiteService,
                        final Mapper<Worksite, WorksiteResponse> worksiteResponseMapper) {
                this.worksiteService = Objects.requireNonNull(worksiteService, "WorksiteService can't be null");
                this.worksiteResponseMapper = Objects.requireNonNull(worksiteResponseMapper,
                                "WorksiteResponseMapper can't be null");
        }

	/**
	 * Retrieves all worksites registered in the system.
	 *
	 * @return a {@link ResponseEntity} containing the list of worksites
	 */
	@GetMapping
	public ResponseEntity<List<WorksiteResponse>> getWorksites() {
		logger.debug("List worksites ACTION performed");

		final List<Worksite> worksites = this.worksiteService.findAllWorksites();
		final List<WorksiteResponse> responses = worksites.stream().map(worksiteResponseMapper::map).toList();
		return ResponseEntity.ok(responses);
	}

	/**
	 * Retrieves a specific worksite by its unique code.
	 *
	 * @param worksiteCode the unique code of the worksite; must not be {@code null}
	 * @return a {@link ResponseEntity} containing the requested worksite
	 */
	@GetMapping("/{worksiteCode}")
	public ResponseEntity<WorksiteResponse> findWorksite( //
			final @PathVariable("worksiteCode") //
			@Pattern( //
					regexp = "[A-Za-z0-9_-]{1,50}", //
					message = "code must contain only letters, digits, underscores or hyphens (max 50)") //
					String worksiteCode) {
		logger.debug("Find worksite ACTION performed");

		final Worksite worksite = this.worksiteService.findWorksiteByCode(worksiteCode);
		final WorksiteResponse response = this.worksiteResponseMapper.map(worksite);
		return ResponseEntity.ok(response);
	}

	/**
	 * Creates a new worksite.
	 *
	 * @param request the payload describing the worksite to create; must not be
	 *                {@code null}
	 * @return a {@link ResponseEntity} containing the created worksite
	 */
	@PostMapping
	public ResponseEntity<WorksiteResponse> createWorksite(@Valid @RequestBody final CreateWorksiteRequest request) {
		logger.debug("Create worksite ACTION performed");

		final String code = request.code();
		final String name = request.name();
		final String timeZone = request.timeZone();
		final ZoneId timeZoneId = ZoneId.of(timeZone);

		final Worksite worksite = this.worksiteService.createWorksite(code, name, timeZoneId);
		final WorksiteResponse response = this.worksiteResponseMapper.map(worksite);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	/**
	 * Updates an existing worksite identified by its code.
	 *
	 * @param worksiteCode the code of the worksite to update; must not be
	 *                     {@code null}
	 * @param request      the payload describing the new worksite data; must not be
	 *                     {@code null}
	 * @return a {@link ResponseEntity} containing the updated worksite
	 */
	@PutMapping("/{worksiteCode}")
	public ResponseEntity<WorksiteResponse> updateWorksite(final @PathVariable("worksiteCode") //
	@Pattern(//
			regexp = "[A-Za-z0-9_-]{1,50}", //
			message = "code must contain only letters, digits, underscores or hyphens (max 50)") //
			String worksiteCode, //
			@Valid @RequestBody final UpdateWorksiteRequest request) {
		logger.debug("Update worksite ACTION performed");

		final String code = request.code();
		final String name = request.name();
		final String timeZone = request.timeZone();
		final ZoneId timeZoneId = ZoneId.of(timeZone);

		final Worksite worksite = this.worksiteService.updateWorksite(worksiteCode, code, name, timeZoneId);
		final WorksiteResponse response = this.worksiteResponseMapper.map(worksite);
		return ResponseEntity.ok(response);
	}

	/**
	 * Deletes the worksite identified by the given code.
	 *
	 * @param worksiteCode the unique code of the worksite; must not be {@code null}
	 * @return a {@link ResponseEntity} with an empty body and HTTP 204 status
	 */
	@DeleteMapping("/{worksiteCode}")
	public ResponseEntity<Void> deleteWorksite(final @PathVariable("worksiteCode") //
	@Pattern(//
			regexp = "[A-Za-z0-9_-]{1,50}", //
			message = "code must contain only letters, digits, underscores or hyphens (max 50)") //
			String worksiteCode) {
		logger.debug("Delete worksite ACTION performed");

		final Worksite workiste = this.worksiteService.findWorksiteByCode(worksiteCode);
		this.worksiteService.deleteWorksite(workiste);
		return ResponseEntity.noContent().build();
	}
}
