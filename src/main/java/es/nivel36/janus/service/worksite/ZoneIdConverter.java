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
package es.nivel36.janus.service.worksite;

import java.time.ZoneId;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA attribute converter for mapping {@link ZoneId} objects to their string
 * representations in the database and vice versa.
 * <p>
 * This converter ensures that {@link ZoneId} values are stored as their textual
 * identifiers (e.g., {@code "Europe/Madrid"}) in database columns and restored
 * back into {@link ZoneId} instances when reading from the database.
 * </p>
 *
 * <p>
 * It is annotated with {@link Converter}(autoApply = true), which means it will
 * be automatically applied to all entity attributes of type {@link ZoneId}
 * without requiring explicit configuration.
 * </p>
 *
 * <p><b>Example:</b></p>
 * <ul>
 *   <li>{@code ZoneId.of("Europe/Madrid")} → stored as {@code "Europe/Madrid"}</li>
 *   <li>{@code "America/New_York"} → converted back to {@code ZoneId.of("America/New_York")}</li>
 * </ul>
 */
@Converter(autoApply = true)
public class ZoneIdConverter implements AttributeConverter<ZoneId, String> {

    /**
     * Converts a {@link ZoneId} into its database column representation.
     *
     * @param zoneId the {@link ZoneId} to convert; may be {@code null}
     * @return the string identifier of the zone (e.g., {@code "Europe/Madrid"}),
     *         or {@code null} if the input was {@code null}
     */
    @Override
    public String convertToDatabaseColumn(ZoneId zoneId) {
        return (zoneId != null ? zoneId.getId() : null);
    }

    /**
     * Converts a database column value into a {@link ZoneId}.
     *
     * @param dbData the string identifier of the zone as stored in the database;
     *               may be {@code null}
     * @return the corresponding {@link ZoneId} instance, or {@code null} if
     *         the input was {@code null}
     */
    @Override
    public ZoneId convertToEntityAttribute(String dbData) {
        return (dbData != null ? ZoneId.of(dbData) : null);
    }
}
