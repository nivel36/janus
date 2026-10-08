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
package es.nivel36.janus.api.v1.schedule;

import java.time.Duration;

import es.nivel36.janus.service.schedule.Schedule;

/**
 * API search response representing a {@link Schedule} without its rules.
 *
 * @param code           unique business identifier of the schedule
 * @param name           human readable name describing the schedule
 * @param entryTolerance allowed tolerance for entry times
 * @param exitTolerance  allowed tolerance for exit times
 */
public record ScheduleSummaryResponse(String code, String name, Duration entryTolerance, Duration exitTolerance) {
}
