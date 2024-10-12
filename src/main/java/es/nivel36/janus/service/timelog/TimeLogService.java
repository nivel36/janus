package es.nivel36.janus.service.timelog;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.nivel36.janus.service.employee.Employee;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

/**
 * Service class responsible for managing clock-in and clock-out operations for
 * employees and interacting with the {@link TimeLogRepository} to handle
 * persistence.
 */
@Stateless
public class TimeLogService {

	private static final Logger logger = LoggerFactory.getLogger(TimeLogService.class);

	private @Inject TimeLogRepository timeLogRepository;

	/**
	 * Clocks in an employee at the current time.
	 *
	 * @param employee the employee to clock in
	 * @return the created {@link TimeLog} entry with the current entry time
	 * @throws NullPointerException if the employee is {@code null}
	 */
	public TimeLog clockIn(final Employee employee) {
		return clockIn(employee, LocalDateTime.now());
	}

	/**
	 * Clocks in an employee at the specified entry time.
	 *
	 * @param employee  the employee to clock in
	 * @param entryTime the specific time for clocking in
	 * @return the created {@link TimeLog} entry with the provided entry time
	 * @throws NullPointerException if the employee or entryTime is {@code null}
	 */
	public TimeLog clockIn(final Employee employee, final LocalDateTime entryTime) {
		Objects.requireNonNull(employee, "Employee cannot be null.");
		Objects.requireNonNull(entryTime, "Entry time cannot be null.");
		logger.debug("Clocking in employee: {} at {}", employee, entryTime);
		final TimeLog timeLog = new TimeLog(employee, entryTime);
		this.timeLogRepository.createTimeLog(timeLog);
		return timeLog;
	}

	/**
	 * Clocks out an employee at the current time.
	 *
	 * @param employee the employee to clock out
	 * @return the updated {@link TimeLog} with the exit time set to now
	 * @throws NullPointerException if the employee is {@code null}
	 */
	public TimeLog clockOut(final Employee employee) {
		return clockOut(employee, LocalDateTime.now());
	}

	/**
	 * Clocks out an employee at the specified exit time.
	 *
	 * @param employee the employee to clock out
	 * @param exitTime the specific time for clocking out
	 * @return the updated {@link TimeLog} with the provided exit time
	 * @throws NullPointerException  if the employee or exitTime is {@code null}
	 * @throws IllegalStateException if there is no {@link TimeLog} for the employee
	 */
	public TimeLog clockOut(final Employee employee, final LocalDateTime exitTime) {
		Objects.requireNonNull(employee, "Employee cannot be null.");
		Objects.requireNonNull(exitTime, "Exit time cannot be null.");
		logger.debug("Clocking out employee: {} at {}", employee, exitTime);

		final Optional<TimeLog> lastTimeLogOpt = this.timeLogRepository.findLastTimeLogByEmployee(employee);

		// Check if TimeLog is present, otherwise throw an IllegalStateException
		final TimeLog lastTimeLog = lastTimeLogOpt
				.orElseThrow(() -> new IllegalStateException("No TimeLog found for the employee."));

		lastTimeLog.setExitTime(exitTime);

		return lastTimeLog;
	}

	/**
	 * Calculates the total hours worked for a given time log. If the employee
	 * hasn't clocked out yet, the duration will be calculated from the entry time
	 * to the current time.
	 *
	 * @param timeLog the {@link TimeLog} entry containing the clock-in and
	 *                clock-out times
	 * @return a {@link Duration} representing the total hours worked
	 * @throws NullPointerException if the timeLog is {@code null}
	 */
	public Duration getHoursWorked(final TimeLog timeLog) {
		Objects.requireNonNull(timeLog, "TimeLog cannot be null.");
		logger.debug("Calculating hours worked for TimeLog: {}", timeLog);

		if (timeLog.getExitTime() == null) {
			Duration duration = Duration.between(timeLog.getEntryTime(), LocalDateTime.now());
			logger.trace("TimeLog has no exit time. Calculating duration until now: {}", duration);
			return duration;
		} else {
			Duration duration = Duration.between(timeLog.getEntryTime(), timeLog.getExitTime());
			logger.trace("TimeLog has exit time. Calculating duration between entry and exit: {}", duration);
			return duration;
		}
	}

