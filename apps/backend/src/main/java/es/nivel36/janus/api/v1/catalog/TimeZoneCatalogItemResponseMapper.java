package es.nivel36.janus.api.v1.catalog;

import org.springframework.stereotype.Component;

import es.nivel36.janus.api.Mapper;
import es.nivel36.janus.service.catalog.TimeZoneCatalogItem;

/**
 * Maps time-zone catalog items to API responses under the {@link Mapper}
 * contract.
 */
@Component
public class TimeZoneCatalogItemResponseMapper implements Mapper<TimeZoneCatalogItem, TimeZoneCatalogItemResponse> {

	@Override
	public TimeZoneCatalogItemResponse map(final TimeZoneCatalogItem timeZoneCatalogItem) {
		if (timeZoneCatalogItem == null) {
			return null;
		}
		final String literal = timeZoneCatalogItem.literal();
		final String level1 = timeZoneCatalogItem.level1();
		final String level2 = timeZoneCatalogItem.level2();
		final String utc = timeZoneCatalogItem.utc();
		return new TimeZoneCatalogItemResponse(literal, level1, level2, utc);
	}
}
