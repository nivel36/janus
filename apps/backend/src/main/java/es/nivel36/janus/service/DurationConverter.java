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
package es.nivel36.janus.service;

import java.time.Duration;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converts JPA duration attributes to stored whole seconds and back.
 * <p>
 * The seconds component is obtained from {@link Duration#getSeconds()};
 * fractional nanoseconds are discarded. {@code null} values remain {@code null}
 * in both directions.
 */
@Converter(autoApply = true)
public class DurationConverter implements AttributeConverter<Duration, Long> {

	/**
	 * Returns the duration's seconds component for storage.
	 *
	 * @param  duration the duration to convert, or {@code null}
	 * @return          the seconds component without fractional nanoseconds, or
	 *                  {@code null} if the input is {@code null}
	 */
	@Override
	public Long convertToDatabaseColumn(final Duration duration) {
		return duration != null ? duration.getSeconds() : null;
	}

	/**
	 * Converts a database column value into a {@link Duration}.
	 *
	 * @param  dbData the total number of seconds stored in the database; may be
	 *                {@code null}
	 * @return        the corresponding {@link Duration} instance, or {@code null}
	 *                if input was {@code null}
	 */
	@Override
	public Duration convertToEntityAttribute(final Long dbData) {
		return dbData != null ? Duration.ofSeconds(dbData) : null;
	}
}
