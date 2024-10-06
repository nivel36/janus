package es.nivel36.janus.service.timelog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import es.nivel36.janus.service.employee.Employee;

class TimeLogServiceTest {

	private @Mock TimeLogRepository timeLogRepository;
	private @InjectMocks TimeLogService timeLogService;
	private Employee employee;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		this.employee = new Employee();
	}

	@Test
	void testClockInSuccess() {
		final LocalDateTime now = LocalDateTime.now();

		// Act
		final TimeLog timeLog = this.timeLogService.clockIn(this.employee, now);

		// Assert
		assertNotNull(timeLog);
		assertEquals(this.employee, timeLog.getEmployee());
		assertEquals(now, timeLog.getEntryTime());
		verify(this.timeLogRepository, times(1)).createTimeLog(timeLog);
	}

	@Test
	void testClockInWithNullEmployee() {
		assertThrows(NullPointerException.class, () -> {
			this.timeLogService.clockIn(null, LocalDateTime.now());
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
		final LocalDateTime now = LocalDateTime.now();
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
			this.timeLogService.clockOut(this.employee, LocalDateTime.now());
		});
	}

	@Test
	void testFindTimeLogByIdSuccess() {
		final long validId = 1L;
		final TimeLog expectedTimeLog = new TimeLog(this.employee, LocalDateTime.now());

		when(this.timeLogRepository.findTimeLogById(validId)).thenReturn(expectedTimeLog);

		// Act
		final TimeLog result = this.timeLogService.findTimeLogById(validId);

		// Assert
		assertNotNull(result);
		assertEquals(expectedTimeLog, result);
	}

	@Test
	void testFindTimeLogByIdInvalidId() {
		assertThrows(IllegalArgumentException.class, () -> {
			this.timeLogService.findTimeLogById(-1L);
		});
	}

	@Test
	void testFindLastTimeLogByEmployeeSuccess() {
		final TimeLog expectedTimeLog = new TimeLog(this.employee, LocalDateTime.now());

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
}