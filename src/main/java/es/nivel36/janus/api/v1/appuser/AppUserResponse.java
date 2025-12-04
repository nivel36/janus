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
package es.nivel36.janus.api.v1.appuser;

import es.nivel36.janus.service.TimeFormat;
import es.nivel36.janus.service.appuser.AppUser;

/**
 * Response DTO exposing the public representation of an {@link AppUser}.
 *
 * @param username   the unique username of the user
 * @param name       the user's first name
 * @param surname    the user's surname
 * @param locale     the user's preferred locale expressed as a BCP 47 language
 *                   tag
 * @param timeFormat the preferred {@link TimeFormat}
 */
public record AppUserResponse(String username, String name, String surname, String locale, TimeFormat timeFormat) {
}
