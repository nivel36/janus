package es.nivel36.janus.service.timelog;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

import es.nivel36.janus.service.employee.Employee;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(indexes = { //
		@Index(name = "idx_employee_entry_time", columnList = "employee_id, entryTime") //
}, uniqueConstraints = { //
		@UniqueConstraint(name = "uk_employee_entry_time", columnNames = { "employee_id", "entryTime" }) //
})
public class TimeLog implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * Unique identifier of the time log.
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	/**
	 * The employee associated with this time log.
	 */
	@NotNull
	@ManyToOne
	@JoinColumn(name = "employee_id", nullable = false)
	private Employee employee;

	/**
	 * The clock-in time for this time log.
	 */
	@NotNull
	@Column(nullable = false)
	private LocalDateTime entryTime;

	/**
	 * The clock-out time for this time log.
	 */
	private LocalDateTime exitTime;

	/**
	 * Default constructor for the {@link TimeLog} entity. Initializes an empty
	 * {@link TimeLog} instance.
	 */
	public TimeLog() {
	}

	/**
	 * Constructor for the {@link TimeLog} entity that initializes the time log with
	 * the given employee.
	 *
	 * @param employee the employee associated with this time log, must not be null
	 * @throws NullPointerException if the employee or entryTime is null
	 */
	public TimeLog(final Employee employee) {
		this.employee = Objects.requireNonNull(employee);
	}

	/**
	 * Constructor for the {@link TimeLog} entity that initializes the time log with
	 * the given employee and entry time.
	 *
	 * @param employee  the employee associated with this time log, must not be null
	 * @param entryTime the entry (clock-in) time for this time log, must not be
	 *                  null
	 * @throws NullPointerException if the employee or entryTime is null
	 */
	public TimeLog(final Employee employee, final LocalDateTime entryTime) {
		this.employee = Objects.requireNonNull(employee);
		this.entryTime = Objects.requireNonNull(entryTime);
	}

	/**
	 * Constructor for the {@link TimeLog} entity that initializes the time log with
	 * the given employee and entry time.
	 *
	 * @param employee  the employee associated with this time log, must not be null
	 * @param entryTime the entry (clock-in) time for this time log, must not be
	 *                  null
	 * @param exitTime  the exit (clock-out) time for this time log, must not be
	 *                  null
	 * @throws NullPointerException if the employee or entryTime is null
	 */
	public TimeLog(final Employee employee, final LocalDateTime entryTime, final LocalDateTime exitTime) {
		this.employee = Objects.requireNonNull(employee);
		this.entryTime = Objects.requireNonNull(entryTime);
		this.exitTime = Objects.requireNonNull(exitTime);
	}

	/**
	 * Gets the unique identifier of the time log.
	 *
	 * @return the id of the time log
	 */
	public long getId() {
		return this.id;
	}

	/**
	 * Gets the employee associated with this time log.
	 *
	 * @return the employee for this time log
	 */
	public Employee getEmployee() {
		return this.employee;
	}

	/**
	 * Gets the clock-in time for this time log.
	 *
	 * @return the clock-in time as a {@link LocalDateTime}, or null if the employee
	 *         has not clocked in
	 */
	public LocalDateTime getEntryTime() {
		return this.entryTime;
	}

	/**
	 * Gets the clock-out time for this time log.
	 *
	 * @return the clock-out time as a {@link LocalDateTime}, or null if the
	 *         employee has not clocked out
	 */
	public LocalDateTime getExitTime() {
		return this.exitTime;
	}

	/**
	 * Sets the unique identifier for this time log.
	 *
	 * @param id the ID to set for this time log
	 */
	public void setId(final long id) {
		this.id = id;
	}

	/**
	 * Sets the employee for this time log.
	 *
	 * @param employee the employee to associate with this time log
	 */
	public void setEmployee(final Employee employee) {
		this.employee = employee;
	}

	/**
	 * Sets the clock-in time for this time log.
	 *
	 * @param entryTime the clock-in time to set as a {@link LocalDateTime}
	 */
	public void setEntryTime(final LocalDateTime entryTime) {
		this.entryTime = entryTime;
	}

	/**
	 * Sets the clock-out time for this time log.
	 *
	 * @param exitTime the clock-out time to set as a {@link LocalDateTime}, or null
	 *                 if the employee has not clocked out yet
	 */
	public void setExitTime(final LocalDateTime exitTime) {
		this.exitTime = exitTime;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || this.getClass() != obj.getClass()) {
			return false;
		}
		final TimeLog other = (TimeLog) obj;
		return Objects.equals(this.employee, other.employee) && Objects.equals(this.entryTime, other.entryTime);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.employee, this.entryTime);
	}

	@Override
	public String toString() {
		return "TimeLog [employee=" + this.employee + ", entryTime=" + this.entryTime + ", exitTime=" + this.exitTime
				+ "]";
	}
}
