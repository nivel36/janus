package es.nivel36.janus.web;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.omnifaces.cdi.Param;
import org.omnifaces.cdi.ViewScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.nivel36.janus.service.employee.Employee;
import es.nivel36.janus.service.schedule.ScheduleService;
import es.nivel36.janus.service.schedule.TimeRange;
import es.nivel36.janus.service.timelog.TimeLog;
import es.nivel36.janus.service.timelog.TimeLogService;
import es.nivel36.janus.service.workshift.WorkShift;
import es.nivel36.janus.service.workshift.WorkShiftService;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named
@ViewScoped
public class WorkShiftView extends AbstractView {

	private static final long serialVersionUID = 1L;
	private static final Logger logger = LoggerFactory.getLogger(WorkShiftView.class);
	public static final String URL = "/workshift";

	private transient @Inject TimeLogService timeLogService;
	private transient @Inject WorkShiftService workShiftService;
	private transient @Inject ScheduleService scheduleService;
	private @Param Employee employee;
	private WorkShift workShift;

	private TimeRange timeRange;

	@PostConstruct
	public void init() {
		// this.workShift = this.workShiftService.getWorkShift(employee,
		// LocalDate.now());
		// this.timeRange =
		// this.scheduleService.findTimeRangeForEmployeeByDate(this.employee,
		// LocalDate.now())
		// .orElse(null);
		List<TimeLog> timeLogs = new ArrayList<>();
		TimeLog tl1 = new TimeLog();
		tl1.setEmployee(employee);
		tl1.setEntryTime(LocalDateTime.of(LocalDate.now(), LocalTime.of(7, 30)));
		tl1.setExitTime(LocalDateTime.of(LocalDate.now(), LocalTime.of(8, 30)));
		timeLogs.add(tl1);

		TimeLog tl2 = new TimeLog();
		tl2.setEmployee(employee);
		tl2.setEntryTime(LocalDateTime.of(LocalDate.now(), LocalTime.of(8, 45)));
		tl2.setExitTime(LocalDateTime.of(LocalDate.now(), LocalTime.of(13, 30)));
		timeLogs.add(tl2);

		TimeLog tl3 = new TimeLog();
		tl3.setEmployee(employee);
		tl3.setEntryTime(LocalDateTime.of(LocalDate.now(), LocalTime.of(14, 30)));
		tl3.setExitTime(LocalDateTime.of(LocalDate.now(), LocalTime.of(17, 30)));
		timeLogs.add(tl3);

		this.workShift = new WorkShift();
		this.workShift.setEmployee(employee);
		this.workShift.setStartDateTime(LocalDate.now().atTime(8, 0));
		this.workShift.setEndDateTime(LocalDate.now().atTime(17, 30));
		this.workShift.setTimeLogs(timeLogs);

		this.timeRange = new TimeRange(LocalTime.of(8, 0), LocalTime.of(17, 30));
	}

	public TimeRange getTimeRange() {
		return timeRange;
	}

	public Duration getHoursWorked(final TimeLog timeLog) {
		Objects.requireNonNull(timeLog, "TimeLog can't be null");
		return this.timeLogService.getHoursWorked(timeLog);
	}

	public Employee getEmployee() {
		return this.employee;
	}

	public void setWorkShiftService(WorkShiftService workShiftService) {
		this.workShiftService = workShiftService;
	}

	public WorkShift getWorkShift() {
		return this.workShift;
	}

	public void setEmployee(final Employee employee) {
		this.employee = employee;
	}

}
