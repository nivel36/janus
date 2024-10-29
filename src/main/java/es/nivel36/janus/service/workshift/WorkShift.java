package es.nivel36.janus.service.workshift;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import es.nivel36.janus.service.employee.Employee;
import es.nivel36.janus.service.timelog.TimeLog;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

/**
 * Represents a work shift for an employee, containing multiple time logs
 * (clock-in) and exits (clock-out), along with break periods (pauses).
 *
 * <p>
 * The {@code WorkShift} tracks when an employee starts working, takes breaks
 * (e.g., breakfast, lunch), and finishes the workday. It calculates the total
 * working time and break time within the shift.
 * </p>
 *
 * @see TimeLog
 * @see Employee
 */
@Entity
@Table(indexes = { //
		@Index(name = "idx_workshift_employee", columnList = "employee_id") //
})
public class WorkShift implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * The employee who is assigned to this work shift.
	 */
	@ManyToOne
	@JoinColumn(name = "employee_id", nullable = false)
	private Employee employee;

	/**
	 * The date and time when the work shift ended.
	 */
	private LocalDateTime endDateTime;

	/**
	 * The list of time logs (clock-ins and clock-outs) that define the work shift.
	 */
	@OneToMany(mappedBy = "workShift", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<TimeLog> timeLogs = new ArrayList<>();

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * The date and time when the work shift started.
	 */
	@NotNull
	@Column(nullable = false)
	private LocalDateTime startDateTime;

	/**
	 * Total time spent on breaks (pauses) during this shift.
	 */
	@Column(nullable = false)
	private Duration totalPauseTime = Duration.ZERO;

	/**
	 * Total time spent working during this shift.
	 */
	@Column(nullable = false)
	private Duration totalWorkTime = Duration.ZERO;

	/**
	 * Returns the employee associated with this work shift.
	 *
	 * @return the employee assigned to this work shift.
	 */
	public Employee getEmployee() {
		return this.employee;
	}

	/**
	 * Returns the end date and time of the work shift, or null if the shift is
	 * still in progress.
	 *
	 * @return the end date and time as a {@link LocalDateTime}, or null if not set.
	 */
	public LocalDateTime getEndDateTime() {
		return this.endDateTime;
	}

	/**
	 * Returns the list of time logs (clock-ins and clock-outs) for this work shift.
	 *
	 * @return the list of time logs.
	 */
	public List<TimeLog> getTimeLogs() {
		return this.timeLogs;
	}

	/**
	 * Returns the unique identifier of the work shift.
	 *
	 * @return the ID of the work shift.
	 */
	public Long getId() {
		return this.id;
	}

	/**
	 * Returns the start date and time of the work shift.
	 *
	 * @return the start date and time as a {@link LocalDateTime}.
	 */
	public LocalDateTime getStartDateTime() {
		return this.startDateTime;
	}

	/**
	 * Returns the total time spent on breaks (pauses) during this shift.
	 *
	 * @return the total pause time as a {@link Duration}.
	 */
	public Duration getTotalPauseTime() {
		return this.totalPauseTime;
	}

	/**
	 * Returns the total time spent working during this shift.
	 *
	 * @return the total work time as a {@link Duration}.
	 */
	public Duration getTotalWorkTime() {
		return this.totalWorkTime;
	}

	/**
	 * Sets the employee associated with this work shift.
	 *
	 * @param employee the employee to assign to this work shift. Cannot be null.
	 */
	public void setEmployee(final Employee employee) {
		this.employee = employee;
	}

	/**
	 * Sets the end date and time of the work shift.
	 *
	 * @param endDateTime the end date and time to set.
	 */
	public void setEndDateTime(final LocalDateTime endDateTime) {
		this.endDateTime = endDateTime;
	}

	/**
	 * Sets the list of work time logs (clock-ins and clock-outs) for this work
	 * shift.
	 *
	 * @param entries the list of time logs to set. Cannot be null.
	 */
	public void setTimeLogs(final List<TimeLog> timeLogs) {
		this.timeLogs = timeLogs;
	}

	/**
	 * Sets the unique identifier of the work shift.
	 *
	 * @param id the ID to set for the work shift.
	 */
	public void setId(final Long id) {
		this.id = id;
	}

	/**
	 * Sets the start date and time of the work shift.
	 *
	 * @param startDateTime the start date and time to set. Cannot be null.
	 */
	public void setStartDateTime(final LocalDateTime startDateTime) {
		this.startDateTime = startDateTime;
	}

	/**
	 * Sets the total time spent on breaks (pauses) during this shift.
	 *
	 * @param totalPauseTime the total pause time to set. Cannot be null.
	 */
	public void setTotalPauseTime(final Duration totalPauseTime) {
		this.totalPauseTime = totalPauseTime;
	}

	/**
	 * Sets the total time spent working during this shift.
	 *
	 * @param totalWorkTime the total work time to set. Cannot be null.
	 */
	public void setTotalWorkTime(final Duration totalWorkTime) {
		this.totalWorkTime = totalWorkTime;
	}

	public void addTimeLog(final TimeLog timeLog) {
		this.timeLogs.add(timeLog);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || this.getClass() != obj.getClass()) {
			return false;
		}
		final WorkShift other = (WorkShift) obj;
		return Objects.equals(this.id, other.id) && Objects.equals(this.employee, other.employee)
				&& Objects.equals(this.startDateTime, other.startDateTime);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.id, this.employee, this.startDateTime);
	}

	@Override
	public String toString() {
		return "WorkShift [id=" + this.id + ", employee=" + this.employee.getName() + ", startDateTime="
				+ this.startDateTime + "]";
	}
}
