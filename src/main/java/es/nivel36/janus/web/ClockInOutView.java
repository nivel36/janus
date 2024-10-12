package es.nivel36.janus.web;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import org.omnifaces.cdi.Param;
import org.omnifaces.cdi.ViewScoped;
import org.primefaces.model.LazyDataModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.nivel36.janus.service.employee.Employee;
import es.nivel36.janus.service.schedule.ScheduleService;
import es.nivel36.janus.service.schedule.TimeRange;
import es.nivel36.janus.service.timelog.TimeLog;
import es.nivel36.janus.service.timelog.TimeLogService;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named
@ViewScoped
public class ClockInOutView extends AbstractView {

	private static final long serialVersionUID = 1L;
	private static final Logger logger = LoggerFactory.getLogger(ClockInOutView.class);

	private transient @Inject TimeLogService timeLogService;
	private transient @Inject ScheduleService scheduleService;
	private @Param Employee employee;
	private TimeLog lastTimeLog;
	private boolean clockInEnabled;
	private LazyDataModel<TimeLog> timeLogs;
	private TimeRange timeRange;

	@PostConstruct
	public void init() {
		timeLogs = new LazyTimeLogDataModel(timeLogService, employee);
		final Optional<TimeLog> timeLogOpt = this.timeLogService.findLastTimeLogByEmployee(employee);
		timeRange = this.scheduleService.findTimeRangeForEmployeeByDate(employee, LocalDate.now()).orElse(null);

		if (hasExitTime(timeLogOpt)) {
			lastTimeLog = timeLogOpt.get();
			clockInEnabled = false;
		} else {
			lastTimeLog = new TimeLog(employee);
			clockInEnabled = true;
		}
	}

	private boolean hasExitTime(final Optional<TimeLog> timeLogOpt) {
		return timeLogOpt.isPresent() && timeLogOpt.get().getExitTime() != null;
	}

	public void clockIn() {
		logger.debug("ClockIn ACTION performed");
		lastTimeLog = this.timeLogService.clockIn(employee);
		clockInEnabled = false;
	}

	public void clockOut() {
		logger.debug("ClockOut ACTION performed");
		this.timeLogService.clockOut(employee);
		lastTimeLog = new TimeLog(employee);
		clockInEnabled = true;
	}

	public Duration getHoursWorked(final TimeLog timeLog) {
		Objects.requireNonNull(timeLog, "TimeLog can't be null");
		return timeLogService.getHoursWorked(timeLog);
	}

	public boolean isClockInEnabled() {
		return clockInEnabled;
	}

	public boolean isClockOutEnabled() {
		return !clockInEnabled;
	}

	public TimeLog getLastTimeLog() {
		return lastTimeLog;
	}

	public Employee getEmployee() {
		return employee;
	}

	public LazyDataModel<TimeLog> getTimeLogs() {
		return timeLogs;
	}

	public TimeRange getTimeRange() {
		return timeRange;
	}

	public void setEmployee(final Employee employee) {
		this.employee = employee;
	}

	public void setLastTimeLog(final TimeLog lastTimeLog) {
		this.lastTimeLog = lastTimeLog;
	}

	public void setTimeLogService(final TimeLogService timeLogService) {
		this.timeLogService = Objects.requireNonNull(timeLogService);
	}

	public void setScheduleService(final ScheduleService scheduleService) {
		this.scheduleService = Objects.requireNonNull(scheduleService);
	}
}
