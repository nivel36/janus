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
import java.time.Instant;
import java.util.Objects;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import es.nivel36.janus.service.employee.Employee;
import es.nivel36.janus.service.worksite.Worksite;
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
 * <p>
 * A time log records the clock-in ({@code entryTime}) and optional clock-out
 * ({@code exitTime}) instants of an employee at a specific {@link Worksite}.
 * </p>
 *
 * <p>
 * Each time log is uniquely identified by the combination of the employee and
 * the entry time, enforced through a unique constraint. The entity also
 * supports logical deletion via the {@code deleted} flag, which ensures that
 * records remain in the database for auditing purposes even after they are
 * "deleted".
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
	 * Unique identifier of the time log. Auto-generated.
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * The employee associated with this time log
	 */
	@NotNull
	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "employee_id")
	private Employee employee;

	/**
	 * The worksite associated with this time log
	 */
	@NotNull
	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "worksite_id")
	private Worksite worksite;

	/**
	 * The clock-in time for this time log.
	 */
	@NotNull
	private Instant entryTime;

	/**
	 * The clock-out time for this time log.
	 */
	private Instant exitTime;

	/**
	 * Indicates whether this time log has been logically deleted.
	 * <p>
	 * When {@code true}, the record is considered deleted but is still present in
	 * the database for auditing.
	 */
	@NotNull
	private boolean deleted = false;

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
	 * @param employee associated with this time log, must not be null
	 * @param worksite associated with this time log, must not be null
	 * @throws NullPointerException if the employee or the work site are null
	 */
	public TimeLog(final Employee employee, final Worksite worksite) {
		this.employee = Objects.requireNonNull(employee, "Employee can't be null");
		this.worksite = Objects.requireNonNull(worksite, "Worksite can't be null");
	}

	/**
	 * Constructor for the {@link TimeLog} entity that initializes the time log with
	 * the given employee and entry time.
	 *
	 * @param employee  associated with this time log, must not be null
	 * @param worksite  associated with this time log, must not be null
	 * @param entryTime the entry (clock-in) time for this time log, must not be
	 *                  null
	 * @throws NullPointerException if the employee or entryTime is null
	 */
	public TimeLog(final Employee employee, final Worksite worksite, final Instant entryTime) {
		this.employee = Objects.requireNonNull(employee, "Employee can't be null");
		this.worksite = Objects.requireNonNull(worksite, "Worksite can't be null");
		this.entryTime = Objects.requireNonNull(entryTime, "EntryTime can't be null");
	}

	/**
	 * Constructor for the {@link TimeLog} entity that initializes the time log with
	 * the given employee and entry time.
	 *
	 * @param employee  associated with this time log, must not be null
	 * @param worksite  associated with this time log, must not be null
	 * @param entryTime the entry (clock-in) time for this time log, must not be
	 *                  null
	 * @param exitTime  the exit (clock-out) time for this time log, must not be
	 *                  null
	 * @throws NullPointerException if the employee or entryTime is null
	 */
	public TimeLog(final Employee employee, final Worksite worksite, final Instant entryTime, final Instant exitTime) {
		this.employee = Objects.requireNonNull(employee, "Employee can't be null");
		this.worksite = Objects.requireNonNull(worksite, "Worksite can't be null");
		this.entryTime = Objects.requireNonNull(entryTime, "EntryTime can't be null");
		this.exitTime = Objects.requireNonNull(exitTime, "ExitTime can't be null");
	}

	/**
	 * Gets the unique identifier of the time log.
	 *
	 * @return the id of the time log
	 */
	public Long getId() {
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
	 * @return the clock-in time as a {@link Instant}, or null if the employee has
	 *         not clocked in
	 */
	public Instant getEntryTime() {
		return this.entryTime;
	}

	/**
	 * Gets the clock-out time for this time log.
	 *
	 * @return the clock-out time as a {@link Instant}, or null if the employee has
	 *         not clocked out
	 */
	public Instant getExitTime() {
		return this.exitTime;
	}

	/**
	 * Gets the logical deletion flag for this time log.
	 * 
	 * @return {@code true} to mark the entity as logically deleted, {@code false}
	 *         otherwise
	 */
	public boolean isDeleted() {
		return deleted;
	}

	/**
	 * Sets the unique identifier for this time log.
	 *
	 * @param id the ID to set for this time log
	 */
	public void setId(final Long id) {
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
	 * @param entryTime the clock-in time to set as a {@link Instant}
	 */
	public void setEntryTime(final Instant entryTime) {
		this.entryTime = entryTime;
	}

	/**
	 * Sets the clock-out time for this time log.
	 *
	 * @param exitTime the clock-out time to set as a {@link Instant}, or null if
	 *                 the employee has not clocked out yet
	 */
	public void setExitTime(final Instant exitTime) {
		this.exitTime = exitTime;
	}

	/**
	 * Sets the logical deletion flag for this time log.
	 *
	 * @param deleted {@code true} to mark the entity as logically deleted,
	 *                {@code false} otherwise
	 */
	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}

	/**
	 * Gets the worksite associated with this time log.
	 *
	 * @return the {@link Worksite} where the employee performed the logged
	 *         activity; never {@code null} once the entity is persisted
	 */
	public Worksite getWorksite() {
		return worksite;
	}

	/**
	 * Sets the worksite associated with this time log.
	 *
	 * @param worksite the {@link Worksite} to associate with this time log; must
	 *                 not be {@code null}
	 */
	public void setWorksite(Worksite worksite) {
		this.worksite = worksite;
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
		return Objects.equals(this.employee, other.employee) && //
				Objects.equals(this.entryTime, other.entryTime);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.employee, this.entryTime);
	}

	@Override
	public String toString() {
		return "TimeLog [employee=" + this.employee //
				+ ", entryTime=" + this.entryTime //
				+ ", exitTime=" + this.exitTime //
				+ ", deleted=" + this.deleted //
				+ "]";
	}
}
