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
package es.nivel36.janus.service.timelog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import es.nivel36.janus.api.v1.timelog.CreateTimeLogRequest;
import es.nivel36.janus.service.admin.AdminService;
import es.nivel36.janus.service.employee.Employee;
import es.nivel36.janus.service.worksite.Worksite;
import es.nivel36.janus.service.worksite.WorksiteService;

class TimeLogServiceTest {

	private @Mock TimeLogRepository timeLogRepository;
	private @Mock AdminService adminService;
	private @Mock WorksiteService worksiteService;
	private @Mock Clock clock;
	private @InjectMocks TimeLogService timeLogService;
	private Employee employee;
	private Worksite worksite;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		this.employee = new Employee();
		employee.setEmail("aaron@test,com");
		this.worksite = new Worksite();
		worksite.setCode("BCN-HQ");
		final ZoneId UTCZone = ZoneId.of("UTC");
		worksite.setTimeZone(UTCZone);
		employee.getWorksites().add(worksite);
		when(this.clock.getZone()).thenReturn(UTCZone);
		when(this.timeLogRepository.save(any(TimeLog.class))).thenAnswer(inv -> inv.getArgument(0));
	}

	private Instant now() {
		return this.clock.instant().truncatedTo(ChronoUnit.SECONDS);
	}

	@Test
	void testClockInSuccess() {
		// Arrange
		final Instant fixedNow = LocalDateTime.of(2025, 8, 29, 12, 0, 0).toInstant(ZoneOffset.UTC);
		when(this.clock.instant()).thenReturn(fixedNow);
		final Instant entry = now();

		// Act
		final TimeLog timeLog = this.timeLogService.clockIn(this.employee, this.worksite, entry);

		// Assert
		assertNotNull(timeLog);
		assertEquals(this.employee, timeLog.getEmployee());
		assertEquals(entry, timeLog.getEntryTime());
		verify(this.timeLogRepository, times(1)).save(timeLog);
	}

	@Test
	void testClockInWithNullEmployee() {
		final Instant fixedNow = LocalDateTime.of(2025, 8, 29, 12, 0, 0).toInstant(ZoneOffset.UTC);
		when(this.clock.instant()).thenReturn(fixedNow);

		assertThrows(NullPointerException.class, () -> {
			this.timeLogService.clockIn(null, this.worksite, now());
		});
	}

	@Test
	void testClockInWithNullEntryTime() {
		assertThrows(NullPointerException.class, () -> {
			this.timeLogService.clockIn(this.employee, this.worksite, null);
		});
	}

	@Test
	void testClockOutSuccess() {
		// Arrange
		final Instant fixedNow = LocalDateTime.of(2025, 8, 29, 20, 0, 0).toInstant(ZoneOffset.UTC);
		when(this.clock.instant()).thenReturn(fixedNow);
		final Instant eightHoursBefore = fixedNow.minus(8, ChronoUnit.HOURS);

		final TimeLog existingTimeLog = new TimeLog(this.employee, this.worksite, eightHoursBefore);
		when(this.timeLogRepository.findTopByEmployeeAndWorksiteOrderByEntryTimeDesc(this.employee, this.worksite))
				.thenReturn(existingTimeLog);

		// Act
		final TimeLog result = this.timeLogService.clockOut(this.employee, this.worksite, now());

		// Assert
		assertNotNull(result);
		assertEquals(now(), result.getExitTime());
	}

	@Test
	void testClockOutWithNoPreviousLog() {
		final Instant fixedNow = LocalDateTime.of(2025, 8, 29, 20, 0, 0).toInstant(ZoneOffset.UTC);
		when(this.clock.instant()).thenReturn(fixedNow);
		when(this.timeLogRepository.findTopByEmployeeAndWorksiteOrderByEntryTimeDesc(this.employee, this.worksite))
				.thenReturn(null);

		final TimeLog result = this.timeLogService.clockOut(this.employee, this.worksite, now());

		// Assert
		assertNotNull(result);
		assertEquals(now().minusSeconds(1), result.getEntryTime());
		assertEquals(now(), result.getExitTime());
	}

	@Test
	void testGetHoursWorkedWithExitTime() {
		// Arrange
		final Instant fixedNow = LocalDateTime.of(2025, 8, 30, 10, 0, 0).toInstant(ZoneOffset.UTC);
		when(this.clock.instant()).thenReturn(fixedNow);

		final Instant entryTime = fixedNow.minus(5, ChronoUnit.HOURS);
		final Instant exitTime = fixedNow;
		final TimeLog timeLog = new TimeLog(this.employee, this.worksite, entryTime);
		timeLog.setExitTime(exitTime);

		// Act
		final Duration duration = this.timeLogService.getTimeWorked(timeLog);

		// Assert
		assertNotNull(duration);
		assertEquals(Duration.between(entryTime, exitTime), duration);
	}

	@Test
	void testGetHoursWorkedWithoutExitTime() {
		// Arrange
		final Instant fixedNow = LocalDateTime.of(2025, 8, 30, 10, 0, 0).toInstant(ZoneOffset.UTC);
		when(this.clock.instant()).thenReturn(fixedNow);

		final Instant entryTime = fixedNow.minus(3, ChronoUnit.HOURS);
		final TimeLog timeLog = new TimeLog(this.employee, this.worksite, entryTime);

		// Act
		final Duration duration = this.timeLogService.getTimeWorked(timeLog);

		// Assert
		assertNotNull(duration);
		assertEquals(Duration.between(entryTime, fixedNow).toHours(), duration.toHours(), 1);
	}

	@Test
	void testGetHoursWorkedWithNullTimeLog() {
		assertThrows(NullPointerException.class, () -> this.timeLogService.getTimeWorked(null));
	}

	@Test
	void testFindLastTimeLogByEmployeeSuccess() {
		final Instant fixedNow = LocalDateTime.of(2025, 8, 29, 9, 0, 0).toInstant(ZoneOffset.UTC);
		when(this.clock.instant()).thenReturn(fixedNow);

		final TimeLog expectedTimeLog = new TimeLog(this.employee, this.worksite, now());
		when(this.timeLogRepository.findTopByEmployeeAndWorksiteOrderByEntryTimeDesc(this.employee, this.worksite))
				.thenReturn(expectedTimeLog);

		// Act
		final TimeLog result = this.timeLogService.findLastTimeLogByEmployee(this.employee, this.worksite);

		// Assert
		assertNotNull(result);
		assertEquals(expectedTimeLog, result);
	}

	@Test
	void testFindLastTimeLogByEmployeeWithNullEmployee() {
		assertThrows(NullPointerException.class,
				() -> this.timeLogService.findLastTimeLogByEmployee(null, this.worksite));
	}

	@Test
	void testCreateTimeLogSuccessWithinEditingWindow() {
		// Arrange
		final Instant fixedNow = LocalDateTime.of(2025, 8, 30, 10, 0, 0).toInstant(ZoneOffset.UTC);
		when(this.clock.instant()).thenReturn(fixedNow);
		when(this.adminService.getDaysUntilLocked()).thenReturn(3);

		final Instant entry = fixedNow.minus(1, ChronoUnit.DAYS); // dentro de ventana (3 días)
		final Instant exit = fixedNow.minus(2, ChronoUnit.HOURS); // dentro de ventana (3 días)
		final CreateTimeLogRequest req = new CreateTimeLogRequest(entry, exit);

		// Act
		final TimeLog saved = this.timeLogService.createTimeLog(this.employee, this.worksite, req);

		// Assert
		assertNotNull(saved);
		assertEquals(this.employee, saved.getEmployee());
		assertEquals(this.worksite, saved.getWorksite());
		assertEquals(entry, saved.getEntryTime());
		assertEquals(exit, saved.getExitTime());
		verify(this.timeLogRepository, times(1)).save(any(TimeLog.class));
	}

	@Test
	void testCreateTimeLogThrowsWhenRequestIsNull() {
		assertThrows(NullPointerException.class,
				() -> this.timeLogService.createTimeLog(this.employee, this.worksite, null));
	}

	@Test
	void testCreateTimeLogThrowsWhenEntryIsNull() {
		// Arrange
		final Instant fixedNow = LocalDateTime.of(2025, 8, 30, 10, 0, 0).toInstant(ZoneOffset.UTC);
		when(this.clock.instant()).thenReturn(fixedNow);
		when(this.adminService.getDaysUntilLocked()).thenReturn(3);

		final CreateTimeLogRequest req = new CreateTimeLogRequest(null, fixedNow.minus(1, ChronoUnit.HOURS));

		// Act & Assert
		assertThrows(NullPointerException.class,
				() -> this.timeLogService.createTimeLog(this.employee, this.worksite, req));
		verify(this.timeLogRepository, times(0)).save(any());
	}

	@Test
	void testCreateTimeLogThrowsWhenExitIsNull() {
		// Arrange
		final Instant fixedNow = LocalDateTime.of(2025, 8, 30, 10, 0, 0).toInstant(ZoneOffset.UTC);
		when(this.clock.instant()).thenReturn(fixedNow);
		when(this.adminService.getDaysUntilLocked()).thenReturn(3);

		final CreateTimeLogRequest req = new CreateTimeLogRequest(fixedNow.minus(1, ChronoUnit.HOURS), null);

		// Act & Assert
		assertThrows(NullPointerException.class,
				() -> this.timeLogService.createTimeLog(this.employee, this.worksite, req));
		verify(this.timeLogRepository, times(0)).save(any());
	}

	@Test
	void testCreateTimeLogEntryOutsideEditingWindowThrows() {
		// Arrange
		final Instant fixedNow = LocalDateTime.of(2025, 8, 30, 10, 0, 0).toInstant(ZoneOffset.UTC);
		when(this.clock.instant()).thenReturn(fixedNow);
		when(this.adminService.getDaysUntilLocked()).thenReturn(3);

		final Instant entry = fixedNow.minus(4, ChronoUnit.DAYS); // fuera de ventana
		final Instant exit = fixedNow.minus(1, ChronoUnit.DAYS); // dentro, pero entry bloquea
		final CreateTimeLogRequest req = new CreateTimeLogRequest(entry, exit);

		// Act & Assert
		assertThrows(TimeLogModificationNotAllowedException.class,
				() -> this.timeLogService.createTimeLog(this.employee, this.worksite, req));
		verify(this.timeLogRepository, times(0)).save(any());
	}

	@Test
	void testCreateTimeLogExitOutsideEditingWindowThrows() {
		// Arrange
		final Instant fixedNow = LocalDateTime.of(2025, 8, 30, 10, 0, 0).toInstant(ZoneOffset.UTC);
		when(this.clock.instant()).thenReturn(fixedNow);
		when(this.adminService.getDaysUntilLocked()).thenReturn(3);

		final Instant entry = fixedNow.minus(1, ChronoUnit.DAYS); // dentro de ventana
		final Instant exit = fixedNow.minus(4, ChronoUnit.DAYS); // fuera de ventana
		final CreateTimeLogRequest req = new CreateTimeLogRequest(entry, exit);

		// Act & Assert
		assertThrows(TimeLogModificationNotAllowedException.class,
				() -> this.timeLogService.createTimeLog(this.employee, this.worksite, req));
		verify(this.timeLogRepository, times(0)).save(any());
	}

	@Test
	void testCreateTimeLogBoundaryAtLockInstantForEntryShouldThrow() {
		// Arrange
		final Instant fixedNow = LocalDateTime.of(2025, 8, 30, 10, 0, 0).toInstant(ZoneOffset.UTC);
		when(this.clock.instant()).thenReturn(fixedNow);
		when(this.adminService.getDaysUntilLocked()).thenReturn(3);

		// entry.plus(3d) == now -> !isAfter(now) => EXCEPCIÓN
		final Instant entry = fixedNow.minus(3, ChronoUnit.DAYS);
		final Instant exit = fixedNow.minus(2, ChronoUnit.DAYS);
		final CreateTimeLogRequest req = new CreateTimeLogRequest(entry, exit);

		// Act & Assert
		assertThrows(TimeLogModificationNotAllowedException.class,
				() -> this.timeLogService.createTimeLog(this.employee, this.worksite, req));
		verify(this.timeLogRepository, times(0)).save(any());
	}

	@Test
	void testCreateTimeLogBoundaryAtLockInstantForExitShouldThrow() {
		// Arrange
		final Instant fixedNow = LocalDateTime.of(2025, 8, 30, 10, 0, 0).toInstant(ZoneOffset.UTC);
		when(this.clock.instant()).thenReturn(fixedNow);
		when(this.adminService.getDaysUntilLocked()).thenReturn(3);

		final Instant entry = fixedNow.minus(2, ChronoUnit.DAYS);
		final Instant exit = fixedNow.minus(3, ChronoUnit.DAYS); // exit.plus(3d) == now -> bloquea
		final CreateTimeLogRequest req = new CreateTimeLogRequest(entry, exit);

		// Act & Assert
		assertThrows(TimeLogModificationNotAllowedException.class,
				() -> this.timeLogService.createTimeLog(this.employee, this.worksite, req));
		verify(this.timeLogRepository, times(0)).save(any());
	}

	@Test
	void testCreateTimeLogChronologyEntryAfterExitThrows() {
		// Arrange
		final Instant fixedNow = LocalDateTime.of(2025, 8, 30, 10, 0, 0).toInstant(ZoneOffset.UTC);
		when(this.clock.instant()).thenReturn(fixedNow);
		when(this.adminService.getDaysUntilLocked()).thenReturn(3);

		final Instant entry = fixedNow.minus(1, ChronoUnit.HOURS); // posterior a exit
		final Instant exit = fixedNow.minus(2, ChronoUnit.HOURS);
		final CreateTimeLogRequest req = new CreateTimeLogRequest(entry, exit);

		// Act & Assert
		assertThrows(TimeLogChronologyException.class,
				() -> this.timeLogService.createTimeLog(this.employee, this.worksite, req));
		verify(this.timeLogRepository, times(0)).save(any());
	}

	@Test
	void testDeleteTimeLogSuccessWithinEditingWindow() {
		// Arrange
		final Instant fixedNow = LocalDateTime.of(2025, 8, 29, 12, 0, 0).toInstant(ZoneOffset.UTC);
		when(this.clock.instant()).thenReturn(fixedNow);
		when(this.adminService.getDaysUntilLocked()).thenReturn(3);

		final Instant entryTime = fixedNow.minus(2, ChronoUnit.DAYS);
		final TimeLog timeLog = new TimeLog(this.employee, this.worksite, entryTime);

		// Act
		this.timeLogService.deleteTimeLog(timeLog);

		// Assert
		verify(this.timeLogRepository, times(1)).delete(timeLog);
	}

	@Test
	void testDeleteTimeLogThrowsWhenNull() {
		assertThrows(NullPointerException.class, () -> this.timeLogService.deleteTimeLog(null));
	}

	@Test
	void testDeleteTimeLogThrowsWhenOutsideEditingWindow() {
		// Arrange
		final Instant fixedNow = LocalDateTime.of(2025, 8, 29, 12, 0, 0).toInstant(ZoneOffset.UTC);
		when(this.clock.instant()).thenReturn(fixedNow);
		when(this.adminService.getDaysUntilLocked()).thenReturn(3);

		final Instant entryTime = fixedNow.minus(4, ChronoUnit.DAYS).atZone(ZoneOffset.UTC).toInstant();
		final TimeLog timeLog = new TimeLog(this.employee, this.worksite, entryTime);

		// Act & Assert
		assertThrows(TimeLogModificationNotAllowedException.class, () -> this.timeLogService.deleteTimeLog(timeLog));
		verify(this.timeLogRepository, times(0)).delete(timeLog);
	}

	@Test
	void testDeleteTimeLogBoundaryAtLockInstantShouldThrow() {
		// Arrange
		final Instant fixedNow = LocalDateTime.of(2025, 8, 29, 12, 0, 0).toInstant(ZoneOffset.UTC);
		when(this.clock.instant()).thenReturn(fixedNow);
		when(this.adminService.getDaysUntilLocked()).thenReturn(3);

		final Instant entryTime = fixedNow.minus(3, ChronoUnit.DAYS);
		final TimeLog timeLog = new TimeLog(this.employee, this.worksite, entryTime);

		// Act & Assert
		assertThrows(TimeLogModificationNotAllowedException.class, () -> this.timeLogService.deleteTimeLog(timeLog));
		verify(this.timeLogRepository, times(0)).delete(timeLog);
	}
}
