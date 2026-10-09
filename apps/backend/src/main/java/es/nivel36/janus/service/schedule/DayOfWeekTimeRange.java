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
package es.nivel36.janus.service.schedule;

import java.io.Serializable;
import java.time.DayOfWeek;
import java.time.Duration;
import java.util.Objects;

import jakarta.persistence.Embedded;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;

/**
 * Mutable entity defining a weekday's scheduled range and intended working
 * duration.
 * <p>
 * The weekday identifies when a shift starts. An overnight {@link TimeRange}
 * ends on the following day. Effective work must not exceed the range duration;
 * this entity does not reject negative effective durations or enforce weekday
 * uniqueness within the parent rule.
 * <p>
 * Equality and hashing use the weekday and time range.
 */
@Entity
public class DayOfWeekTimeRange implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotNull
	@Enumerated(EnumType.STRING)
	@Column(columnDefinition = "text")
	private DayOfWeek dayOfWeek;

	@NotNull
	private Duration effectiveWorkHours;

	@NotNull
	@Embedded
	private TimeRange timeRange;

	@NotNull
	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "schedule_rule_id", updatable = false)
	private ScheduleRule scheduleRule;

	/**
	 * Constructs an empty instance for persistence hydration.
	 */
	DayOfWeekTimeRange() {
	}

	/**
	 * Creates a new {@code DayOfWeekTimeRange} associated with the given
	 * {@link ScheduleRule}, defining the time range and effective working hours for
	 * a specific day of the week.
	 * <p>
	 * The shift is considered to start on the specified {@link DayOfWeek} and may
	 * extend into the next calendar day depending on the provided
	 * {@link TimeRange}.
	 * </p>
	 *
	 * @param  scheduleRule             the {@link ScheduleRule} to which this time
	 *                                  range belongs. Must not be {@code null}.
	 * @param  dayOfWeek                the day of the week on which the shift
	 *                                  starts. Must not be {@code null}.
	 * @param  timeRange                the allowed start and end time range for the
	 *                                  shift. Must not be {@code null}.
	 * @param  effectiveWorkHours       the actual amount of working time required
	 *                                  within the given time range. Must not be
	 *                                  {@code null}.
	 * @throws NullPointerException     if scheduleRule, dayOfWeek, timeRange or
	 *                                  effectiveWorkHours is {@code null}
	 * @throws IllegalArgumentException if {@code effectiveWorkHours} is greater
	 *                                  than the duration of {@code timeRange}
	 */
	public DayOfWeekTimeRange(
		final ScheduleRule scheduleRule,
		final DayOfWeek dayOfWeek,
		final TimeRange timeRange,
		final Duration effectiveWorkHours) {
		this.scheduleRule = Objects.requireNonNull(scheduleRule, "scheduleRule can't be null");
		this.defineTimeRange(dayOfWeek, timeRange, effectiveWorkHours);
	}

	/**
	 * Returns the day of the week on which the work shift starts. The shift may
	 * extend into the next calendar day if the end time is after midnight.
	 *
	 * @return the day of the week on which the work shift starts.
	 */
	public DayOfWeek getDayOfWeek() {
		return this.dayOfWeek;
	}

	/**
	 * Returns the effective number of working hours within the allowed time range.
	 * <p>
	 * This value represents the actual working duration (e.g., 8 hours) that the
	 * employee must complete, regardless of the wider time window defined by
	 * {@link #getTimeRange()}.
	 * </p>
	 *
	 * @return the effective number of working hours
	 */
	public Duration getEffectiveWorkHours() {
		return this.effectiveWorkHours;
	}

	/**
	 * Returns the unique identifier of the day-of-week time range.
	 *
	 * @return the unique identifier
	 */
	public Long getId() {
		return this.id;
	}

	/**
	 * Returns the {@link ScheduleRule} that is associated with this time range.
	 *
	 * @return the associated schedule rule
	 */
	public ScheduleRule getScheduleRule() {
		return this.scheduleRule;
	}

	/**
	 * Returns the time range (start and end times) for the specified day of the
	 * week.
	 *
	 * @return the time range for the day
	 */
	public TimeRange getTimeRange() {
		return this.timeRange;
	}

	/**
	 * Sets the unique identifier of the day-of-week time range.
	 *
	 * @param id the new identifier
	 */
	void setId(final Long id) {
		this.id = id;
	}

	/**
	 * Defines or updates the time range configuration for this day of the week.
	 * <p>
	 * This method associates a {@link DayOfWeek}, a {@link TimeRange}, and an
	 * effective working duration with this instance. The effective working hours
	 * must not exceed the total duration of the provided time range.
	 * </p>
	 *
	 * @param  dayOfWeek                the day of the week on which the shift
	 *                                  starts. Must not be {@code null}.
	 * @param  timeRange                the allowed start and end time range for the
	 *                                  shift. Must not be {@code null}.
	 * @param  effectiveWorkHours       the actual amount of working time required
	 *                                  within the given time range. Must not be
	 *                                  {@code null}.
	 * @throws NullPointerException     if any argument is {@code null}
	 * @throws IllegalArgumentException if {@code effectiveWorkHours} is greater
	 *                                  than the duration of {@code timeRange}
	 */
	public void defineTimeRange(
			final DayOfWeek dayOfWeek,
			final TimeRange timeRange,
			final Duration effectiveWorkHours) {
		Objects.requireNonNull(dayOfWeek, "dayOfWeek can't be null");
		Objects.requireNonNull(timeRange, "timeRange can't be null");
		Objects.requireNonNull(effectiveWorkHours, "effectiveWorkHours can't be null");
		if (effectiveWorkHours.compareTo(timeRange.getDuration()) > 0) {
			throw new IllegalArgumentException("effectiveWorkHours cannot be greater than timeRange duration");
		}
		this.dayOfWeek = dayOfWeek;
		this.timeRange = timeRange;
		this.effectiveWorkHours = effectiveWorkHours;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || this.getClass() != obj.getClass()) {
			return false;
		}
		final DayOfWeekTimeRange other = (DayOfWeekTimeRange) obj;
		return Objects.equals(this.dayOfWeek, other.dayOfWeek) && Objects.equals(this.timeRange, other.timeRange);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.dayOfWeek, this.timeRange);
	}

	@Override
	public String toString() {
		return this.dayOfWeek + " from " + this.timeRange;
	}
}
