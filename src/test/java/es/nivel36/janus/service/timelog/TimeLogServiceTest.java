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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.nivel36.janus.service.admin.AdminService;
import es.nivel36.janus.service.employee.Employee;
import es.nivel36.janus.service.worksite.Worksite;
import es.nivel36.janus.service.worksite.WorksiteService;

class TimeLogServiceTest {

	private static final Logger logger = LoggerFactory.getLogger(TimeLogServiceTest.class);

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
		final ZoneId utcZone = ZoneId.of("UTC");
		worksite.setTimeZone(utcZone);
		employee.getWorksites().add(worksite);
		when(this.clock.getZone()).thenReturn(utcZone);
		when(this.timeLogRepository.save(any(TimeLog.class))).thenAnswer(inv -> inv.getArgument(0));
	}

	private Instant now() {
		return this.clock.instant().truncatedTo(ChronoUnit.SECONDS);
	}

	@Test
	void testClockInSuccess() {
		logger.info("Test clock in success");
		// Arrange
		final Instant fixedNow = LocalDateTime.of(2025, 8, 29, 12, 0, 0).toInstant(ZoneOffset.UTC);
		when(this.clock.instant()).thenReturn(fixedNow);
		when(this.adminService.getDaysUntilLocked()).thenReturn(3);
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
		logger.info("Test clock in with null employee");
		final Instant fixedNow = LocalDateTime.of(2025, 8, 29, 12, 0, 0).toInstant(ZoneOffset.UTC);
		when(this.clock.instant()).thenReturn(fixedNow);

		final Instant now = now();
		assertThrows(NullPointerException.class, () -> {
			this.timeLogService.clockIn(null, this.worksite, now);
		});
	}

	@Test
	void testClockInWithNullEntryTime() {
		logger.info("Test clock in with null entry time");
		assertThrows(NullPointerException.class, () -> {
			this.timeLogService.clockIn(this.employee, this.worksite, null);
		});
	}

	@Test
	void testClockOutSuccess() {
		logger.info("Test clock out success");
		// Arrange
		final Instant fixedNow = LocalDateTime.of(2025, 8, 29, 20, 0, 0).toInstant(ZoneOffset.UTC);
		when(this.clock.instant()).thenReturn(fixedNow);
		when(this.adminService.getDaysUntilLocked()).thenReturn(3);
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
		logger.info("Test clock out with no previous log");
		final Instant fixedNow = LocalDateTime.of(2025, 8, 29, 20, 0, 0).toInstant(ZoneOffset.UTC);
		when(this.clock.instant()).thenReturn(fixedNow);
		when(this.adminService.getDaysUntilLocked()).thenReturn(3);
		when(this.timeLogRepository.findTopByEmployeeAndWorksiteOrderByEntryTimeDesc(this.employee, this.worksite))
				.thenReturn(null);

		final TimeLog result = this.timeLogService.clockOut(this.employee, this.worksite, now());

		// Assert
		assertNotNull(result);
		assertEquals(now().minusSeconds(1), result.getEntryTime());
		assertEquals(now(), result.getExitTime());
	}

	@Test
	void testFindLastTimeLogByEmployeeSuccess() {
		logger.info("Test find last timelog by employee success");
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
		logger.info("Test find last timelog by employee with null employee");
		assertThrows(NullPointerException.class,
				() -> this.timeLogService.findLastTimeLogByEmployee(null, this.worksite));
	}

	@Test
	void testCreateTimeLogSuccessWithinEditingWindow() {
		logger.info("Test create timelog within ending window");
		// Arrange
		final Instant fixedNow = LocalDateTime.of(2025, 8, 30, 10, 0, 0).toInstant(ZoneOffset.UTC);
		when(this.clock.instant()).thenReturn(fixedNow);
		when(this.adminService.getDaysUntilLocked()).thenReturn(3);

		final Instant entry = fixedNow.minus(1, ChronoUnit.DAYS); // dentro de ventana (3 días)
		final Instant exit = fixedNow.minus(2, ChronoUnit.HOURS); // dentro de ventana (3 días)

		// Act
		final TimeLog saved = this.timeLogService.createTimeLog(this.employee, this.worksite, entry, exit);

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
		logger.info("Test create timelog throws when request is null");
		assertThrows(NullPointerException.class,
				() -> this.timeLogService.createTimeLog(this.employee, this.worksite, null, null));
	}

	@Test
	void testCreateTimeLogThrowsWhenEntryIsNull() {
		logger.info("Test create timelog throws when entry is null");
		// Arrange
		final Instant fixedNow = LocalDateTime.of(2025, 8, 30, 10, 0, 0).toInstant(ZoneOffset.UTC);
		when(this.clock.instant()).thenReturn(fixedNow);
		when(this.adminService.getDaysUntilLocked()).thenReturn(3);

		// Act & Assert
		assertThrows(NullPointerException.class, () -> this.timeLogService.createTimeLog(this.employee, this.worksite,
				null, fixedNow.minus(1, ChronoUnit.HOURS)));
		verify(this.timeLogRepository, times(0)).save(any());
	}

	@Test
	void testCreateTimeLogThrowsWhenExitIsNull() {
		logger.info("Test create timelog throws when exit is null");
		// Arrange
		final Instant fixedNow = LocalDateTime.of(2025, 8, 30, 10, 0, 0).toInstant(ZoneOffset.UTC);
		when(this.clock.instant()).thenReturn(fixedNow);
		when(this.adminService.getDaysUntilLocked()).thenReturn(3);

		// Act & Assert
		assertThrows(NullPointerException.class, () -> this.timeLogService.createTimeLog(this.employee, this.worksite,
				fixedNow.minus(1, ChronoUnit.HOURS), null));
		verify(this.timeLogRepository, times(0)).save(any());
	}

	@Test
	void testCreateTimeLogEntryOutsideEditingWindowThrows() {
		logger.info("Test create timelog entry outside editing window throws");
		// Arrange
		final Instant fixedNow = LocalDateTime.of(2025, 8, 30, 10, 0, 0).toInstant(ZoneOffset.UTC);
		when(this.clock.instant()).thenReturn(fixedNow);
		when(this.adminService.getDaysUntilLocked()).thenReturn(3);

		final Instant entry = fixedNow.minus(4, ChronoUnit.DAYS); // fuera de ventana
		final Instant exit = fixedNow.minus(1, ChronoUnit.DAYS); // dentro, pero entry bloquea

		// Act & Assert
		assertThrows(TimeLogModificationNotAllowedException.class,
				() -> this.timeLogService.createTimeLog(this.employee, this.worksite, entry, exit));
		verify(this.timeLogRepository, times(0)).save(any());
	}

	@Test
	void testCreateTimeLogExitOutsideEditingWindowThrows() {
		logger.info("Test create timelog exit outside editing window throws");
		// Arrange
		final Instant fixedNow = LocalDateTime.of(2025, 8, 30, 10, 0, 0).toInstant(ZoneOffset.UTC);
		when(this.clock.instant()).thenReturn(fixedNow);
		when(this.adminService.getDaysUntilLocked()).thenReturn(3);

		final Instant entry = fixedNow.minus(1, ChronoUnit.DAYS); // dentro de ventana
		final Instant exit = fixedNow.minus(4, ChronoUnit.DAYS); // fuera de ventana

		// Act & Assert
		assertThrows(TimeLogModificationNotAllowedException.class,
				() -> this.timeLogService.createTimeLog(this.employee, this.worksite, entry, exit));
		verify(this.timeLogRepository, times(0)).save(any());
	}

	@Test
	void testCreateTimeLogBoundaryAtLockInstantForEntryShouldThrow() {
		logger.info("Test create timelog boundary at lock instant for entry should throw");
		// Arrange
		final Instant fixedNow = LocalDateTime.of(2025, 8, 30, 10, 0, 0).toInstant(ZoneOffset.UTC);
		when(this.clock.instant()).thenReturn(fixedNow);
		when(this.adminService.getDaysUntilLocked()).thenReturn(3);

		// entry.plus(3d) == now -> !isAfter(now) => EXCEPCIÓN
		final Instant entry = fixedNow.minus(3, ChronoUnit.DAYS);
		final Instant exit = fixedNow.minus(2, ChronoUnit.DAYS);

		// Act & Assert
		assertThrows(TimeLogModificationNotAllowedException.class,
				() -> this.timeLogService.createTimeLog(this.employee, this.worksite, entry, exit));
		verify(this.timeLogRepository, times(0)).save(any());
	}

	@Test
	void testCreateTimeLogBoundaryAtLockInstantForExitShouldThrow() {
		logger.info("Test create timelog boundary at lock instant for exit should throw");
		// Arrange
		final Instant fixedNow = LocalDateTime.of(2025, 8, 30, 10, 0, 0).toInstant(ZoneOffset.UTC);
		when(this.clock.instant()).thenReturn(fixedNow);
		when(this.adminService.getDaysUntilLocked()).thenReturn(3);

		final Instant entry = fixedNow.minus(2, ChronoUnit.DAYS);
		final Instant exit = fixedNow.minus(3, ChronoUnit.DAYS); // exit.plus(3d) == now -> bloquea

		// Act & Assert
		assertThrows(TimeLogModificationNotAllowedException.class,
				() -> this.timeLogService.createTimeLog(this.employee, this.worksite, entry, exit));
		verify(this.timeLogRepository, times(0)).save(any());
	}

	@Test
	void testCreateTimeLogChronologyEntryAfterExitThrows() {
		logger.info("Test create timelog chronology entry after exit throws");
		// Arrange
		final Instant fixedNow = LocalDateTime.of(2025, 8, 30, 10, 0, 0).toInstant(ZoneOffset.UTC);
		when(this.clock.instant()).thenReturn(fixedNow);
		when(this.adminService.getDaysUntilLocked()).thenReturn(3);

		final Instant entry = fixedNow.minus(1, ChronoUnit.HOURS); // posterior a exit
		final Instant exit = fixedNow.minus(2, ChronoUnit.HOURS);

		// Act & Assert
		assertThrows(TimeLogChronologyException.class,
				() -> this.timeLogService.createTimeLog(this.employee, this.worksite, entry, exit));
		verify(this.timeLogRepository, times(0)).save(any());
	}

	@Test
	void testDeleteTimeLogSuccessWithinEditingWindow() {
		logger.info("Test delete time log succes within editing window");
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
		logger.info("Test delete time log throws when null");
		assertThrows(NullPointerException.class, () -> this.timeLogService.deleteTimeLog(null));
	}

	@Test
	void testDeleteTimeLogThrowsWhenOutsideEditingWindow() {
		logger.info("Test delete time log throws when outside editing window");
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
		logger.info("Test delete time log boundary at lock instant should throw");
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
