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

import java.util.Objects;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import es.nivel36.janus.api.Mapper;
import es.nivel36.janus.service.catalog.TimeZoneCatalogItem;
import es.nivel36.janus.service.catalog.TimeZoneCatalogService;

/**
 * REST controller exposing catalog endpoints.
 */
@RestController
public class CatalogController implements CatalogResource {

	private final TimeZoneCatalogService timeZoneCatalogService;
	private final Mapper<TimeZoneCatalogItem, TimeZoneCatalogItemResponse> timeZoneCatalogItemResponseMapper;

	/**
	 * Constructs a controller for time-zone catalog searches.
	 *
	 * @param  timeZoneCatalogService            service used to retrieve time zone
	 *                                           catalog data; must not be
	 *                                           {@code null}
	 * @param  timeZoneCatalogItemResponseMapper mapper converting catalog items to
	 *                                           API responses; must not be
	 *                                           {@code null}
	 * @throws NullPointerException              if any required dependency is
	 *                                           {@code null}
	 */
	public CatalogController(
		final TimeZoneCatalogService timeZoneCatalogService,
		final @Qualifier("timeZoneCatalogItemResponseMapper") Mapper<TimeZoneCatalogItem, TimeZoneCatalogItemResponse> timeZoneCatalogItemResponseMapper) {
		this.timeZoneCatalogService = Objects
				.requireNonNull(timeZoneCatalogService, "timeZoneCatalogService can't be null");
		this.timeZoneCatalogItemResponseMapper = Objects
				.requireNonNull(timeZoneCatalogItemResponseMapper, "timeZoneCatalogItemResponseMapper can't be null");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ResponseEntity<Page<TimeZoneCatalogItemResponse>> searchTimeZones(
			final String query,
			final Pageable pageable) {
		final Page<TimeZoneCatalogItemResponse> zones = this.timeZoneCatalogService.search(query, pageable)
				.map(this.timeZoneCatalogItemResponseMapper::map);
		return ResponseEntity.ok(zones);
	}
}
