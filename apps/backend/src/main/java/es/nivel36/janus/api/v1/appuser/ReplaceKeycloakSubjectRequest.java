/* Copyright 2026 Abel Ferrer Jiménez. Licensed under the Apache License, Version 2.0. */
package es.nivel36.janus.api.v1.appuser;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReplaceKeycloakSubjectRequest(
		@NotBlank @Size(max = 255) String keycloakSubject) {
}
