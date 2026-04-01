/*
 * Copyright 2026 Abel Ferrer Jiménez
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
package es.nivel36.janus.api.v1.catalog;

import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import es.nivel36.janus.service.catalog.TimeZoneCatalogItem;
import es.nivel36.janus.service.catalog.TimeZoneCatalogService;
import es.nivel36.janus.service.catalog.TimeZoneSortBy;

/**
 * REST controller exposing catalog endpoints.
 */
@RestController
@RequestMapping("/api/v1/catalogs")
public class CatalogController {

	private final TimeZoneCatalogService timeZoneCatalogService;

	/**
	 * Builds a controller with the required catalog service dependency.
	 *
	 * @param timeZoneCatalogService service used to retrieve time zone catalog data
	 */
	public CatalogController(final TimeZoneCatalogService timeZoneCatalogService) {
		this.timeZoneCatalogService = Objects.requireNonNull(timeZoneCatalogService,
				"timeZoneCatalogService can't be null");
	}

	/**
	 * Returns a paginated list of Java time zones with formatted literal and split
	 * levels.
	 *
	 * @param search   optional search text over full zone id values
	 * @param sortBy   sorting mode ({@code LEVEL1} or {@code UTC})
	 * @param pageable Spring pagination information
	 * @return a page with matching time zone catalog items
	 */
	@PreAuthorize("hasAnyRole('JANUS_EMPLOYEE', 'JANUS_USER', 'JANUS_ADMIN')")
	@GetMapping("/time-zones")
	public ResponseEntity<Page<TimeZoneCatalogItemResponse>> searchTimeZones(
			@RequestParam(value = "search", required = false) final String search,
			@RequestParam(value = "sortBy", defaultValue = "LEVEL1") final TimeZoneSortBy sortBy,
			final @PageableDefault(size = 25) Pageable pageable) {
		final Page<TimeZoneCatalogItem> zones = this.timeZoneCatalogService.search(search, sortBy, pageable);
		final Page<TimeZoneCatalogItemResponse> response = zones.map(this::map);
		return ResponseEntity.ok(response);
	}

	private TimeZoneCatalogItemResponse map(final TimeZoneCatalogItem timeZoneCatalogItem) {
		return new TimeZoneCatalogItemResponse(timeZoneCatalogItem.literal(), timeZoneCatalogItem.level1(),
				timeZoneCatalogItem.level2(), timeZoneCatalogItem.utc());
	}
}
