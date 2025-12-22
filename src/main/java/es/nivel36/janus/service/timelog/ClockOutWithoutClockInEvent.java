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
 * JPA entity that represents a domain event triggered when an employee clocks
 * out without having previously clocked in.
 *
 * <p>
 * This event is created when the system detects an exit action that cannot be
 * matched to an existing clock-in record. Once created, the event can be
 * {@link #resolve(TimeLog) resolved} by associating it with a {@link TimeLog},
 * or {@link #invalidate() invalidated} if it is deemed incorrect or not
 * applicable.
 * </p>
 *
 * <p>
 * An event is considered <em>finalized</em> once it has been resolved or
 * invalidated. After finalization, no further state changes are allowed.
 * </p>
 */
@Entity
public class ClockOutWithoutClockInEvent implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * Primary key that uniquely identifies this event.
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * The employee who performed the clock-out action without a prior clock-in.
	 * Can't be {@code null}. This value is immutable after creation.
	 */
	@NotNull
	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "employee_id", updatable = false)
	private Employee employee;

	/**
	 * The worksite where the clock-out action was registered. Can't be
	 * {@code null}. This value is immutable after creation.
	 */
	@NotNull
	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "worksite_id", updatable = false)
	private Worksite worksite;

	/**
	 * The instant when the employee clocked out. This value is immutable after
	 * creation.
	 */
	@NotNull
	@Column(updatable = false)
	private Instant exitTime;

	/**
	 * The instant when this anomaly was detected by the system. This value is
	 * immutable after creation.
	 */
	@NotNull
	@Column(updatable = false)
	private Instant detectedAt;

	/**
	 * Indicates whether this event has been resolved by linking it to a valid
	 * {@link TimeLog}.
	 */
	private boolean resolved;

	/**
	 * Indicates whether this event has been invalidated and should be ignored for
	 * further processing.
	 */
	private boolean invalidated;

	/**
	 * Optional reason explaining why the event was resolved or invalidated.
	 */
	private String reason;

	/**
	 * The {@link TimeLog} that resolves this event, if it has been resolved.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "resolved_timelog_id")
	private TimeLog resolvedTimeLog;

	/**
	 * Default constructor required by JPA.
	 */
	ClockOutWithoutClockInEvent() {
	}

	/**
	 * Creates a new {@code ClockOutWithoutClockInEvent}.
	 *
	 * <p>
	 * This constructor initializes an event representing a clock-out action
	 * performed by an {@link Employee} at a given {@link Worksite} without a
	 * corresponding clock-in. The event records both the instant when the clock-out
	 * occurred and the instant when the anomaly was detected.
	 * </p>
	 *
	 * <p>
	 * The {@code exitTime} must not be after {@code detectedAt}. If this condition
	 * is violated, an {@link IllegalArgumentException} is thrown.
	 * </p>
	 *
	 * @param employee   the employee who performed the clock-out. Can't be
	 *                   {@code null}.
	 * @param worksite   the worksite where the clock-out occurred. Can't be
	 *                   {@code null}.
	 * @param exitTime   the instant when the clock-out occurred. Can't be
	 *                   {@code null}.
	 * @param detectedAt the instant when the event was detected by the system.
	 *                   Can't be {@code null}.
	 * @throws IllegalArgumentException if {@code exitTime} is after
	 *                                  {@code detectedAt}.
	 */
	public ClockOutWithoutClockInEvent(final Employee employee, final Worksite worksite, final Instant exitTime,
			final Instant detectedAt) {
		this.employee = Objects.requireNonNull(employee);
		this.worksite = Objects.requireNonNull(worksite);
		this.exitTime = Objects.requireNonNull(exitTime);
		this.detectedAt = Objects.requireNonNull(detectedAt);
		if (exitTime.isAfter(detectedAt)) {
			throw new IllegalArgumentException("exitTime cannot be after detectedAt");
		}
	}

	/**
	 * Returns the instant when this event was detected by the system.
	 *
	 * @return the detection instant, never {@code null}.
	 */
	public Instant getDetectedAt() {
		return detectedAt;
	}

	/**
	 * Returns the employee associated with this event.
	 *
	 * @return the employee, never {@code null}.
	 */
	public Employee getEmployee() {
		return employee;
	}

	/**
	 * Returns the instant when the employee clocked out.
	 *
	 * @return the exit time, never {@code null}.
	 */
	public Instant getExitTime() {
		return exitTime;
	}

	/**
	 * Returns the unique identifier of this event.
	 *
	 * @return the event identifier, or {@code null} if the entity has not yet been
	 *         persisted.
	 */
	public Long getId() {
		return id;
	}

	/**
	 * Returns the reason associated with the resolution or invalidation of this
	 * event.
	 *
	 * @return the reason, or {@code null} if none was provided.
	 */
	public String getReason() {
		return reason;
	}

	/**
	 * Returns the {@link TimeLog} that resolves this event.
	 *
	 * @return the resolved time log, or {@code null} if the event has not been
	 *         resolved.
	 */
	public TimeLog getResolvedTimeLog() {
		return resolvedTimeLog;
	}

	/**
	 * Returns the worksite where the clock-out occurred.
	 *
	 * @return the worksite, never {@code null}.
	 */
	public Worksite getWorksite() {
		return worksite;
	}
	
	/**
	 * Sets the identifier of this event.
	 *
	 * @param id the identifier to assign
	 */
	void setId(final Long id) {
		this.id = id;
	}

	/**
	 * Indicates whether this event is finalized.
	 *
	 * <p>
	 * An event is finalized if it has been either resolved or invalidated.
	 * </p>
	 *
	 * @return {@code true} if the event is finalized; {@code false} otherwise.
	 */
	public boolean isFinalized() {
		return invalidated || resolved;
	}

	/**
	 * Indicates whether this event has been invalidated.
	 *
	 * @return {@code true} if the event is invalidated; {@code false} otherwise.
	 */
	public boolean isInvalidated() {
		return invalidated;
	}

	/**
	 * Indicates whether this event has been resolved.
	 *
	 * @return {@code true} if the event is resolved; {@code false} otherwise.
	 */
	public boolean isResolved() {
		return resolved;
	}

	/**
	 * Invalidates this event.
	 *
	 * <p>
	 * Once invalidated, the event becomes finalized and cannot be resolved or
	 * invalidated again.
	 * </p>
	 *
	 * @throws EventAlreadyFinalizedException if the event is already finalized.
	 */
	public void invalidate() {
		this.assertNotFinalized();
		this.invalidated = true;
	}

	/**
	 * Invalidates this event and associates a reason with the invalidation.
	 *
	 * @param reason explanation for the invalidation. Can't be {@code null}.
	 * @throws EventAlreadyFinalizedException if the event is already finalized.
	 */
	public void invalidate(final String reason) {
		this.assertNotFinalized();
		this.reason = Objects.requireNonNull(reason, "reason can't be null");
		this.invalidated = true;
	}

	/**
	 * Resolves this event by associating it with an existing {@link TimeLog}.
	 *
	 * <p>
	 * Once resolved, the event becomes finalized and cannot be invalidated.
	 * </p>
	 *
	 * @param resolvedTimeLog the time log that resolves this event. Can't be
	 *                        {@code null}.
	 * @throws EventAlreadyFinalizedException if the event is already finalized.
	 */
	public void resolve(final TimeLog resolvedTimeLog) {
		this.assertNotFinalized();
		this.resolvedTimeLog = Objects.requireNonNull(resolvedTimeLog);
		this.resolved = true;
	}

	/**
	 * Resolves this event by associating it with an existing {@link TimeLog} and a
	 * resolution reason.
	 *
	 * @param resolvedTimeLog the time log that resolves this event. Can't be
	 *                        {@code null}.
	 * @param reason          explanation for the resolution. Can't be {@code null}.
	 * @throws EventAlreadyFinalizedException if the event is already finalized.
	 */
	public void resolve(final TimeLog resolvedTimeLog, final String reason) {
		this.assertNotFinalized();
		this.resolvedTimeLog = Objects.requireNonNull(resolvedTimeLog);
		this.reason = Objects.requireNonNull(reason);
		this.resolved = true;
	}

	private void assertNotFinalized() {
		if (isFinalized()) {
			throw new EventAlreadyFinalizedException();
		}
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if ((obj == null) || (getClass() != obj.getClass())) {
			return false;
		}
		final ClockOutWithoutClockInEvent other = (ClockOutWithoutClockInEvent) obj;
		return Objects.equals(detectedAt, other.detectedAt) && Objects.equals(employee, other.employee)
				&& Objects.equals(exitTime, other.exitTime) && Objects.equals(worksite, other.worksite);
	}

	@Override
	public int hashCode() {
		return Objects.hash(detectedAt, employee, exitTime, worksite);
	}

	@Override
	public String toString() {
		return "ClockOutWithoutClockInEvent [employee=" + employee + ", worksite=" + worksite + ", exitTime=" + exitTime
				+ ", detectedAt=" + detectedAt + ", resolved=" + resolved + "]";
	}
}
