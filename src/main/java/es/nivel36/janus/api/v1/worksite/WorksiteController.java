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
        public ResponseEntity<WorksiteResponse> getWorksite(
                        final @PathVariable("worksiteCode") @Pattern(regexp = "[A-Za-z0-9_-]{1,50}") String worksiteCode) {
                Objects.requireNonNull(worksiteCode, "WorksiteCode can't be null");
                logger.debug("Get worksite ACTION performed for code {}", worksiteCode);

                final Worksite worksite = this.worksiteService.getWorksiteByCode(worksiteCode);
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
                Objects.requireNonNull(request, "CreateWorksiteRequest can't be null");
                logger.debug("Create worksite ACTION performed for code {}", request.code());

                final String code = Objects.requireNonNull(request.code(), "Code can't be null");
                final String name = Objects.requireNonNull(request.name(), "Name can't be null");
                final String timeZoneId = Objects.requireNonNull(request.timeZone(), "TimeZone can't be null");
                final ZoneId timeZone = ZoneId.of(timeZoneId);

                final Worksite worksite = this.worksiteService.createWorksite(code, name, timeZone);
                final WorksiteResponse response = this.worksiteResponseMapper.map(worksite);
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

        /**
         * Updates an existing worksite identified by its code.
         *
         * @param worksiteCode the code of the worksite to update; must not be
         *                     {@code null}
         * @param request      the payload describing the new worksite data; must not
         *                     be {@code null}
         * @return a {@link ResponseEntity} containing the updated worksite
         */
        @PutMapping("/{worksiteCode}")
        public ResponseEntity<WorksiteResponse> updateWorksite(
                        final @PathVariable("worksiteCode") @Pattern(regexp = "[A-Za-z0-9_-]{1,50}") String worksiteCode,
                        @Valid @RequestBody final UpdateWorksiteRequest request) {
                Objects.requireNonNull(worksiteCode, "WorksiteCode can't be null");
                Objects.requireNonNull(request, "UpdateWorksiteRequest can't be null");
                logger.debug("Update worksite ACTION performed for code {}", worksiteCode);

                final String name = Objects.requireNonNull(request.name(), "Name can't be null");
                final String timeZoneId = Objects.requireNonNull(request.timeZone(), "TimeZone can't be null");
                final ZoneId timeZone = ZoneId.of(timeZoneId);

                final Worksite worksite = this.worksiteService.updateWorksite(worksiteCode, name, timeZone);
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
        public ResponseEntity<Void> deleteWorksite(
                        final @PathVariable("worksiteCode") @Pattern(regexp = "[A-Za-z0-9_-]{1,50}") String worksiteCode) {
                Objects.requireNonNull(worksiteCode, "WorksiteCode can't be null");
                logger.debug("Delete worksite ACTION performed for code {}", worksiteCode);

                this.worksiteService.deleteWorksite(worksiteCode);
                return ResponseEntity.noContent().build();
        }
}
