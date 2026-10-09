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

import java.time.ZoneId;

/**
 * Worksite details included in a time log response.
 *
 * @param code   the unique worksite code
 * @param name   the worksite name
 * @param zoneId the worksite time zone
 */
public record TimeLogWorksiteResponse(String code, String name, ZoneId zoneId) {
}
