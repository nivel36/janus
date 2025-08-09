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
import java.util.List;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
 * This class is part of a schedule rule, allowing flexible time definitions for
 * different days of the week (e.g., different work hours on Mondays versus
 * Fridays), including shifts that span multiple calendar days.
 * </p>
 */
@Table(name = "DAY_OF_WEEK_TIME_RANGE", //
		indexes = { //
				@Index(name = "idx_day_of_week_time_range_name", columnList = "name") //
		}, uniqueConstraints = { //
				@UniqueConstraint(name = "uk_day_of_week_time_range_name", columnNames = { "name" }) //
		})
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
	 * Unique name of the time range for the day of the week. Cannot be null and
	 * must be unique across all day-of-week time ranges.
	 */
	@NotNull
	@Column(nullable = false, unique = true)
	private String name;

	/**
	 * Many-to-Many relationship with {@link ScheduleRule}. A time range can belong
	 * to multiple schedule rules, allowing flexibility for overlapping time ranges.
	 */
	@ManyToMany(mappedBy = "dayOfWeekRanges")
	private List<ScheduleRule> scheduleRules;

	/**
	 * The day of the week (e.g., Monday, Tuesday) on which the work shift starts.
	 * The shift may extend into the next calendar day if the end time is after
	 * midnight.
	 */
	@Enumerated(EnumType.STRING)
	@Column(name = "DAY_OF_WEEK")
	private DayOfWeek dayOfWeek;

	/**
	 * The time range (start and end times) for the specified day of the week.
	 */
	@Embedded
	private TimeRange timeRange;

	/**
	 * Returns the unique identifier of the day-of-week time range.
	 *
	 * @return the unique identifier
	 */
	public Long getId() {
		return this.id;
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
	 * Returns the unique name of the day-of-week time range.
	 *
	 * @return the name of the time range
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Sets the unique name of the day-of-week time range. The name cannot be null
	 * and must be unique.
	 *
	 * @param name the new name
	 */
	public void setName(final String name) {
		this.name = name;
	}

	/**
	 * Returns the list of {@link ScheduleRule} objects that are associated with
	 * this time range.
	 *
	 * @return the list of schedule rules
	 */
	public List<ScheduleRule> getScheduleRules() {
		return this.scheduleRules;
	}

	/**
	 * Sets the list of {@link ScheduleRule} objects that are associated with this
	 * time range.
	 *
	 * @param scheduleRules the list of schedule rules to associate with this time
	 *                      range
	 */
	public void setScheduleRules(final List<ScheduleRule> scheduleRules) {
		this.scheduleRules = scheduleRules;
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
	 * Sets the day of the week for which the work shift starts. The shift may
	 * extend into the next calendar day if the end time is after midnight.
	 *
	 * @param dayOfWeek the new day of the week on which the work shift starts.
	 */
	public void setDayOfWeek(final DayOfWeek dayOfWeek) {
		this.dayOfWeek = dayOfWeek;
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
	 * Sets the time range (start and end times) for the specified day of the week.
	 *
	 * @param timeRange the new time range
	 */
	public void setTimeRange(final TimeRange timeRange) {
		this.timeRange = timeRange;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.name);
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
		return Objects.equals(this.name, other.name);
	}

	@Override
	public String toString() {
		return this.name;
	}
}
