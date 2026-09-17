/* Copyright 2026 Abel Ferrer Jiménez. Licensed under the Apache License, Version 2.0. */
package es.nivel36.janus.api.v1.appuser;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload used to replace an application's external identity subject.
 *
 * @param keycloakSubject opaque external identity subject; must not be blank
 *                        and must not exceed 255 characters
 */
public record ReplaceKeycloakSubjectRequest(

		@NotBlank(message = "keycloakSubject must not be blank") //
		@Size(max = 255, message = "keycloakSubject must not exceed 255 characters") //
		String keycloakSubject) {
}
