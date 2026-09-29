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
package es.nivel36.janus.api.v1.applicationsettings;

import com.fasterxml.jackson.annotation.JsonAlias;

import es.nivel36.janus.service.applicationsettings.ApplicationSettings;

/**
 * Response payload that represents global {@link ApplicationSettings} values.
 * Serialization always emits the canonical record component names. The
 * obsolete {@code employeeWorkplaceCreationAllowed} and
 * {@code employeeManualTimelogEntryAllowed} names are temporary
 * deserialization-only aliases.
 *
 * @param daysUntilLocked                   number of days a time log remains
 *                                          editable
 * @param employeeWorksiteCreationAllowed   whether employees can create
 *                                          personal worksites
 * @param worksiteChangeDuringShiftAllowed  whether changing worksite during a
 *                                          shift is allowed
 * @param employeeManualTimeLogEntryAllowed whether employees can set custom
 *                                          entry/exit instants in timelog
 *                                          operations
 * @param defaultTimezone                   IANA time zone identifier used as
 *                                          default
 */
public record ApplicationSettingsResponse( //
		int daysUntilLocked, //
		@JsonAlias("employeeWorkplaceCreationAllowed") boolean employeeWorksiteCreationAllowed, //
		boolean worksiteChangeDuringShiftAllowed, //
		@JsonAlias("employeeManualTimelogEntryAllowed") boolean employeeManualTimeLogEntryAllowed, //
		String defaultTimezone) {
}
