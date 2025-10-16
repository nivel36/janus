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
package es.nivel36.janus.service;

import java.time.Duration;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA attribute converter for mapping {@link Duration} objects to their string
 * representations in the database and vice versa.
 * <p>
 * This converter stores {@link Duration} values as ISO-8601 strings (e.g.,
 * {@code "PT8H30M"}) and reconstructs them back when reading.
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
 * {@code "PT8H30M"}</li>
 * <li>{@code "PT8H30M"} → converted back to
 * {@code Duration.ofHours(8).plusMinutes(30)}</li>
 * </ul>
 */
@Converter(autoApply = true)
public class DurationConverter implements AttributeConverter<Duration, String> {

	/**
	 * Converts a {@link Duration} into its database column representation.
	 *
	 * @param duration the {@link Duration} to convert; may be {@code null}
	 * @return the ISO-8601 string (e.g., {@code "PT8H30M"}), or {@code null} if
	 *         input was {@code null}
	 */
	@Override
	public String convertToDatabaseColumn(Duration duration) {
		return (duration != null ? duration.toString() : null);
	}

	/**
	 * Converts a database column value into a {@link Duration}.
	 *
	 * @param dbData the ISO-8601 string stored in the database; may be {@code null}
	 * @return the corresponding {@link Duration} instance, or {@code null} if input
	 *         was {@code null}
	 */
	@Override
	public Duration convertToEntityAttribute(String dbData) {
		return (dbData != null ? Duration.parse(dbData) : null);
	}
}
