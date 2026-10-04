/*
 * Copyright 2026 Abel Ferrer Jiménez
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package es.nivel36.janus.service.workshift;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import es.nivel36.janus.service.applicationsettings.ApplicationSettingsService;
import es.nivel36.janus.service.employee.Employee;
import es.nivel36.janus.service.employee.EmployeeService;
import es.nivel36.janus.service.schedule.Schedule;
import es.nivel36.janus.service.schedule.ScheduleService;
import es.nivel36.janus.service.schedule.TimeRange;
import es.nivel36.janus.service.timelog.TimeLog;
import es.nivel36.janus.service.timelog.TimeLogService;
import es.nivel36.janus.service.timelog.TimeLogs;
import es.nivel36.janus.service.worksite.Worksite;

@ExtendWith(MockitoExtension.class)
class WorkShiftPrecomputeJobTest {

	private @Mock WorkshiftRepository workshiftRepository;
	private @Mock TimeLogService timeLogService;
	private @Mock ScheduleService scheduleService;
	private @Mock EmployeeService employeeService;
	private @Mock ApplicationSettingsService applicationSettingsService;

	private Employee employee;
	private Worksite worksite;

	@BeforeEach
	void setUp() {
		this.employee = new Employee(
				"EMP-0001",
				"Abel",
				"Ferrer",
				"aferrer@nivel36.es",
				new Schedule("NIGHT", "Night", Duration.ZERO, Duration.ZERO));
		this.worksite = new Worksite("BCN-HQ", "Barcelona Headquarters", ZoneOffset.UTC);
		this.worksite.assignEmployee(this.employee);
	}

	@Test
	void precomputesLogsAcrossMidnightAsOneOvernightShift() {
		final LocalDate shiftDay = LocalDate.of(2026, 9, 28);
		final TimeLog beforeMidnight = this.timeLog(shiftDay, "22:00:00Z", shiftDay.plusDays(1), "00:00:00Z");
		final TimeLog afterMidnight = this
				.timeLog(shiftDay.plusDays(1), "00:30:00Z", shiftDay.plusDays(1), "06:00:00Z");
		final TimeRange overnight = new TimeRange(LocalTime.of(22, 0), LocalTime.of(6, 0));

		when(this.applicationSettingsService.getDaysUntilLocked()).thenReturn(0);
		when(this.employeeService.findEmployeesWithoutWorkshiftsSince(any())).thenReturn(List.of(1L));
		when(this.employeeService.findEmployeeById(1L)).thenReturn(this.employee);
		when(this.timeLogService.findOrphanTimeLogs(any(), any()))
				.thenReturn(new TimeLogs(List.of(beforeMidnight, afterMidnight)));
		when(this.scheduleService.findTimeRangeForEmployeeByDate(this.employee, shiftDay.minusDays(1)))
				.thenReturn(Optional.empty());
		when(this.scheduleService.findTimeRangeForEmployeeByDate(this.employee, shiftDay))
				.thenReturn(Optional.of(overnight));
		when(this.workshiftRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		final Clock clock = Clock.fixed(Instant.parse("2026-10-01T02:15:00Z"), ZoneOffset.UTC);
		new WorkShiftPrecomputeJob(
				this.workshiftRepository,
				this.timeLogService,
				this.scheduleService,
				this.employeeService,
				this.applicationSettingsService,
				clock).run();

		final ArgumentCaptor<WorkShift> captor = ArgumentCaptor.forClass(WorkShift.class);
		verify(this.workshiftRepository).save(captor.capture());
		assertThat(captor.getValue().getDate()).isEqualTo(shiftDay);
		assertThat(captor.getValue().getTimeLogs()).containsExactly(beforeMidnight, afterMidnight);
		assertThat(captor.getValue().getTotalWorkTime()).isEqualTo(Duration.ofHours(7).plusMinutes(30));
	}

	private TimeLog timeLog(
			final LocalDate entryDay,
			final String entryTime,
			final LocalDate exitDay,
			final String exitTime) {
		return new TimeLog(
				this.employee,
				this.worksite,
				Instant.parse(entryDay + "T" + entryTime),
				Instant.parse(exitDay + "T" + exitTime));
	}
}
