/*
 * Copyright 2026 Abel Ferrer Jiménez
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package es.nivel36.janus.api.v1.timelog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

import es.nivel36.janus.service.employee.Employee;
import es.nivel36.janus.service.timelog.ClockOutWithoutClockInEvent;
import es.nivel36.janus.service.timelog.TimeLog;
import es.nivel36.janus.service.worksite.Worksite;

class ClockOutWithoutClockInEventResponseMapperTest {

	private final DurationResponseMapper durationMapper = new DurationResponseMapper();
	private final ClockOutWithoutClockInEventResponseMapper mapper = new ClockOutWithoutClockInEventResponseMapper(
			this.durationMapper);

	@Test
	void mapsEmployeeAndWorksiteDetailsForUnresolvedEvent() {
		final ClockOutWithoutClockInEvent event = event();
		final Instant exitTime = Instant.parse("2026-10-09T16:00:00Z");
		when(event.getExitTime()).thenReturn(exitTime);
		when(event.getDetectedAt()).thenReturn(exitTime);

		final ClockOutWithoutClockInEventResponse response = this.mapper.map(event);

		assertThat(response.employee()).isEqualTo(new TimeLogEmployeeResponse("EMP-0042", "Ada Lovelace"));
		final ZoneId zoneId = ZoneId.of("Europe/Madrid");
		assertThat(response.worksite()).isEqualTo(new TimeLogWorksiteResponse("OFFICE", "Main Office", zoneId));
		assertThat(response.exitTime()).isEqualTo(exitTime);
		assertThat(response.detectedAt()).isEqualTo(exitTime);
		assertThat(response.resolved()).isFalse();
		assertThat(response.invalidated()).isFalse();
		assertThat(response.resolution()).isNull();
	}

	@Test
	void mapsResolutionWithEntryTimeAndDuration() {
		final ClockOutWithoutClockInEvent event = event();
		final TimeLog timeLog = mock(TimeLog.class);
		final Instant entryTime = Instant.parse("2026-10-09T09:00:00Z");
		final Instant exitTime = Instant.parse("2026-10-09T16:00:00Z");
		final Duration duration = Duration.ofHours(7);
		when(event.getExitTime()).thenReturn(exitTime);
		when(timeLog.getEntryTime()).thenReturn(entryTime);
		when(timeLog.getWorkDuration()).thenReturn(duration);
		when(event.getResolvedTimeLog()).thenReturn(timeLog);
		when(event.isResolved()).thenReturn(true);
		when(event.getReason()).thenReturn("Missing clock-in corrected");

		final ClockOutWithoutClockInEventResponse response = this.mapper.map(event);

		assertThat(response.resolved()).isTrue();
		assertThat(response.reason()).isEqualTo("Missing clock-in corrected");
		assertThat(response.resolution().entryTime()).isEqualTo(entryTime);
		assertThat(response.resolution().workTime()).isEqualTo(new DurationResponse(7, 0, 0, "PT7H"));
	}

	@Test
	void mapsInvalidatedEventWithoutResolvingTimeLog() {
		final ClockOutWithoutClockInEvent event = event();
		when(event.isInvalidated()).thenReturn(true);
		when(event.getReason()).thenReturn("Accidental clock-out");

		final ClockOutWithoutClockInEventResponse response = this.mapper.map(event);

		assertThat(response.invalidated()).isTrue();
		assertThat(response.resolved()).isFalse();
		assertThat(response.reason()).isEqualTo("Accidental clock-out");
		assertThat(response.resolution()).isNull();
	}

	@Test
	void mapsNullToNull() {
		assertThat(this.mapper.map(null)).isNull();
	}

	private ClockOutWithoutClockInEvent event() {
		final Employee employee = mock(Employee.class);
		final Worksite worksite = mock(Worksite.class);
		final ClockOutWithoutClockInEvent event = mock(ClockOutWithoutClockInEvent.class);
		final ZoneId zoneId = ZoneId.of("Europe/Madrid");
		when(employee.getEmployeeNumber()).thenReturn("EMP-0042");
		when(employee.getName()).thenReturn("Ada");
		when(employee.getSurname()).thenReturn("Lovelace");
		when(worksite.getCode()).thenReturn("OFFICE");
		when(worksite.getName()).thenReturn("Main Office");
		when(worksite.getTimeZone()).thenReturn(zoneId);
		when(event.getEmployee()).thenReturn(employee);
		when(event.getWorksite()).thenReturn(worksite);
		return event;
	}
}
