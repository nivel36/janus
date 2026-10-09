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
package es.nivel36.janus.api.v1.timelog;

/**
 * Response payload representing a duration as numeric parts and ISO-8601 text.
 * <p>
 * The duration mapper uses total hours and the remaining minute and second
 * parts. For negative durations these numeric values may be negative.
 * Fractional seconds are retained in the text representation.
 *
 * @param hours   the total whole hours; may exceed {@code 24} in magnitude
 * @param minutes the remaining minute part, from {@code -59} to {@code 59}
 * @param seconds the remaining second part, from {@code -59} to {@code 59}
 * @param iso8601 the ISO-8601 duration representation, including fractional
 *                seconds
 */
public record DurationResponse(long hours, int minutes, int seconds, String iso8601) {
}