	/**
	 * Finds a time log by its primary key (id).
	 *
	 * @param id the ID of the time log to be found
	 * @return the {@link TimeLog} with the specified id, or {@code null} if not
	 *         found
	 * @throws IllegalArgumentException if the id is negative
	 */
	public TimeLog findTimeLogById(final long id) {
		if (id < 0) {
			throw new IllegalArgumentException(String.format("Id is %s, but cannot be less than 0.", id));
		}
		logger.debug("Finding TimeLog by id: {}", id);
		return this.timeLogRepository.findTimeLogById(id);
	}

	/**
	 * Finds the last time log for the specified employee.
	 *
	 * @param employee the employee whose last time log is to be found
	 * @return the last {@link TimeLog} for the employee
	 * @throws NullPointerException if the employee is {@code null}
	 */
	public Optional<TimeLog> findLastTimeLogByEmployee(final Employee employee) {
		Objects.requireNonNull(employee, "Employee cannot be null.");
		logger.debug("Finding last TimeLog for employee: {}", employee);
		return this.timeLogRepository.findLastTimeLogByEmployee(employee);
	}

	/**
	 * Finds a list of time logs for the specified employee with pagination.
	 *
	 * @param employee      the employee whose time logs are to be found
	 * @param startPosition the initial position of the search from which to start
	 *                      returning values.
	 * @param pageSize      the size of each page
	 * @return a list of {@link TimeLog} entries for the employee
	 * @throws NullPointerException     if the employee is {@code null}
	 * @throws IllegalArgumentException if the page is negative or the pageSize is
	 *                                  less than 1
	 */
	public List<TimeLog> findTimeLogsByEmployee(final Employee employee, int startPosition, int pageSize) {
		Objects.requireNonNull(employee, "Employee cannot be null.");

		if (startPosition < 0) {
			throw new IllegalArgumentException(
					String.format("Start position is %s, but cannot be less than 0.", startPosition));
		}

		if (pageSize < 1) {
			throw new IllegalArgumentException(String.format("Page size is %s, but must be greater than 0.", pageSize));
		}

		logger.debug("Finding TimeLogs for employee: {} with startPosition: {}, pageSize: {}", employee, startPosition,
				pageSize);
		return this.timeLogRepository.findTimeLogsByEmployee(employee, startPosition, pageSize);
	}

	/**
	 * Counts a list of time logs for the specified employee.
	 *
	 * @param employee the employee whose time logs are to be found
	 * @return the number of {@link TimeLog} entries for the employee
	 * @throws NullPointerException if the employee is {@code null}
	 */
	public long countTimeLogsByEmployee(final Employee employee) {
		Objects.requireNonNull(employee, "Employee cannot be null.");

		logger.debug("Counting TimeLogs for employee: {} ", employee);
		return this.timeLogRepository.countTimeLogsByEmployee(employee);
	}

	/**
	 * Updates a time log entry.
	 *
	 * @param timeLog the time log to be updated
	 * @return the updated {@link TimeLog} entry
	 * @throws NullPointerException if the time log is {@code null}
	 */
	public TimeLog updateTimeLog(final TimeLog timeLog) {
		Objects.requireNonNull(timeLog, "TimeLog cannot be null.");
		logger.debug("Updating TimeLog: {}", timeLog);
		return this.timeLogRepository.updateTimeLog(timeLog);
	}

	/**
	 * Deletes a time log entry.
	 *
	 * @param timeLog the time log to be deleted
	 * @throws NullPointerException if the time log is {@code null}
	 */
	public void deleteTimeLog(final TimeLog timeLog) {
		Objects.requireNonNull(timeLog, "TimeLog cannot be null.");
		logger.debug("Deleting TimeLog: {}", timeLog);
		this.timeLogRepository.deleteTimeLog(timeLog);
	}

	/**
	 * Sets the timeLogRepository.
	 *
	 * @param timeLogRepository the repository used to manage time log records
	 * @throws NullPointerException if the repository is {@code null}
	 */
	public void setTimeLogService(final TimeLogRepository timeLogRepository) {
		this.timeLogRepository = Objects.requireNonNull(timeLogRepository, "TimeLogRepository cannot be null");
	}
}
