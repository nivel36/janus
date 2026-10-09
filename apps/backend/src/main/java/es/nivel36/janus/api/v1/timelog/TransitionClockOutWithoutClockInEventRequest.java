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
 * Request payload for resolving or invalidating a clock-out-without-clock-in
 * event.
 * <p>
 * Bean Validation checks the action, reason and action-specific entry-time
 * requirement; construction alone performs no validation.
 *
 * @param action    the final transition to apply; must not be {@code null} when
 *                  validated
 * @param entryTime the proposed entry instant; required for {@code RESOLVE} and
 *                  forbidden for {@code INVALIDATE}
 * @param reason    an optional explanation of at most {@code 255} characters
 *                  without NUL characters; {@code null} is allowed
 */
public record TransitionClockOutWithoutClockInEventRequest(

		@NotNull(message = "action must not be null")
		ClockOutWithoutClockInEventAction action,

		Instant entryTime,

		@Size(max = 255, message = "reason must not exceed 255 characters")
		@Pattern(regexp = "^[^\\x00]*$", message = "reason must not contain NUL")
		String reason) {

	/**
	 * Returns whether the entry-time presence matches the selected action.
	 *
	 * @return {@code true} if the action is absent, resolution has an entry time,
	 *         or invalidation has no entry time; {@code false} otherwise
	 */
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
