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
import java.util.Objects;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table( //
		indexes = { //
				@Index(name = "idx_dowtr_name", columnList = "name"), //
				@Index(name = "idx_dowtr_dow", columnList = "dayOfWeek"), //
				@Index(name = "idx_dowtr_rule", columnList = "schedule_rule_id") //
		}, //
		uniqueConstraints = { //
				@UniqueConstraint(name = "uk_dowtr_rule_day", columnNames = { "schedule_rule_id", "dayOfWeek" }) //
		} //
) //
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
	@Column(nullable = false, length = 128)
	private String name;

	/**
	 * Many-to-Many relationship with {@link ScheduleRule}. A time range can belong
	 * to multiple schedule rules, allowing flexibility for overlapping time ranges.
	 */
	@NotNull
	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "schedule_rule_id", nullable = false)
	private ScheduleRule scheduleRule;

	/**
	 * The day of the week (e.g., Monday, Tuesday) on which the work shift starts.
	 * The shift may extend into the next calendar day if the end time is after
	 * midnight.
	 */
	@NotNull
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private DayOfWeek dayOfWeek;

	/**
	 * The time range (start and end times) for the specified day of the week.
	 */
	@NotNull
	@Embedded
	@AttributeOverrides({
			@AttributeOverride(name = "startTime", column = @Column(name = "start_time", nullable = false)),
			@AttributeOverride(name = "endTime", column = @Column(name = "end_time", nullable = false)) })
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
	 * Returns the {@link ScheduleRule} that is associated with this time range.
	 *
	 * @return the schedule rules
	 */
	public ScheduleRule getScheduleRule() {
		return this.scheduleRule;
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
