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
package es.nivel36.janus.service.workshift;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import es.nivel36.janus.service.admin.AdminService;
import es.nivel36.janus.service.employee.Employee;
import es.nivel36.janus.service.schedule.Schedule;
import es.nivel36.janus.service.schedule.ScheduleService;
import es.nivel36.janus.service.schedule.TimeRange;
import es.nivel36.janus.service.timelog.TimeLog;
import es.nivel36.janus.service.timelog.TimeLogService;
import es.nivel36.janus.service.worksite.Worksite;

class WorkShiftServiceTest {

	private static final Logger logger = LoggerFactory.getLogger(WorkShiftServiceTest.class);

	private @Mock WorkshiftRepository workshiftRepository;
	private @Mock TimeLogService timeLogService;
	private @Mock ScheduleService scheduleService;
	private @Mock AdminService adminService;
	private @Mock Clock clock;
	private @InjectMocks WorkShiftService workShiftService;
	private Employee employee;
	private Worksite worksite;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		this.employee = new Employee("Abel", "Ferrer", "aferrer@nivel36.es", new Schedule("CODE", "Name"));
		final ZoneId utcZone = ZoneId.of("UTC");
		this.worksite = new Worksite("BCN-HQ", "Barcelona Headquarters", utcZone);
		employee.assignToWorksite(worksite);
	}

	public static Stream<Arguments> provideTimeLogArguments() {
		final LocalDate date = LocalDate.of(2024, 10, 10);
		final LocalDate previousDay = date.minusDays(1);
		final LocalDate nextDay = date.plusDays(1);
		final Employee employee = new Employee("Abel", "Ferrer", "aferrer@nivel36.es", new Schedule("CODE", "Name"));
		final ZoneId utcZone = ZoneId.of("UTC");
		final Worksite worksite = new Worksite("BCN-HQ", "Barcelona Headquarters", utcZone);
		employee.assignToWorksite(worksite);

		final Instant[] normalDay = { //
				buildInstant(previousDay, 8, 0), // 0
				buildInstant(previousDay, 12, 0), // 1
				buildInstant(previousDay, 13, 0), // 2
				buildInstant(previousDay, 17, 0), // 3
				buildInstant(date, 7, 30), // 4
				buildInstant(date, 8, 30), // 5
				buildInstant(date, 8, 45), // 6
				buildInstant(date, 13, 30), // 7
				buildInstant(date, 14, 30), // 8
				buildInstant(date, 17, 30), // 9
				buildInstant(nextDay, 8, 0), // 10
				buildInstant(nextDay, 12, 0), // 11
				buildInstant(nextDay, 13, 0), // 12
				buildInstant(nextDay, 17, 0) // 13
		};

		final Instant[] crunchDay = { //
				buildInstant(previousDay, 9, 0), // 0
				buildInstant(previousDay, 12, 30), // 1
				buildInstant(previousDay, 14, 0), // 2
				buildInstant(previousDay, 18, 0), // 3
				buildInstant(date, 7, 30), // 4
				buildInstant(date, 8, 30), // 5
				buildInstant(date, 8, 45), // 6
				buildInstant(date, 13, 30), // 7
				buildInstant(date, 14, 30), // 8
				buildInstant(date, 20, 30), // 9
				buildInstant(date, 21, 30), // 10
				buildInstant(nextDay, 02, 30), // 11
				buildInstant(nextDay, 9, 0), // 12
				buildInstant(nextDay, 12, 30), // 13
				buildInstant(nextDay, 14, 0), // 14
				buildInstant(nextDay, 18, 0) // 15
		};

		final Instant[] halfDay = { //
				buildInstant(previousDay, 7, 30), // 0
				buildInstant(previousDay, 11, 30), // 1
				buildInstant(previousDay, 12, 30), // 2
				buildInstant(previousDay, 16, 30), // 3
				buildInstant(date, 8, 30), // 4
				buildInstant(date, 13, 30), // 5
				buildInstant(nextDay, 7, 30), // 6
				buildInstant(nextDay, 11, 30), // 7
				buildInstant(nextDay, 12, 30), // 8
				buildInstant(nextDay, 16, 30) // 9
		}; //

		final Instant[] halfDayWithCrunch = { //
				buildInstant(previousDay, 7, 30), // 0
				buildInstant(previousDay, 11, 30), // 1
				buildInstant(previousDay, 12, 30), // 2
				buildInstant(previousDay, 16, 30), // 3
				buildInstant(date, 8, 30), // 4
				buildInstant(date, 13, 30), // 5
				buildInstant(date, 16, 30), // 6
				buildInstant(date, 22, 30), // 7
				buildInstant(nextDay, 7, 30), // 8
				buildInstant(nextDay, 11, 30), // 9
				buildInstant(nextDay, 12, 30), // 10
				buildInstant(nextDay, 16, 30) // 11
		}; //

		final Instant[] frydayWithHalfDayAndCrunch = { //
				buildInstant(previousDay, 7, 30), // 0
				buildInstant(previousDay, 11, 30), // 1
				buildInstant(previousDay, 12, 30), // 2
				buildInstant(previousDay, 16, 30), // 3
				buildInstant(date, 8, 30), // 4
				buildInstant(date, 13, 30), // 5
				buildInstant(date, 16, 30), // 6
				buildInstant(date, 22, 30), // 7
		}; //

		final Instant[] monday = { //
				buildInstant(date, 7, 30), // 0
				buildInstant(date, 8, 30), // 1
				buildInstant(date, 8, 45), // 2
				buildInstant(date, 13, 30), // 3
				buildInstant(date, 14, 30), // 4
				buildInstant(date, 17, 30), // 5
				buildInstant(nextDay, 9, 0), // 6
				buildInstant(nextDay, 12, 30), // 7
				buildInstant(nextDay, 14, 0), // 8
				buildInstant(nextDay, 18, 0) // 9
		};

		final Instant[] friday = { //
				buildInstant(previousDay, 8, 0), // 0
				buildInstant(previousDay, 12, 0), // 1
				buildInstant(previousDay, 13, 0), // 2
				buildInstant(previousDay, 17, 0), // 3
				buildInstant(date, 7, 30), // 4
				buildInstant(date, 8, 30), // 5
				buildInstant(date, 8, 45), // 6
				buildInstant(date, 13, 30), // 7
				buildInstant(date, 14, 30), // 8
				buildInstant(date, 17, 30), // 9
		};

		final Instant[] isolatedDay = { //
				buildInstant(date, 8, 0), // 0
				buildInstant(date, 12, 0), // 1
				buildInstant(date, 13, 0), // 2
				buildInstant(date, 17, 0), // 3
		};

		return Stream.of( //
				Arguments.of( //
						Arrays.asList( //
								new TimeLog(employee, worksite, normalDay[0], normalDay[1]), //
								new TimeLog(employee, worksite, normalDay[2], normalDay[3]), //
								new TimeLog(employee, worksite, normalDay[4], normalDay[5]), //
								new TimeLog(employee, worksite, normalDay[6], normalDay[7]), //
								new TimeLog(employee, worksite, normalDay[8], normalDay[9]), //
								new TimeLog(employee, worksite, normalDay[10], normalDay[11]), //
								new TimeLog(employee, worksite, normalDay[12], normalDay[13]) //
						), //
						Duration.ofHours(8).plusMinutes(45), //
						Duration.ofHours(1).plusMinutes(15), //
						normalDay[4], //
						normalDay[9], //
						LocalTime.of(8, 0), //
						LocalTime.of(18, 0) //
				), //
				Arguments.of( //
						Arrays.asList( //
								new TimeLog(employee, worksite, crunchDay[0], crunchDay[1]), //
								new TimeLog(employee, worksite, crunchDay[2], crunchDay[3]), //
								new TimeLog(employee, worksite, crunchDay[4], crunchDay[5]), //
								new TimeLog(employee, worksite, crunchDay[6], crunchDay[7]), //
								new TimeLog(employee, worksite, crunchDay[8], crunchDay[9]), //
								new TimeLog(employee, worksite, crunchDay[10], crunchDay[11]), //
								new TimeLog(employee, worksite, crunchDay[12], crunchDay[13]), //
								new TimeLog(employee, worksite, crunchDay[14], crunchDay[15]) //
						), //
						Duration.ofHours(16).plusMinutes(45), //
						Duration.ofHours(2).plusMinutes(15), // ECTZone
						crunchDay[4], //
						crunchDay[11], //
						LocalTime.of(8, 0), //
						LocalTime.of(18, 0) //
				), Arguments.of( //
						Arrays.asList( //
								new TimeLog(employee, worksite, halfDay[0], halfDay[1]), //
								new TimeLog(employee, worksite, halfDay[2], halfDay[3]), //
								new TimeLog(employee, worksite, halfDay[4], halfDay[5]), //
								new TimeLog(employee, worksite, halfDay[6], halfDay[7]), //
								new TimeLog(employee, worksite, halfDay[8], halfDay[9]) //
						), //
						Duration.ofHours(5), //
						Duration.ofHours(0), //
						halfDay[4], //
						halfDay[5], //
						LocalTime.of(8, 0), //
						LocalTime.of(18, 0) //
				), Arguments.of( //
						Arrays.asList( //
								new TimeLog(employee, worksite, halfDayWithCrunch[0], halfDayWithCrunch[1]), //
								new TimeLog(employee, worksite, halfDayWithCrunch[2], halfDayWithCrunch[3]), //
								new TimeLog(employee, worksite, halfDayWithCrunch[4], halfDayWithCrunch[5]), //
								new TimeLog(employee, worksite, halfDayWithCrunch[6], halfDayWithCrunch[7]), //
								new TimeLog(employee, worksite, halfDayWithCrunch[8], halfDayWithCrunch[9]), //
								new TimeLog(employee, worksite, halfDayWithCrunch[10], halfDayWithCrunch[11]) //
						), //
						Duration.ofHours(11), //
						Duration.ofHours(3), //
						halfDayWithCrunch[4], //
						halfDayWithCrunch[7], //
						LocalTime.of(8, 0), //
						LocalTime.of(18, 0) //
				), Arguments.of( //
						Arrays.asList( //
								new TimeLog(employee, worksite, frydayWithHalfDayAndCrunch[0],
										frydayWithHalfDayAndCrunch[1]), //
								new TimeLog(employee, worksite, frydayWithHalfDayAndCrunch[2],
										frydayWithHalfDayAndCrunch[3]), //
								new TimeLog(employee, worksite, frydayWithHalfDayAndCrunch[4],
										frydayWithHalfDayAndCrunch[5]), //
								new TimeLog(employee, worksite, frydayWithHalfDayAndCrunch[6],
										frydayWithHalfDayAndCrunch[7]) //
						), //
						Duration.ofHours(11), //
						Duration.ofHours(3), //
						frydayWithHalfDayAndCrunch[4], //
						frydayWithHalfDayAndCrunch[7], //
						LocalTime.of(8, 0), //
						LocalTime.of(18, 0) //
				), Arguments.of( //
						Arrays.asList( //
								new TimeLog(employee, worksite, monday[0], monday[1]), //
								new TimeLog(employee, worksite, monday[2], monday[3]), //
								new TimeLog(employee, worksite, monday[4], monday[5]), //
								new TimeLog(employee, worksite, monday[6], monday[7]), //
								new TimeLog(employee, worksite, monday[8], monday[9]) //
						), //
						Duration.ofHours(8).plusMinutes(45), //
						Duration.ofHours(1).plusMinutes(15), //
						monday[0], //
						monday[5], //
						LocalTime.of(8, 0), //
						LocalTime.of(18, 0) //
				), Arguments.of( //
						Arrays.asList( //
								new TimeLog(employee, worksite, friday[0], friday[1]), //
								new TimeLog(employee, worksite, friday[2], friday[3]), //
								new TimeLog(employee, worksite, friday[4], friday[5]), //
								new TimeLog(employee, worksite, friday[6], friday[7]), //
								new TimeLog(employee, worksite, friday[8], friday[9]) //
						), //
						Duration.ofHours(8).plusMinutes(45), //
						Duration.ofHours(1).plusMinutes(15), //
						friday[4], //
						friday[9], //
						LocalTime.of(8, 0), //
						LocalTime.of(18, 0) //
				), Arguments.of( //
						Arrays.asList( //
								new TimeLog(employee, worksite, isolatedDay[0], isolatedDay[1]), //
								new TimeLog(employee, worksite, isolatedDay[2], isolatedDay[3]) //
						), //
						Duration.ofHours(8), //
						Duration.ofHours(1), //
						isolatedDay[0], //
						isolatedDay[3], //
						LocalTime.of(8, 0), //
						LocalTime.of(18, 0) //
				) //
		); //
	}

	private static Instant buildInstant(LocalDate date, int hour, int minutes) {
		final ZoneId utcZone = ZoneId.of("UTC");
		return LocalDateTime.of(date, LocalTime.of(hour, minutes)).atZone(utcZone).toInstant();
	}

	@ParameterizedTest
	@MethodSource("provideTimeLogArguments")
	void testFindWorkShiftForWorkingDay(final List<TimeLog> timeLogs, final Duration expectedTotalWorkTime,
			final Duration expectedTotalPauseTime, Instant startWorkShiftTime, Instant endWorkShiftTime,
			LocalTime startTime, LocalTime endTime) {
		logger.info("Test find work shift for working day");
		// Arrange
		final LocalDate date = LocalDate.of(2024, 10, 10);
		final TimeRange timeRange = new TimeRange(startTime, endTime);
		final Pageable page = Pageable.unpaged();

		final ZoneId z = worksite.getTimeZone();
		final Instant fromInstant = date.atStartOfDay(z).toInstant().minus(1, ChronoUnit.DAYS);
		final Instant toInstant = date.atStartOfDay(z).toInstant().plus(2, ChronoUnit.DAYS);

		final Instant fixedNow = LocalDateTime.of(2025, 8, 29, 12, 0, 0).toInstant(ZoneOffset.UTC);
		when(this.clock.instant()).thenReturn(fixedNow);

		when(timeLogService.searchTimeLogsByEmployeeAndEntryTimeInRange(employee, fromInstant, toInstant, page))
				.thenReturn(new PageImpl<>(timeLogs, page, timeLogs.size()));
		when(this.scheduleService.findTimeRangeForEmployeeByDate(employee, date)).thenReturn(Optional.of(timeRange));
		when(adminService.getDaysUntilLocked()).thenReturn(7);

		// Act
		final WorkShift result = this.workShiftService.findWorkShift(this.employee, this.worksite, date);

		// Assert
		assertNotNull(result);
		assertEquals(this.employee, result.getEmployee());
		assertEquals(expectedTotalWorkTime, result.getTotalWorkTime());
		assertEquals(expectedTotalPauseTime, result.getTotalPauseTime());
	}

	@ParameterizedTest
	@MethodSource("provideTimeLogArguments")
	void testFindWorkShiftForNonWorkingDay(final List<TimeLog> timeLogs, final Duration expectedTotalWorkTime,
			final Duration expectedTotalPauseTime, Instant startWorkShiftTime, Instant endWorkShiftTime) {
		logger.info("Test find work shift for non working day");
		// Arrange
		final LocalDate date = LocalDate.of(2024, 10, 10);
		final Pageable page = Pageable.unpaged();

		final ZoneId z = worksite.getTimeZone();
		final Instant fromInstant = date.atStartOfDay(z).toInstant().minus(1, ChronoUnit.DAYS);
		final Instant toInstant = date.atStartOfDay(z).toInstant().plus(2, ChronoUnit.DAYS);

		final Instant fixedNow = LocalDateTime.of(2025, 8, 29, 12, 0, 0).toInstant(ZoneOffset.UTC);
		when(this.clock.instant()).thenReturn(fixedNow);

		when(timeLogService.searchTimeLogsByEmployeeAndEntryTimeInRange(employee, fromInstant, toInstant, page))
				.thenReturn(new PageImpl<>(timeLogs, page, timeLogs.size()));
		when(scheduleService.findTimeRangeForEmployeeByDate(employee, date)).thenReturn(Optional.empty());
		when(adminService.getDaysUntilLocked()).thenReturn(7);

		// Act
		final WorkShift result = this.workShiftService.findWorkShift(this.employee, this.worksite, date);

		// Assert
		assertNotNull(result);
		assertEquals(this.employee, result.getEmployee());
		assertEquals(expectedTotalWorkTime, result.getTotalWorkTime());
		assertEquals(expectedTotalPauseTime, result.getTotalPauseTime());
	}
}
