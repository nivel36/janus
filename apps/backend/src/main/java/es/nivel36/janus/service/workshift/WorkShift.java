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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotNull;

/**
 * Mutable entity representing an employee's work shift on a local date.
 * <p>
 * Associated time logs retain their recorded work periods. Work and pause
 * totals are stored separately and do not automatically change when logs are
 * modified. Persistence permits at most one shift per employee and date.
 * Equality and hashing use those same values.
 */
@Entity
public class WorkShift implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotNull
	@ManyToOne(optional = false)
	@JoinColumn(name = "employee_id", updatable = false)
	private Employee employee;

	@NotNull
	@Column(updatable = false)
	private LocalDate date;

	@OneToMany(mappedBy = "workShift")
	private List<TimeLog> timeLogs = new ArrayList<>();

	@NotNull
	private Duration totalPauseTime = Duration.ZERO;

	@NotNull
	private Duration totalWorkTime = Duration.ZERO;

	/**
	 * Constructs an empty shift for persistence hydration.
	 */
	WorkShift() {
	}

	/**
	 * Creates a shift and associates each supplied log with it.
	 * <p>
	 * The supplied list is retained rather than copied. Changes to that list are
	 * visible through {@link #getTimeLogs()}, but do not automatically update log
	 * associations or duration totals. Work and pause totals initially equal zero.
	 *
	 * @param  employee              the shift owner; must not be {@code null}
	 * @param  date                  the local shift date; must not be {@code null}
	 * @param  timeLogs              the retained log list; list and elements must
	 *                               not be {@code null}
	 * @throws NullPointerException  if any argument or list element is {@code null}
	 * @throws IllegalStateException if a log is assigned to a different
	 *                               employee/date shift
	 */
	public WorkShift(final Employee employee, final LocalDate date, final List<TimeLog> timeLogs) {
		this.employee = Objects.requireNonNull(employee, "employee can't be null");
		this.date = Objects.requireNonNull(date, "date can't be null");
		this.timeLogs = Objects.requireNonNull(timeLogs, "timeLogs can't be null");
		this.attachTimeLogs(timeLogs);
	}

	/**
	 * Returns the surrogate identifier of this work shift.
	 *
	 * @return the internal identifier, or {@code null} if the entity has not yet
	 *         been persisted
	 */
	public Long getId() {
		return this.id;
	}

	/**
	 * Returns the employee associated with this work shift.
	 *
	 * @return the {@link Employee} assigned to this work shift
	 */
	public Employee getEmployee() {
		return this.employee;
	}

	/**
	 * Returns the date on which the work shift started.
	 *
	 * @return the start date of the work shift
	 */
	public LocalDate getDate() {
		return this.date;
	}

	/**
	 * Returns an unmodifiable view of the associated time logs.
	 * <p>
	 * Changes to the underlying list are reflected in this view. No ordering is
	 * imposed by this accessor.
	 *
	 * @return the unmodifiable view of the associated logs
	 */
	public List<TimeLog> getTimeLogs() {
		return Collections.unmodifiableList(this.timeLogs);
	}

	private void attachTimeLogs(final List<TimeLog> timeLogs) {
		for (final TimeLog timeLog : timeLogs) {
			Objects.requireNonNull(timeLog, "timeLogs can't contain null");
			timeLog.assignWorkShift(this);
		}
	}

	/**
	 * Returns the total pause time accumulated during this work shift.
	 *
	 * @return the total pause time; never {@code null}
	 */
	public Duration getTotalPauseTime() {
		return this.totalPauseTime;
	}

	/**
	 * Returns the total effective working time accumulated during this work shift.
	 *
	 * @return the total working time; never {@code null}
	 */
	public Duration getTotalWorkTime() {
		return this.totalWorkTime;
	}

	/**
	 * Assigns the surrogate identifier of the work shift.
	 * <p>
	 * This method exists exclusively for testing purposes and must not be used in
	 * production code. It allows controlled assignment of the identifier when
	 * working with manually constructed or detached entities in tests.
	 * </p>
	 *
	 * @param id the identifier to assign
	 */
	void setId(final Long id) {
		this.id = id;
	}

	/**
	 * Updates the total pause time of this work shift.
	 *
	 * @param  totalPauseTime       the new total pause time; must not be
	 *                              {@code null}
	 * @throws NullPointerException if {@code totalPauseTime} is {@code null}
	 */
	public void setTotalPauseTime(final Duration totalPauseTime) {
		this.totalPauseTime = Objects.requireNonNull(totalPauseTime, "totalPauseTime can't be null");
	}

	/**
	 * Updates the total effective working time of this work shift.
	 *
	 * @param  totalWorkTime        the new total working time; must not be
	 *                              {@code null}
	 * @throws NullPointerException if {@code totalWorkTime} is {@code null}
	 */
	public void setTotalWorkTime(final Duration totalWorkTime) {
		this.totalWorkTime = Objects.requireNonNull(totalWorkTime, "totalWorkTime can't be null");
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
		return "WorkShift [id=" + this.id + (this.employee != null ? ", employee=" + this.employee.getName() : "")
				+ ", date=" + (this.date != null ? this.date : "null") + "]";
	}
}
