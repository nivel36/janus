/*
 * Copyright 2026 Abel Ferrer Jiménez Licensed under the Apache License, Version
 * 2.0 (the "License");
 */
package es.nivel36.janus.service.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

class ScheduleTest {

	@Test
	void scheduleIdentityUsesItsImmutableCode() {
		final Schedule first = schedule("DAY", "Day shift");
		final Schedule sameIdentity = schedule("DAY", "Renamed day shift");

		assertThat(first).isEqualTo(sameIdentity).hasSameHashCodeAs(sameIdentity);
	}

	@Test
	void scheduleCodeMustNotBeBlank() {
		assertThatThrownBy(() -> schedule(" ", "Day shift")).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void scheduleRuleIdentityUsesItsNameAndSchedule() {
		final Schedule firstSchedule = schedule("DAY", "Day shift");
		final Schedule sameScheduleIdentity = schedule("DAY", "Renamed day shift");
		final ScheduleRule first = new ScheduleRule("Weekdays", firstSchedule);
		final ScheduleRule sameIdentity = new ScheduleRule("Weekdays", sameScheduleIdentity);

		assertThat(first).isEqualTo(sameIdentity).hasSameHashCodeAs(sameIdentity);
	}

	@Test
	void timeRangeIdentityUsesItsBoundaryValues() {
		final TimeRange first = new TimeRange(LocalTime.of(9, 0), LocalTime.of(17, 0));
		final TimeRange sameIdentity = new TimeRange(LocalTime.of(9, 0), LocalTime.of(17, 0));

		assertThat(first).isEqualTo(sameIdentity).hasSameHashCodeAs(sameIdentity);
	}

	private static Schedule schedule(final String code, final String name) {
		return new Schedule(code, name, Duration.ZERO, Duration.ZERO);
	}
}
