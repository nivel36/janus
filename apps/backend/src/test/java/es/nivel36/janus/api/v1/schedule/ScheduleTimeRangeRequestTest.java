/*
 * Copyright 2026 Abel Ferrer Jiménez
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
package es.nivel36.janus.api.v1.schedule;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

class ScheduleTimeRangeRequestTest {

	@Test
	void ordinaryRangeIsValidAndHasSameDayDuration() {
		final ScheduleTimeRangeRequest range = range("09:00", "17:00");

		assertThat(range.isTimeRangeValid()).isTrue();
		assertThat(range.duration()).isEqualTo(Duration.ofHours(8));
	}

	@Test
	void overnightRangeIsValidAndHasNextDayDuration() {
		final ScheduleTimeRangeRequest range = range("22:00", "06:00");

		assertThat(range.isTimeRangeValid()).isTrue();
		assertThat(range.duration()).isEqualTo(Duration.ofHours(8));
	}

	@Test
	void equalBoundsAreInvalidAndHaveZeroDuration() {
		final ScheduleTimeRangeRequest range = range("22:00", "22:00");

		assertThat(range.isTimeRangeValid()).isFalse();
		assertThat(range.duration()).isZero();
	}

	@Test
	void effectiveWorkHoursAreComparedWithOvernightDuration() {
		final ScheduleTimeRangeRequest overnightRange = range("22:00", "06:00");

		assertThat(rule(Duration.ofHours(8), overnightRange).isEffectiveWorkHoursWithinTimeRange()).isTrue();
		assertThat(rule(Duration.ofHours(8).plusMinutes(1), overnightRange).isEffectiveWorkHoursWithinTimeRange())
				.isFalse();
	}

	private static ScheduleTimeRangeRequest range(final String startTime, final String endTime) {
		return new ScheduleTimeRangeRequest(LocalTime.parse(startTime), LocalTime.parse(endTime));
	}

	private static ScheduleRuleTimeRangeRequest rule(final Duration effectiveWorkHours,
			final ScheduleTimeRangeRequest timeRange) {
		return new ScheduleRuleTimeRangeRequest(DayOfWeek.MONDAY, effectiveWorkHours, timeRange);
	}
}
