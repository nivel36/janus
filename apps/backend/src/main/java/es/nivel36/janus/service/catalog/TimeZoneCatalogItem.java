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

/**
 * Internal projection representing a single time zone catalog row.
 *
 * @param zoneId  full Java zone id
 * @param literal full display value, for example {@code Europe/Madrid (UTC+2)}
 * @param level1  first segment of the zone id (before the first slash)
 * @param level2  remainder of the zone id after the first slash
 * @param utc     UTC offset string used in the literal
 */
public record TimeZoneCatalogItem(String zoneId, String literal, String level1, String level2, String utc) {

}
