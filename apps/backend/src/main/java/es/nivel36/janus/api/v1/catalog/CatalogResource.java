/*
 * Copyright 2026 Abel Ferrer Jiménez
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package es.nivel36.janus.api.v1.catalog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import es.nivel36.janus.validation.SearchQuery;

/**
 * Returns a page of time-zone catalog entries with their current UTC offsets.
 * <p>
 * A query matches zone identifiers case-insensitively and literally without
 * trimming; {@code null} or blank text disables filtering. Supplied queries
 * must satisfy {@link SearchQuery}. Supported sort fields are {@code level1},
 * {@code level2} and {@code utc}, with the last ordered by numeric offset. Zone
 * identifiers break ties.
 *
 * @param  query                    the optional zone-identifier fragment
 * @param  pageable                 the requested page and sort; defaults to
 *                                  page {@code 0}, size {@code 20}, and
 *                                  ascending {@code level1}
 * @return                          an HTTP {@code 200 OK} response containing
 *                                  the matching catalog page
 * @throws IllegalArgumentException if a sort property is unsupported
 */
/**
 * HTTP contract for reference catalogs at {@code /api/v1/catalogs}.
 * <p>
 * Access requires an authenticated identity with an existing application
 * profile and a recognized Janus role.
 */
@RequestMapping("/api/v1/catalogs")
public interface CatalogResource {
	@GetMapping("/time-zones")
	@PreAuthorize("@catalogAuthorization.canView(authentication)")
	ResponseEntity<Page<TimeZoneCatalogItemResponse>> searchTimeZones(
			@RequestParam(required = false)
			@SearchQuery
			String query,
			@PageableDefault(size = 20, sort = "level1")
			Pageable pageable);
}
