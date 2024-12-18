
package es.nivel36.janus.service.workshift;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import es.nivel36.janus.service.employee.Employee;
import es.nivel36.janus.service.schedule.ScheduleService;
import es.nivel36.janus.service.schedule.TimeRange;
import es.nivel36.janus.service.timelog.TimeLog;
import es.nivel36.janus.service.timelog.TimeLogService;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WorkShiftServiceTest {

	private @Mock TimeLogService timeLogService;
	private @Mock ScheduleService scheduleService;
	private @InjectMocks WorkShiftService workShiftService;
	private Employee employee;

	@BeforeEach
	void setUp() {
		this.employee = new Employee();
	}

	public static Stream<Arguments> provideTimeLogArguments() {
		final LocalDate date = LocalDate.of(2024, 10, 10);
		final LocalDate previousDay = date.minusDays(1);
		final LocalDate nextDay = date.plusDays(1);
		final Employee employee = new Employee();
		employee.setEmail("aaron@test,com");

		final LocalDateTime[] normalDay = { //
				LocalDateTime.of(previousDay, LocalTime.of(8, 0)), // 0
				LocalDateTime.of(previousDay, LocalTime.of(12, 0)), // 1
				LocalDateTime.of(previousDay, LocalTime.of(13, 0)), // 2
				LocalDateTime.of(previousDay, LocalTime.of(17, 0)), // 3
				LocalDateTime.of(date, LocalTime.of(7, 30)), // 4
				LocalDateTime.of(date, LocalTime.of(8, 30)), // 5
				LocalDateTime.of(date, LocalTime.of(8, 45)), // 6
				LocalDateTime.of(date, LocalTime.of(13, 30)), // 7
				LocalDateTime.of(date, LocalTime.of(14, 30)), // 8
				LocalDateTime.of(date, LocalTime.of(17, 30)), // 9
				LocalDateTime.of(nextDay, LocalTime.of(8, 0)), // 10
				LocalDateTime.of(nextDay, LocalTime.of(12, 0)), // 11
				LocalDateTime.of(nextDay, LocalTime.of(13, 0)), // 12
				LocalDateTime.of(nextDay, LocalTime.of(17, 0)) // 13
		};

		final LocalDateTime[] crunchDay = { //
				LocalDateTime.of(previousDay, LocalTime.of(9, 0)), // 0
				LocalDateTime.of(previousDay, LocalTime.of(12, 30)), // 1
				LocalDateTime.of(previousDay, LocalTime.of(14, 0)), // 2
				LocalDateTime.of(previousDay, LocalTime.of(18, 0)), // 3
				LocalDateTime.of(date, LocalTime.of(7, 30)), // 4
				LocalDateTime.of(date, LocalTime.of(8, 30)), // 5
				LocalDateTime.of(date, LocalTime.of(8, 45)), // 6
				LocalDateTime.of(date, LocalTime.of(13, 30)), // 7
				LocalDateTime.of(date, LocalTime.of(14, 30)), // 8
				LocalDateTime.of(date, LocalTime.of(20, 30)), // 9
				LocalDateTime.of(date, LocalTime.of(21, 30)), // 10
				LocalDateTime.of(nextDay, LocalTime.of(02, 30)), // 11
				LocalDateTime.of(nextDay, LocalTime.of(9, 0)), // 12
				LocalDateTime.of(nextDay, LocalTime.of(12, 30)), // 13
				LocalDateTime.of(nextDay, LocalTime.of(14, 0)), // 14
				LocalDateTime.of(nextDay, LocalTime.of(18, 0)) // 15
		};

		final LocalDateTime[] halfDay = { //
				LocalDateTime.of(previousDay, LocalTime.of(7, 30)), // 0
				LocalDateTime.of(previousDay, LocalTime.of(11, 30)), // 1
				LocalDateTime.of(previousDay, LocalTime.of(12, 30)), // 2
				LocalDateTime.of(previousDay, LocalTime.of(16, 30)), // 3
				LocalDateTime.of(date, LocalTime.of(8, 30)), // 4
				LocalDateTime.of(date, LocalTime.of(13, 30)), // 5
				LocalDateTime.of(nextDay, LocalTime.of(7, 30)), // 6
				LocalDateTime.of(nextDay, LocalTime.of(11, 30)), // 7
				LocalDateTime.of(nextDay, LocalTime.of(12, 30)), // 8
				LocalDateTime.of(nextDay, LocalTime.of(16, 30)) // 9
		}; //

		final LocalDateTime[] halfDayWithCrunch = { //
				LocalDateTime.of(previousDay, LocalTime.of(7, 30)), // 0
				LocalDateTime.of(previousDay, LocalTime.of(11, 30)), // 1
				LocalDateTime.of(previousDay, LocalTime.of(12, 30)), // 2
				LocalDateTime.of(previousDay, LocalTime.of(16, 30)), // 3
				LocalDateTime.of(date, LocalTime.of(8, 30)), // 4
				LocalDateTime.of(date, LocalTime.of(13, 30)), // 5
				LocalDateTime.of(date, LocalTime.of(16, 30)), // 6
				LocalDateTime.of(date, LocalTime.of(22, 30)), // 7
				LocalDateTime.of(nextDay, LocalTime.of(7, 30)), // 8
				LocalDateTime.of(nextDay, LocalTime.of(11, 30)), // 9
				LocalDateTime.of(nextDay, LocalTime.of(12, 30)), // 10
				LocalDateTime.of(nextDay, LocalTime.of(16, 30)) // 11
		}; //

		final LocalDateTime[] frydayWithHalfDayAndCrunch = { //
				LocalDateTime.of(previousDay, LocalTime.of(7, 30)), // 0
				LocalDateTime.of(previousDay, LocalTime.of(11, 30)), // 1
				LocalDateTime.of(previousDay, LocalTime.of(12, 30)), // 2
				LocalDateTime.of(previousDay, LocalTime.of(16, 30)), // 3
				LocalDateTime.of(date, LocalTime.of(8, 30)), // 4
				LocalDateTime.of(date, LocalTime.of(13, 30)), // 5
				LocalDateTime.of(date, LocalTime.of(16, 30)), // 6
				LocalDateTime.of(date, LocalTime.of(22, 30)), // 7
		}; //

		final LocalDateTime[] monday = { //
				LocalDateTime.of(date, LocalTime.of(7, 30)), // 0
				LocalDateTime.of(date, LocalTime.of(8, 30)), // 1
				LocalDateTime.of(date, LocalTime.of(8, 45)), // 2
				LocalDateTime.of(date, LocalTime.of(13, 30)), // 3
				LocalDateTime.of(date, LocalTime.of(14, 30)), // 4
				LocalDateTime.of(date, LocalTime.of(17, 30)), // 5
				LocalDateTime.of(nextDay, LocalTime.of(9, 0)), // 6
				LocalDateTime.of(nextDay, LocalTime.of(12, 30)), // 7
				LocalDateTime.of(nextDay, LocalTime.of(14, 0)), // 8
				LocalDateTime.of(nextDay, LocalTime.of(18, 0)) // 9
		};

		final LocalDateTime[] friday = { //
				LocalDateTime.of(previousDay, LocalTime.of(8, 0)), // 0
				LocalDateTime.of(previousDay, LocalTime.of(12, 0)), // 1
				LocalDateTime.of(previousDay, LocalTime.of(13, 0)), // 2
				LocalDateTime.of(previousDay, LocalTime.of(17, 0)), // 3
				LocalDateTime.of(date, LocalTime.of(7, 30)), // 4
				LocalDateTime.of(date, LocalTime.of(8, 30)), // 5
				LocalDateTime.of(date, LocalTime.of(8, 45)), // 6
				LocalDateTime.of(date, LocalTime.of(13, 30)), // 7
				LocalDateTime.of(date, LocalTime.of(14, 30)), // 8
				LocalDateTime.of(date, LocalTime.of(17, 30)), // 9
		};

		final LocalDateTime[] isolatedDay = { //
				LocalDateTime.of(date, LocalTime.of(8, 0)), // 0
				LocalDateTime.of(date, LocalTime.of(12, 0)), // 1
				LocalDateTime.of(date, LocalTime.of(13, 0)), // 2
				LocalDateTime.of(date, LocalTime.of(17, 0)), // 3
		};

		return Stream.of( //
				Arguments.of( //
						Arrays.asList( //
								new TimeLog(employee, normalDay[0], normalDay[1]), //
								new TimeLog(employee, normalDay[2], normalDay[3]), //
								new TimeLog(employee, normalDay[4], normalDay[5]), //
								new TimeLog(employee, normalDay[6], normalDay[7]), //
								new TimeLog(employee, normalDay[8], normalDay[9]), //
								new TimeLog(employee, normalDay[10], normalDay[11]), //
								new TimeLog(employee, normalDay[12], normalDay[13]) //
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
								new TimeLog(employee, crunchDay[0], crunchDay[1]), //
								new TimeLog(employee, crunchDay[2], crunchDay[3]), //
								new TimeLog(employee, crunchDay[4], crunchDay[5]), //
								new TimeLog(employee, crunchDay[6], crunchDay[7]), //
								new TimeLog(employee, crunchDay[8], crunchDay[9]), //
								new TimeLog(employee, crunchDay[10], crunchDay[11]), //
								new TimeLog(employee, crunchDay[12], crunchDay[13]), //
								new TimeLog(employee, crunchDay[14], crunchDay[15]) //
						), //
						Duration.ofHours(16).plusMinutes(45), //
						Duration.ofHours(2).plusMinutes(15), //
						crunchDay[4], //
						crunchDay[11], //
						LocalTime.of(8, 0), //
						LocalTime.of(18, 0) //
				), Arguments.of( //
						Arrays.asList( //
								new TimeLog(employee, halfDay[0], halfDay[1]), //
								new TimeLog(employee, halfDay[2], halfDay[3]), //
								new TimeLog(employee, halfDay[4], halfDay[5]), //
								new TimeLog(employee, halfDay[6], halfDay[7]), //
								new TimeLog(employee, halfDay[8], halfDay[9]) //
						), //
						Duration.ofHours(5), //
						Duration.ofHours(0), //
						halfDay[4], //
						halfDay[5], //
						LocalTime.of(8, 0), //
						LocalTime.of(18, 0) //
				), Arguments.of( //
						Arrays.asList( //
								new TimeLog(employee, halfDayWithCrunch[0], halfDayWithCrunch[1]), //
								new TimeLog(employee, halfDayWithCrunch[2], halfDayWithCrunch[3]), //
								new TimeLog(employee, halfDayWithCrunch[4], halfDayWithCrunch[5]), //
								new TimeLog(employee, halfDayWithCrunch[6], halfDayWithCrunch[7]), //
								new TimeLog(employee, halfDayWithCrunch[8], halfDayWithCrunch[9]), //
								new TimeLog(employee, halfDayWithCrunch[10], halfDayWithCrunch[11]) //
						), //
						Duration.ofHours(11), //
						Duration.ofHours(3), //
						halfDayWithCrunch[4], //
						halfDayWithCrunch[7], //
						LocalTime.of(8, 0), //
						LocalTime.of(18, 0) //
				), Arguments.of( //
						Arrays.asList( //
								new TimeLog(employee, frydayWithHalfDayAndCrunch[0], frydayWithHalfDayAndCrunch[1]), //
								new TimeLog(employee, frydayWithHalfDayAndCrunch[2], frydayWithHalfDayAndCrunch[3]), //
								new TimeLog(employee, frydayWithHalfDayAndCrunch[4], frydayWithHalfDayAndCrunch[5]), //
								new TimeLog(employee, frydayWithHalfDayAndCrunch[6], frydayWithHalfDayAndCrunch[7]) //
						), //
						Duration.ofHours(11), //
						Duration.ofHours(3), //
						frydayWithHalfDayAndCrunch[4], //
						frydayWithHalfDayAndCrunch[7], //
						LocalTime.of(8, 0), //
						LocalTime.of(18, 0) //
				), Arguments.of( //
						Arrays.asList( //
								new TimeLog(employee, monday[0], monday[1]), //
								new TimeLog(employee, monday[2], monday[3]), //
								new TimeLog(employee, monday[4], monday[5]), //
								new TimeLog(employee, monday[6], monday[7]), //
								new TimeLog(employee, monday[8], monday[9]) //
						), //
						Duration.ofHours(8).plusMinutes(45), //
						Duration.ofHours(1).plusMinutes(15), //
						monday[0], //
						monday[5], //
						LocalTime.of(8, 0), //
						LocalTime.of(18, 0) //
				), Arguments.of( //
						Arrays.asList( //
								new TimeLog(employee, friday[0], friday[1]), //
								new TimeLog(employee, friday[2], friday[3]), //
								new TimeLog(employee, friday[4], friday[5]), //
								new TimeLog(employee, friday[6], friday[7]), //
								new TimeLog(employee, friday[8], friday[9]) //
						), //
						Duration.ofHours(8).plusMinutes(45), //
						Duration.ofHours(1).plusMinutes(15), //
						friday[4], //
						friday[9], //
						LocalTime.of(8, 0), //
						LocalTime.of(18, 0) //
				), Arguments.of( //
						Arrays.asList( //
								new TimeLog(employee, isolatedDay[0], isolatedDay[1]), //
								new TimeLog(employee, isolatedDay[2], isolatedDay[3]) //
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

	@ParameterizedTest
	@MethodSource("provideTimeLogArguments")
	void testGetWorkShiftForWorkingDay(final List<TimeLog> timeLogs, final Duration expectedTotalWorkTime,
			final Duration expectedTotalPauseTime, LocalDateTime startWorkShiftTime, LocalDateTime endWorkShiftTime,
			LocalTime startTime, LocalTime endTime) {
		// Arrange
		final LocalDate date = LocalDate.of(2024, 10, 10);
		final TimeRange timeRange = new TimeRange(startTime,endTime);

		when(this.timeLogService.findTimeLogsByEmployeeAndDate(this.employee, date)).thenReturn(timeLogs);
		when(this.scheduleService.findTimeRangeForEmployeeByDate(this.employee, date))
				.thenReturn(Optional.of(timeRange));

		// Act
		final WorkShift result = this.workShiftService.getWorkShift(this.employee, date);

		// Assert
		assertNotNull(result);
		assertEquals(this.employee, result.getEmployee());
		assertEquals(expectedTotalWorkTime, result.getTotalWorkTime());
		assertEquals(expectedTotalPauseTime, result.getTotalPauseTime());
		assertEquals(startWorkShiftTime, result.getStartDateTime());
		assertEquals(endWorkShiftTime, result.getEndDateTime());
	}

	@ParameterizedTest
	@MethodSource("provideTimeLogArguments")
	void testGetWorkShiftForNonWorkingDay(final List<TimeLog> timeLogs, final Duration expectedTotalWorkTime,
			final Duration expectedTotalPauseTime, LocalDateTime startWorkShiftTime, LocalDateTime endWorkShiftTime) {
		// Arrange
		final LocalDate date = LocalDate.of(2024, 10, 10);

		when(this.timeLogService.findTimeLogsByEmployeeAndDate(this.employee, date)).thenReturn(timeLogs);
		when(this.scheduleService.findTimeRangeForEmployeeByDate(this.employee, date)).thenReturn(Optional.empty());

		// Act
		final WorkShift result = this.workShiftService.getWorkShift(this.employee, date);

		// Assert
		assertNotNull(result);
		assertEquals(this.employee, result.getEmployee());
		assertEquals(expectedTotalWorkTime, result.getTotalWorkTime());
		assertEquals(expectedTotalPauseTime, result.getTotalPauseTime());
		assertEquals(startWorkShiftTime, result.getStartDateTime());
		assertEquals(endWorkShiftTime, result.getEndDateTime());
	}
}
