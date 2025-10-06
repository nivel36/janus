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
package es.nivel36.janus.api.v1.worksite;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import es.nivel36.janus.service.worksite.Worksite;

/**
 * Request payload for updating an existing {@link Worksite}.
 *
 * @param name     the new human readable name of the worksite; must contain
 *                 between 1 and 250 characters
 * @param timeZone the new {@link java.time.ZoneId} identifier of the worksite;
 *                 must contain between 1 and 80 characters
 */
public record UpdateWorksiteRequest(@NotBlank @Size(min = 1, max = 250) String name,
                @NotBlank @Size(min = 1, max = 80) String timeZone) {
}
