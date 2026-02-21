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
package es.nivel36.janus.api.v1.timelog;

/**
 * Response record representing a duration in multiple formats.
 * <p>
 * This DTO is designed to be returned by REST controllers when exposing a time
 * span to clients. It provides the duration broken down into hours, minutes,
 * and seconds, as well as the full ISO-8601 textual representation.
 * </p>
 *
 * @param hours   the total number of elapsed hours; may exceed 24
 * @param minutes the minute part of the duration, from {@code 0} to {@code 59}
 * @param seconds the second part of the duration, from {@code 0} to {@code 59}
 * @param iso8601 the ISO-8601 compliant string representation of the duration,
 *                e.g. {@code "PT4H30M15S"}
 */
public record DurationResponse(long hours, int minutes, int seconds, String iso8601) {
}
