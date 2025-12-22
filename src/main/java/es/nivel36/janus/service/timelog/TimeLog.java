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

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import es.nivel36.janus.service.employee.Employee;
import es.nivel36.janus.service.worksite.Worksite;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;

/**
 * Entity representing a time log entry for an {@link Employee}.
 *
 * <p>
 * A time log records the clock-in ({@code entryTime}) and optional clock-out
 * ({@code exitTime}) instants of an employee at a specific {@link Worksite}.
 * </p>
 *
 * <p>
 * Each time log is uniquely identified by the combination of the employee and
 * the entry time, which together form the natural key. This constraint is
 * enforced at the database level.
 * </p>
 * 
 * <p>
 * For calculation efficiency reasons, the duration of the object is calculated
 * automatically once it is closed using the {@link #close(Instant)} method.
 * </p>
 *
 * <p>
 * The entity supports logical deletion via the {@code deleted} flag, allowing
 * records to remain stored for auditing purposes even after being deleted from
 * the active dataset.
 * </p>
 *
 * @see Employee
 * @see Worksite
 */
@SQLDelete(sql = "UPDATE TIME_LOG SET deleted = true WHERE id = ?")
@SQLRestriction("deleted = false")
@Entity
public class TimeLog implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * Unique identifier of the time log.
	 * <p>
	 * This value is auto-generated and has no business meaning.
	 * </p>
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * The employee associated with this time log.
	 * <p>
	 * This association is mandatory and cannot be changed once the entity is
	 * persisted.
	 * </p>
	 */
	@NotNull
	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "employee_id", updatable = false)
	private Employee employee;

	/**
	 * The worksite where the employee performed the logged work.
	 * <p>
	 * This association is mandatory and cannot be changed once the entity is
	 * persisted.
	 * </p>
	 */
	@NotNull
	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "worksite_id", updatable = false)
	private Worksite worksite;

	/**
	 * The clock-in time for this time log.
	 * <p>
	 * This value is immutable once set and defines the start of the working period.
	 * </p>
	 */
	@NotNull
	@Column(updatable = false)
	private Instant entryTime;

	/**
	 * The clock-out time for this time log.
	 * <p>
	 * This value is {@code null} while the time log is open.
	 * </p>
	 */
	private Instant exitTime;

	/**
	 * The total duration of work between {@code entryTime} and {@code exitTime}.
	 * <p>
	 * This value is {@code null} while the time log is open and it's calculated
	 * when the method {@link #close(Instant)} is called.
	 * </p>
	 */
	private Duration workDuration;

	/**
	 * Indicates whether this time log has been logically deleted.
	 * <p>
	 * When {@code true}, the record is considered deleted but remains stored in the
	 * database for auditing purposes.
	 * </p>
	 */
	private boolean deleted = false;

	/**
	 * Default constructor required by JPA.
	 */
	protected TimeLog() {
	}

	/**
	 * Creates a new open {@link TimeLog} with the given employee, worksite and entry
	 * time.
	 *
	 * @param employee  the employee associated with this time log; can't be
	 *                  {@code null}
	 * @param worksite  the worksite associated with this time log; can't be
	 *                  {@code null}
	 * @param entryTime the clock-in time; can't be {@code null}
	 *
	 * @throws NullPointerException if any argument is {@code null}
	 */
	public TimeLog(final Employee employee, final Worksite worksite, final Instant entryTime) {
		this.employee = Objects.requireNonNull(employee, "Employee can't be null");
		this.worksite = Objects.requireNonNull(worksite, "Worksite can't be null");
		this.entryTime = Objects.requireNonNull(entryTime, "EntryTime can't be null");
	}

	/**
	 * Creates a new closed {@link TimeLog} with the given employee, worksite, entry time
	 * and exit time.
	 *
	 * @param employee  the employee associated with this time log; can't be
	 *                  {@code null}
	 * @param worksite  the worksite associated with this time log; can't be
	 *                  {@code null}
	 * @param entryTime the clock-in time; can't be {@code null}
	 * @param exitTime  the clock-out time; can't be {@code null}
	 *
	 * @throws NullPointerException if {@code employee}, {@code worksite} or
	 *                              {@code entryTime} is {@code null}
	 */
	public TimeLog(final Employee employee, final Worksite worksite, final Instant entryTime, final Instant exitTime) {
		this.employee = Objects.requireNonNull(employee, "Employee can't be null");
		this.worksite = Objects.requireNonNull(worksite, "Worksite can't be null");
		this.entryTime = Objects.requireNonNull(entryTime, "EntryTime can't be null");
		this.close(exitTime);
	}

	/**
	 * Returns the unique identifier of this time log.
	 *
	 * @return the identifier, or {@code null} if the entity has not been persisted
	 *         yet
	 */
	public Long getId() {
		return this.id;
	}

	/**
	 * Returns the employee associated with this time log.
	 *
	 * @return the associated {@link Employee}; never {@code null}
	 */
	public Employee getEmployee() {
		return this.employee;
	}

	/**
	 * Returns the worksite associated with this time log.
	 *
	 * @return the associated {@link Worksite}; never {@code null}
	 */
	public Worksite getWorksite() {
		return worksite;
	}

	/**
	 * Returns the clock-in time of this time log.
	 *
	 * @return the entry time; never {@code null}
	 */
	public Instant getEntryTime() {
		return this.entryTime;
	}

	/**
	 * Returns the clock-out time of this time log.
	 *
	 * @return the exit time, or {@code null} if the time log is still open
	 */
	public Instant getExitTime() {
		return this.exitTime;
	}

	/**
	 * Returns the total duration of work. This value is only meaningful when the
	 * time log is closed.
	 *
	 * @return the work duration, or {@code null} if the time log is still open
	 */
	public Duration getWorkDuration() {
		return this.workDuration;
	}

	/**
	 * Indicates whether this time log has been logically deleted.
	 *
	 * @return {@code true} if the time log is deleted; {@code false} otherwise
	 */
	public boolean isDeleted() {
		return deleted;
	}

	/**
	 * Sets the identifier of this time log.
	 *
	 * @param id the identifier to assign
	 */
	void setId(final Long id) {
		this.id = id;
	}

	/**
	 * Marks this time log as logically deleted or not.
	 *
	 * @param deleted {@code true} to mark as deleted; {@code false} otherwise
	 */
	void setDeleted(final boolean deleted) {
		this.deleted = deleted;
	}

	/**
	 * Closes this time log by setting the exit time.
	 * <p>
	 * Once closed, a time log cannot be reopened.
	 * </p>
	 *
	 * @param exitTime the clock-out time; can't be {@code null}
	 *
	 * @throws NullPointerException          if {@code exitTime} is {@code null}
	 * @throws TimeLogAlreadyClosedException if the time log is already closed
	 * @throws TimeLogDeletedException       if the time log is logically deleted
	 * @throws TimeLogChronologyException    if {@code exitTime} is not strictly
	 *                                       after {@code entryTime}
	 */
	public void close(final Instant exitTime) {
		Objects.requireNonNull(exitTime, "exitTime cannot be null");
		if (isClosed()) {
			throw new TimeLogAlreadyClosedException();
		}
		this.assertNotDeleted();
		this.assertChronology(exitTime);
		this.exitTime = exitTime;
		this.workDuration = Duration.between(this.entryTime, this.exitTime);
	}

	private void assertNotDeleted() {
		if (deleted) {
			throw new TimeLogDeletedException("TimeLog is deleted and cannot be modified");
		}
	}

	private void assertChronology(final Instant exitTime) {
		if (!this.entryTime.isBefore(exitTime)) {
			throw new TimeLogChronologyException(
					String.format("entryTime %s must be strictly before exitTime %s.", this.entryTime, exitTime));
		}
	}

	/**
	 * Indicates whether this time log is currently open.
	 *
	 * @return {@code true} if {@code exitTime} is {@code null}; {@code false}
	 *         otherwise
	 */
	public boolean isOpen() {
		return exitTime == null;
	}

	/**
	 * Indicates whether this time log is closed.
	 *
	 * @return {@code true} if {@code exitTime} is not {@code null}; {@code false}
	 *         otherwise
	 */
	public boolean isClosed() {
		return exitTime != null;
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
				+ ", workDuration=" + this.workDuration + ", deleted=" + this.deleted + "]";
	}
}
