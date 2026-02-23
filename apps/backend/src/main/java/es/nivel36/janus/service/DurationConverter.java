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
package es.nivel36.janus.service;

import java.time.Duration;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA attribute converter for mapping {@link Duration} objects to their numeric
 * representation in the database and vice versa.
 *
 * <p>
 * This converter stores {@link Duration} values as the total number of seconds
 * in a {@code BIGINT} column and reconstructs them back when reading.
 * </p>
 *
 * <p>
 * Annotated with {@link Converter}(autoApply = true) to apply automatically to
 * all entity attributes of type {@link Duration}.
 * </p>
 *
 * <p>
 * <b>Example:</b>
 * </p>
 * <ul>
 * <li>{@code Duration.ofHours(8).plusMinutes(30)} → stored as
 * {@code 30600}</li>
 * <li>{@code 30600} → converted back to
 * {@code Duration.ofHours(8).plusMinutes(30)}</li>
 * </ul>
 */
@Converter(autoApply = true)
public class DurationConverter implements AttributeConverter<Duration, Long> {

	/**
	 * Converts a {@link Duration} into its database column representation.
	 *
	 * @param duration the {@link Duration} to convert; may be {@code null}
	 * @return the total number of seconds represented by the duration, or
	 *         {@code null} if input was {@code null}
	 */
	@Override
	public Long convertToDatabaseColumn(Duration duration) {
		return duration != null ? duration.getSeconds() : null;
	}

	/**
	 * Converts a database column value into a {@link Duration}.
	 *
	 * @param dbData the total number of seconds stored in the database; may be
	 *               {@code null}
	 * @return the corresponding {@link Duration} instance, or {@code null} if input
	 *         was {@code null}
	 */
	@Override
	public Duration convertToEntityAttribute(Long dbData) {
		return dbData != null ? Duration.ofSeconds(dbData) : null;
	}
}
