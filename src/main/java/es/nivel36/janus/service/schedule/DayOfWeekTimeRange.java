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
package es.nivel36.janus.service.schedule;

import java.io.Serializable;
import java.time.DayOfWeek;
import java.time.Duration;
import java.util.Objects;

import jakarta.persistence.Embedded;
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
 * Represents a time range for a specific day of the week within a schedule
 * rule, where the work shift can start on the specified day and extend into the
 * next day if necessary.
 *
 * <p>
 * A {@code DayOfWeekTimeRange} defines the working hours or other time
 * configurations for a specific {@link DayOfWeek} within a
 * {@link ScheduleRule}. Each instance contains a {@code TimeRange} that
 * specifies the start and end times for the given day. It's important to note
 * that the shift may start late in the day (e.g., 8:00 PM) and end on the
 * following day.
 * </p>
 *
 * <p>
 * Within a single {@link ScheduleRule}, only one {@code DayOfWeekTimeRange} is
 * allowed for each {@link DayOfWeek}. This ensures that for any given day of
 * the week there is at most one shift starting on that day, avoiding
 * ambiguities when retrieving the applicable time range for a specific date.
 * </p>
 *
 * <p>
 * This class is part of a schedule rule, allowing flexible time definitions for
 * different days of the week (e.g., different work hours on Mondays versus
 * Fridays), including shifts that span multiple calendar days.
 * </p>
 */
@Entity
public class DayOfWeekTimeRange implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * Unique identifier for the day-of-week time range. Auto-generated.
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * The day of the week (e.g., Monday, Tuesday) on which the work shift starts.
	 * The shift may extend into the next calendar day if the end time is after
	 * midnight.
	 */
	@NotNull
	@Enumerated(EnumType.STRING)
	private DayOfWeek dayOfWeek;

	/**
	 * Specifies the actual number of working hours within the allowed time range.
	 *
	 * <p>
	 * For example, if the shift allows clock-in between 08:00–10:00 and clock-out
	 * between 17:00–19:00, the employee may still be required to work only 8
	 * effective hours even though the full range spans 11 hours. This field
	 * represents the intended working duration, not the total span between the
	 * earliest start and latest end times.
	 * </p>
	 */
	@NotNull
	private Duration effectiveWorkHours;

	/**
	 * The time range (start and end times) for the specified day of the week.
	 */
	@NotNull
	@Embedded
	private TimeRange timeRange;

	/**
	 * Many-to-Many relationship with {@link ScheduleRule}. A time range can belong
	 * to multiple schedule rules, allowing flexibility for overlapping time ranges.
	 */
	@NotNull
	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "schedule_rule_id")
	private ScheduleRule scheduleRule;

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
	 *
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
	 * @return the schedule rules
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
	 * Sets the day of the week for which the work shift starts. The shift may
	 * extend into the next calendar day if the end time is after midnight.
	 *
	 * @param dayOfWeek the new day of the week on which the work shift starts.
	 */
	public void setDayOfWeek(final DayOfWeek dayOfWeek) {
		this.dayOfWeek = dayOfWeek;
	}

	/**
	 * Sets the effective number of working hours within the allowed time range.
	 *
	 * <p>
	 * For example, if the allowed time window is 08:00–19:00 but the employee
	 * should work only 8 hours, this method should receive {@code 8.00}.
	 *
	 * @param effectiveWorkHours the effective working hours to be set; must not be
	 *                           {@code null}
	 */
	public void setEffectiveWorkHours(final Duration effectiveWorkHours) {
		this.effectiveWorkHours = effectiveWorkHours;
	}

	/**
	 * Sets the unique identifier of the day-of-week time range.
	 *
	 * @param id the new identifier
	 */
	public void setId(final Long id) {
		this.id = id;
	}

	/**
	 * Sets the {@link ScheduleRule} that is associated with this time range.
	 *
	 * @param scheduleRule to associate with this time range
	 */
	public void setScheduleRule(final ScheduleRule scheduleRule) {
		this.scheduleRule = scheduleRule;
	}

	/**
	 * Sets the time range (start and end times) for the specified day of the week.
	 *
	 * @param timeRange the new time range
	 */
	public void setTimeRange(final TimeRange timeRange) {
		this.timeRange = timeRange;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if ((obj == null) || (this.getClass() != obj.getClass())) {
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
