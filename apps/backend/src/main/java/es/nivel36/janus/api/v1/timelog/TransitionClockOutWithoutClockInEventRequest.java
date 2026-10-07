/*
 * Copyright 2026 Abel Ferrer Jiménez Licensed under the Apache License, Version
 * 2.0 (the "License");
 */
package es.nivel36.janus.api.v1.timelog;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

/**
 * Uniform payload for either final transition of a clock-out-without-clock-in
 * event.
 */
public record TransitionClockOutWithoutClockInEventRequest(

		@NotNull(message = "action must not be null")
		ClockOutWithoutClockInEventAction action,

		Instant entryTime,

		@Size(max = 255, message = "reason must not exceed 255 characters")
		@Pattern(regexp = "^[^\\x00]*$", message = "reason must not contain NUL")
		String reason) {

	@JsonIgnore
	@AssertTrue(message = "entryTime is required when action is RESOLVE and forbidden when action is INVALIDATE")
	public boolean isEntryTimeCompatibleWithAction() {
		if (this.action == null) {
			return true;
		}

		return switch (this.action) {
		case RESOLVE -> this.entryTime != null;
		case INVALIDATE -> this.entryTime == null;
		};
	}
}
