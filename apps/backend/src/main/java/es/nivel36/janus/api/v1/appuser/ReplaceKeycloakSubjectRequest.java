/* Copyright 2026 Abel Ferrer Jiménez. Licensed under the Apache License, Version 2.0. */
package es.nivel36.janus.api.v1.appuser;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ReplaceKeycloakSubjectRequest(
		@NotBlank @Pattern(regexp = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}") String keycloakSubject) {
}
