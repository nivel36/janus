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

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import es.nivel36.janus.service.employee.Employee;
import es.nivel36.janus.service.timelog.TimeLog;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
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
public class WorkShift implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * Unique identifier of the work shift.
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * The employee who is assigned to this work shift.
	 */
	@NotNull
	@ManyToOne(optional = false)
	@JoinColumn(name = "employee_id", updatable = false)
	private Employee employee;

	/**
	 * The date when the work shift started
	 */
	@NotNull
	@Column(updatable = false)
	private LocalDate date;

	/**
	 * The list of time logs (clock-ins and clock-outs) that define the work shift.
	 */
	@OneToMany
	@JoinTable(name = "workshift_timelog", //
			joinColumns = @JoinColumn(name = "workshift_id"), //
			inverseJoinColumns = @JoinColumn(name = "timelog_id"))
	private List<TimeLog> timeLogs = new ArrayList<>();

	/**
	 * Total time spent on breaks (pauses) during this shift.
	 */
	@NotNull
	private Duration totalPauseTime = Duration.ZERO;

	/**
	 * Total time spent working during this shift.
	 */
	@NotNull
	private Duration totalWorkTime = Duration.ZERO;

	/**
	 * Protected no-argument constructor required by persistence frameworks.
	 *
	 * <p>
	 * This constructor should not be used directly in application code. It exists
	 * solely to allow frameworks such as JPA to instantiate the entity.
	 */
	WorkShift() {
	}

	/**
	 * Creates a new {@code WorkShift} for the specified employee and date,
	 * initialized with the given list of time logs.
	 *
	 * @param employee the employee assigned to this work shift. Can't be
	 *                 {@code null}.
	 * @param date     the date when the work shift started. Can't be {@code null}.
	 * @param timeLogs the list of time logs (clock-ins and clock-outs) associated
	 *                 with this work shift. Can't be {@code null}.
	 * @throws NullPointerException if any of the parameters is {@code null}
	 */
	public WorkShift(final Employee employee, final LocalDate date, final List<TimeLog> timeLogs) {
		this.employee = Objects.requireNonNull(employee, "employee can't be null");
		this.date = Objects.requireNonNull(date, "date can't be null");
		this.timeLogs = Objects.requireNonNull(timeLogs, "timeLogs can't be null");
	}

	/**
	 * Returns the employee associated with this work shift.
	 *
	 * @return the employee assigned to this work shift.
	 */
	public Employee getEmployee() {
		return this.employee;
	}

	/**
	 * Returns the date when the work shift started, or null if the shift is still
	 * in progress.
	 *
	 * @return the date as a {@link LocalDate}, or null if not set.
	 */
	public LocalDate getDate() {
		return this.date;
	}

	/**
	 * Returns the list of time logs (clock-ins and clock-outs) for this work shift.
	 *
	 * @return the list of time logs.
	 */
	public List<TimeLog> getTimeLogs() {
		return Collections.unmodifiableList(timeLogs);
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
	 * Sets the unique identifier of the work shift.
	 *
	 * @param id the ID to set for the work shift.
	 */
	void setId(final Long id) {
		this.id = id;
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

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || this.getClass() != obj.getClass()) {
			return false;
		}
		final WorkShift other = (WorkShift) obj;
		return Objects.equals(this.employee, other.employee) && Objects.equals(this.date, other.date);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.employee, this.date);
	}

	@Override
	public String toString() {
		return "WorkShift [id=" + id + (employee != null ? ", employee=" + employee.getName() : "") + ", date="
				+ (date != null ? date : "null") + "]";
	}
}
