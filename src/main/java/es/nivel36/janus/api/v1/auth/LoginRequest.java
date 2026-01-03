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
package es.nivel36.janus.api.v1.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request payload for authenticating a user.
 *
 * @param username user's username
 * @param password raw password
 */
public record LoginRequest( //
		@NotBlank(message = "username must not be blank") //
		@Pattern(regexp = "[A-Za-z0-9_.@-]{3,50}", //
				message = "username must contain only letters, digits, dots, underscores, hyphens or at signs (3-50 characters)") //
		String username, //

		@NotBlank(message = "password must not be blank") //
		@Size(min = 8, max = 72, message = "password must be between 8 and 72 characters") //
		String password //
) {
}
