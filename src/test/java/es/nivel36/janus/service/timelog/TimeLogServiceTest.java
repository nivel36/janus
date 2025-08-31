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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openapitools.jackson.nullable.JsonNullable;

import es.nivel36.janus.api.timelog.UpdateTimeLogRequest;
import es.nivel36.janus.service.admin.AdminService;
import es.nivel36.janus.service.employee.Employee;
import jakarta.persistence.EntityNotFoundException;

class TimeLogServiceTest {

	private @Mock TimeLogRepository timeLogRepository;
	private @Mock AdminService adminService;
	private @Mock Clock clock;
	private @InjectMocks TimeLogService timeLogService;
	private Employee employee;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		this.employee = new Employee();
	}

	@Test
	void testClockInSuccess() {
		final LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

		// Act
		final TimeLog timeLog = this.timeLogService.clockIn(this.employee, now);

		// Assert
		assertNotNull(timeLog);
		assertEquals(this.employee, timeLog.getEmployee());
		assertEquals(now, timeLog.getEntryTime());
		verify(this.timeLogRepository, times(1)).save(timeLog);
	}

	@Test
	void testClockInWithNullEmployee() {
		assertThrows(NullPointerException.class, () -> {
			this.timeLogService.clockIn(null, LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
		});
	}

	@Test
	void testClockInWithNullEntryTime() {
		assertThrows(NullPointerException.class, () -> {
			this.timeLogService.clockIn(this.employee, null);
		});
	}

	@Test
	void testClockOutSuccess() {
		final LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
		final TimeLog existingTimeLog = new TimeLog(this.employee, now.minusHours(8)); // Simula una entrada previa

		when(this.timeLogRepository.findLastTimeLogByEmployee(this.employee)).thenReturn(Optional.of(existingTimeLog));

		// Act
		final TimeLog result = this.timeLogService.clockOut(this.employee, now);

		// Assert
		assertNotNull(result);
		assertEquals(now, result.getExitTime());
	}

	@Test
	void testClockOutWithNoPreviousLog() {
		when(this.timeLogRepository.findLastTimeLogByEmployee(this.employee)).thenReturn(Optional.empty());

		assertThrows(IllegalStateException.class, () -> {
			this.timeLogService.clockOut(this.employee, LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
		});
	}

	@Test
	void testGetHoursWorkedWithExitTime() {
		// Arrange
		final LocalDateTime entryTime = LocalDateTime.now().minusHours(5);
		final LocalDateTime exitTime = LocalDateTime.now();
		final TimeLog timeLog = new TimeLog(this.employee, entryTime);
		timeLog.setExitTime(exitTime);

		// Act
		final Duration duration = this.timeLogService.getHoursWorked(timeLog);

		// Assert
		assertNotNull(duration);
		assertEquals(Duration.between(entryTime, exitTime), duration);
	}

	@Test
	void testGetHoursWorkedWithoutExitTime() {
		// Arrange
		final LocalDateTime entryTime = LocalDateTime.now().minusHours(3);
		final TimeLog timeLog = new TimeLog(this.employee, entryTime);

		// Act
		final Duration duration = this.timeLogService.getHoursWorked(timeLog);

		// Assert
		assertNotNull(duration);
		assertEquals(Duration.between(entryTime, LocalDateTime.now()).toHours(), duration.toHours(), 1);
	}

	@Test
	void testGetHoursWorkedWithNullTimeLog() {
		// Act & Assert
		assertThrows(NullPointerException.class, () -> {
			this.timeLogService.getHoursWorked(null);
		});
	}

	@Test
	void testFindTimeLogByIdSuccess() {
		final long validId = 1L;
		final TimeLog expectedTimeLog = new TimeLog(this.employee, LocalDateTime.now());

		when(this.timeLogRepository.findById(validId)).thenReturn(Optional.of(expectedTimeLog));

		// Act
		final TimeLog result = this.timeLogService.findTimeLogById(validId);

		// Assert
		assertNotNull(result);
		assertEquals(expectedTimeLog, result);
	}

	@Test
	void testFindTimeLogByIdInvalidId() {
		assertThrows(NullPointerException.class, () -> {
			this.timeLogService.findTimeLogById(null);
		});
	}

	@Test
	void testFindLastTimeLogByEmployeeSuccess() {
		final TimeLog expectedTimeLog = new TimeLog(this.employee, LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));

		when(this.timeLogRepository.findLastTimeLogByEmployee(this.employee)).thenReturn(Optional.of(expectedTimeLog));

		// Act
		final Optional<TimeLog> result = this.timeLogService.findLastTimeLogByEmployee(this.employee);

		// Assert
		assertTrue(result.isPresent());
		assertEquals(expectedTimeLog, result.get());
	}

	@Test
	void testFindLastTimeLogByEmployeeWithNullEmployee() {
		assertThrows(NullPointerException.class, () -> {
			this.timeLogService.findLastTimeLogByEmployee(null);
		});
	}

	@Test
	void testUpdateTimeLogSuccessUpdateEntryWithinWindow() {
		// Arrange
		final long id = 10L;
		final LocalDateTime fixedNow = LocalDateTime.of(2025, 8, 29, 12, 0, 0);
		when(this.clock.instant()).thenReturn(fixedNow.toInstant(java.time.ZoneOffset.UTC));
		when(this.clock.getZone()).thenReturn(ZoneId.of("UTC"));
		when(this.adminService.getDaysUntilLocked()).thenReturn(3);

		final TimeLog existing = new TimeLog(this.employee, fixedNow.minusHours(8));
		when(this.timeLogRepository.findById(id)).thenReturn(Optional.of(existing));

		when(this.timeLogRepository.save(any(TimeLog.class)))
				.thenAnswer(inv -> inv.getArgument(0));

		final LocalDateTime newEntry = fixedNow.minusDays(2);
		final UpdateTimeLogRequest req = new UpdateTimeLogRequest(JsonNullable.of(newEntry), JsonNullable.undefined());

		// Act
		final TimeLog saved = this.timeLogService.updateTimeLog(id, req);

		// Assert
		assertEquals(newEntry, saved.getEntryTime());
		verify(this.timeLogRepository, times(1)).save(existing);
	}

	@Test
	void testUpdateTimeLogSuccessUpdateExitWithinWindow() {
		// Arrange
		final long id = 11L;
		final LocalDateTime fixedNow = LocalDateTime.of(2025, 8, 29, 12, 0, 0);
		when(this.clock.instant()).thenReturn(fixedNow.toInstant(java.time.ZoneOffset.UTC));
		when(this.clock.getZone()).thenReturn(ZoneId.of("UTC"));
		when(this.adminService.getDaysUntilLocked()).thenReturn(3);

		final TimeLog existing = new TimeLog(this.employee, fixedNow.minusHours(6));
		when(this.timeLogRepository.findById(id)).thenReturn(Optional.of(existing));

		final LocalDateTime newExit = fixedNow.minusHours(1); // dentro de ventana
		final UpdateTimeLogRequest req = new UpdateTimeLogRequest(JsonNullable.undefined(), JsonNullable.of(newExit));

		when(this.timeLogRepository.save(org.mockito.ArgumentMatchers.any(TimeLog.class)))
				.thenAnswer(inv -> inv.getArgument(0));

		// Act
		final TimeLog saved = this.timeLogService.updateTimeLog(id, req);

		// Assert
		assertEquals(newExit, saved.getExitTime());
		verify(this.timeLogRepository, times(1)).save(existing);
	}

	@Test
	void testUpdateTimeLogSuccessUpdateBothWithValidChronology() {
		// Arrange
		final long id = 12L;
		final LocalDateTime fixedNow = LocalDateTime.of(2025, 8, 29, 12, 0, 0);
		when(this.clock.instant()).thenReturn(fixedNow.toInstant(java.time.ZoneOffset.UTC));
		when(this.clock.getZone()).thenReturn(ZoneId.of("UTC"));
		when(this.adminService.getDaysUntilLocked()).thenReturn(5);

		final TimeLog existing = new TimeLog(this.employee, fixedNow.minusDays(10));
		when(this.timeLogRepository.findById(id)).thenReturn(Optional.of(existing));

		final LocalDateTime newEntry = fixedNow.minusDays(1);
		final LocalDateTime newExit = fixedNow.minusHours(2);
		final UpdateTimeLogRequest req = new UpdateTimeLogRequest(JsonNullable.of(newEntry), JsonNullable.of(newExit));

		when(this.timeLogRepository.save(any(TimeLog.class))).thenAnswer(inv -> inv.getArgument(0));

		// Act
		final TimeLog saved = this.timeLogService.updateTimeLog(id, req);

		// Assert
		assertEquals(newEntry, saved.getEntryTime());
		assertEquals(newExit, saved.getExitTime());
		verify(this.timeLogRepository, times(1)).save(existing);
	}

	@Test
	void testUpdateTimeLogWithNullRequestThrows() {
		assertThrows(NullPointerException.class, () -> this.timeLogService.updateTimeLog(1L, null));
	}

	@Test
	void testUpdateTimeLogWithBothValuesUndefinedThrows() {
		// Arrange
		final long id = 13L;
		when(this.timeLogRepository.findById(id))
				.thenReturn(Optional.of(new TimeLog(this.employee, LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS))));
		final UpdateTimeLogRequest req = new UpdateTimeLogRequest(JsonNullable.undefined(), JsonNullable.undefined());

		// Act & Assert
		assertThrows(NullPointerException.class, () -> this.timeLogService.updateTimeLog(id, req));
	}

	@Test
	void testUpdateTimeLogNotFoundThrows() {
		// Arrange
		final long id = 99L;
		when(this.timeLogRepository.findById(id)).thenReturn(Optional.empty());
		final UpdateTimeLogRequest req = new UpdateTimeLogRequest(JsonNullable.of(LocalDateTime.now()),
				JsonNullable.undefined());

		// Act & Assert
		assertThrows(EntityNotFoundException.class, () -> this.timeLogService.updateTimeLog(id, req));
	}

	@Test
	void testUpdateTimeLogEntryLockWindowBoundaryEqualsNowThrows() {
		// Arrange
		final long id = 14L;
		final LocalDateTime fixedNow = LocalDateTime.of(2025, 8, 29, 12, 0, 0);
		when(this.clock.instant()).thenReturn(fixedNow.toInstant(java.time.ZoneOffset.UTC));
		when(this.clock.getZone()).thenReturn(ZoneId.of("UTC"));
		when(this.adminService.getDaysUntilLocked()).thenReturn(3);

		final TimeLog existing = new TimeLog(this.employee, fixedNow.minusDays(10));
		when(this.timeLogRepository.findById(id)).thenReturn(Optional.of(existing));

		final LocalDateTime newEntry = fixedNow.minusDays(3);
		final UpdateTimeLogRequest req = new UpdateTimeLogRequest(JsonNullable.of(newEntry), JsonNullable.undefined());

		// Act & Assert
		assertThrows(TimeLogModificationNotAllowedException.class, () -> this.timeLogService.updateTimeLog(id, req));
		verify(this.timeLogRepository, times(0)).save(existing);
	}

	@Test
	void testUpdateTimeLogExitOutsideEditingWindowThrows() {
		// Arrange
		final long id = 15L;
		final LocalDateTime fixedNow = LocalDateTime.of(2025, 8, 29, 12, 0, 0);
		when(this.clock.instant()).thenReturn(fixedNow.toInstant(java.time.ZoneOffset.UTC));
		when(this.clock.getZone()).thenReturn(ZoneId.of("UTC"));
		when(this.adminService.getDaysUntilLocked()).thenReturn(3);

		final TimeLog existing = new TimeLog(this.employee, fixedNow.minusDays(10));
		when(this.timeLogRepository.findById(id)).thenReturn(Optional.of(existing));

		final LocalDateTime newExit = fixedNow.minusDays(4);
		final UpdateTimeLogRequest req = new UpdateTimeLogRequest(JsonNullable.undefined(), JsonNullable.of(newExit));

		// Act & Assert
		assertThrows(TimeLogModificationNotAllowedException.class, () -> this.timeLogService.updateTimeLog(id, req));
		verify(this.timeLogRepository, times(0)).save(existing);
	}

	@Test
	void testUpdateTimeLogChronologyBothProvidedEntryAfterExitThrows() {
		// Arrange
		final long id = 16L;
		final LocalDateTime fixedNow = LocalDateTime.of(2025, 8, 29, 12, 0, 0);
		when(this.clock.instant()).thenReturn(fixedNow.toInstant(java.time.ZoneOffset.UTC));
		when(this.clock.getZone()).thenReturn(ZoneId.of("UTC"));
		when(this.adminService.getDaysUntilLocked()).thenReturn(10);

		final TimeLog existing = new TimeLog(this.employee, fixedNow.minusDays(20));
		when(this.timeLogRepository.findById(id)).thenReturn(Optional.of(existing));

		final LocalDateTime newEntry = fixedNow.minusHours(1);
		final LocalDateTime newExit = fixedNow.minusHours(2);
		final UpdateTimeLogRequest req = new UpdateTimeLogRequest(JsonNullable.of(newEntry), JsonNullable.of(newExit));

		// Act & Assert
		assertThrows(TimeLogChronologyException.class, () -> this.timeLogService.updateTimeLog(id, req));
		verify(this.timeLogRepository, times(0)).save(existing);
	}

	@Test
	void testUpdateTimeLogChronologyEntryAfterExistingExitThrows() {
		// Arrange
		final long id = 17L;
		final LocalDateTime fixedNow = LocalDateTime.of(2025, 8, 29, 12, 0, 0);
		when(this.clock.instant()).thenReturn(fixedNow.toInstant(java.time.ZoneOffset.UTC));
		when(this.clock.getZone()).thenReturn(ZoneId.of("UTC"));
		when(this.adminService.getDaysUntilLocked()).thenReturn(10);

		final TimeLog existing = new TimeLog(this.employee, fixedNow.minusHours(5));
		existing.setExitTime(fixedNow.minusHours(1));
		when(this.timeLogRepository.findById(id)).thenReturn(Optional.of(existing));

		final LocalDateTime newEntry = fixedNow.minusMinutes(30);
		final UpdateTimeLogRequest req = new UpdateTimeLogRequest(JsonNullable.of(newEntry), JsonNullable.undefined());

		// Act & Assert
		assertThrows(TimeLogChronologyException.class, () -> this.timeLogService.updateTimeLog(id, req));
		verify(this.timeLogRepository, times(0)).save(existing);
	}

	@Test
	void testUpdateTimeLogChronologyExitBeforeExistingEntryThrows() {
		// Arrange
		final long id = 18L;
		final LocalDateTime fixedNow = LocalDateTime.of(2025, 8, 29, 12, 0, 0);
		when(this.clock.instant()).thenReturn(fixedNow.toInstant(java.time.ZoneOffset.UTC));
		when(this.clock.getZone()).thenReturn(ZoneId.of("UTC"));
		when(this.adminService.getDaysUntilLocked()).thenReturn(10);

		final TimeLog existing = new TimeLog(this.employee, fixedNow.minusHours(2)); // entry
		when(this.timeLogRepository.findById(id)).thenReturn(Optional.of(existing));

		final LocalDateTime newExit = fixedNow.minusHours(3);
		final UpdateTimeLogRequest req = new UpdateTimeLogRequest(JsonNullable.undefined(), JsonNullable.of(newExit));

		// Act & Assert
		assertThrows(TimeLogChronologyException.class, () -> this.timeLogService.updateTimeLog(id, req));
		verify(this.timeLogRepository, times(0)).save(existing);
	}

	@Test
	void testDeleteTimeLogSuccessWithinEditingWindow() {
		// Arrange
		final LocalDateTime fixedNow = LocalDateTime.of(2025, 8, 29, 12, 0, 0);
		when(this.clock.instant()).thenReturn(fixedNow.toInstant(java.time.ZoneOffset.UTC));
		when(this.clock.getZone()).thenReturn(ZoneId.of("UTC"));
		when(this.adminService.getDaysUntilLocked()).thenReturn(3);

		final LocalDateTime entryTime = fixedNow.minusDays(2);
		final TimeLog timeLog = new TimeLog(this.employee, entryTime);

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
		final LocalDateTime fixedNow = LocalDateTime.of(2025, 8, 29, 12, 0, 0);
		when(this.clock.instant()).thenReturn(fixedNow.toInstant(java.time.ZoneOffset.UTC));
		when(this.clock.getZone()).thenReturn(ZoneId.of("UTC"));
		when(this.adminService.getDaysUntilLocked()).thenReturn(3);

		final LocalDateTime entryTime = fixedNow.minusDays(4);
		final TimeLog timeLog = new TimeLog(this.employee, entryTime);

		// Act & Assert
		assertThrows(TimeLogModificationNotAllowedException.class, () -> this.timeLogService.deleteTimeLog(timeLog));
		verify(this.timeLogRepository, times(0)).delete(timeLog);
	}

	@Test
	void testDeleteTimeLogBoundaryAtLockInstantShouldThrow() {
		// Arrange
		final LocalDateTime fixedNow = LocalDateTime.of(2025, 8, 29, 12, 0, 0);
		when(this.clock.instant()).thenReturn(fixedNow.toInstant(java.time.ZoneOffset.UTC));
		when(this.clock.getZone()).thenReturn(ZoneId.of("UTC"));
		when(this.adminService.getDaysUntilLocked()).thenReturn(3);

		final LocalDateTime entryTime = fixedNow.minusDays(3);
		final TimeLog timeLog = new TimeLog(this.employee, entryTime);

		// Act & Assert
		assertThrows(TimeLogModificationNotAllowedException.class, () -> this.timeLogService.deleteTimeLog(timeLog));
		verify(this.timeLogRepository, times(0)).delete(timeLog);
	}

}