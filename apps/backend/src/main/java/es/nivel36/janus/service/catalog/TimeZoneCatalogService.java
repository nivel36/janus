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
package es.nivel36.janus.service.catalog;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Application service that exposes catalog data for Java time zones.
 */
@Service
public class TimeZoneCatalogService {

	/**
	 * Searches available Java {@link ZoneId} values and returns a paginated catalog.
	 * <p>
	 * Search is a simple case-insensitive {@code contains} over the full zone id
	 * string.
	 * </p>
	 *
	 * @param search   optional search text applied to the full zone id
	 * @param sortBy   sort strategy for the resulting catalog page
	 * @param pageable Spring pagination information (page and size)
	 * @return a page of catalog items matching the input filters
	 */
	public Page<TimeZoneCatalogItem> search(final String search, final TimeZoneSortBy sortBy, final Pageable pageable) {
		Objects.requireNonNull(sortBy, "sortBy can't be null");
		Objects.requireNonNull(pageable, "pageable can't be null");

		final String normalizedSearch = search == null ? null : search.trim().toLowerCase(Locale.ROOT);
		final ZonedDateTime now = ZonedDateTime.now();

		final List<TimeZoneCatalogItem> filtered = ZoneId.getAvailableZoneIds().stream().sorted().map(zoneId -> map(zoneId, now))
				.filter(item -> normalizedSearch == null || normalizedSearch.isBlank()
						|| item.zoneId().toLowerCase(Locale.ROOT).contains(normalizedSearch))
				.sorted(resolveSort(sortBy))
				.toList();

		final int start = Math.toIntExact(pageable.getOffset());
		if (start >= filtered.size()) {
			return new PageImpl<>(List.of(), pageable, filtered.size());
		}

		final int end = Math.min(start + pageable.getPageSize(), filtered.size());
		return new PageImpl<>(filtered.subList(start, end), pageable, filtered.size());
	}

	private TimeZoneCatalogItem map(final String zoneId, final ZonedDateTime referenceDateTime) {
		final ZoneId parsedZoneId = ZoneId.of(zoneId);
		final int offsetSeconds = referenceDateTime.withZoneSameInstant(parsedZoneId).getOffset().getTotalSeconds();
		final String utc = formatUtcOffset(offsetSeconds);
		final int splitIndex = zoneId.indexOf('/');
		final String level1 = splitIndex >= 0 ? zoneId.substring(0, splitIndex) : zoneId;
		// Keep everything after the first slash as the second level, including
		// additional nested segments such as "Argentina/Buenos_Aires".
		final String level2 = splitIndex >= 0 ? zoneId.substring(splitIndex + 1) : "";
		final String literal = zoneId + " (" + utc + ")";
		return new TimeZoneCatalogItem(zoneId, literal, level1, level2, utc, offsetSeconds);
	}

	private String formatUtcOffset(final int totalSeconds) {
		final int absTotalSeconds = Math.abs(totalSeconds);
		final int hours = absTotalSeconds / 3600;
		final int minutes = (absTotalSeconds % 3600) / 60;
		final String sign = totalSeconds >= 0 ? "+" : "-";
		if (minutes == 0) {
			return "UTC" + sign + hours;
		}
		return "UTC" + sign + hours + ":" + String.format("%02d", minutes);
	}

	private Comparator<TimeZoneCatalogItem> resolveSort(final TimeZoneSortBy sortBy) {
		return switch (sortBy) {
		case LEVEL1 -> Comparator.comparing(TimeZoneCatalogItem::level1)
				.thenComparing(TimeZoneCatalogItem::zoneId);
		case UTC -> Comparator.comparingInt(TimeZoneCatalogItem::offsetSeconds)
				.thenComparing(TimeZoneCatalogItem::zoneId);
		};
	}
}
