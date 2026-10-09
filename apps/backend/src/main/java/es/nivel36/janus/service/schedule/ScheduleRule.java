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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import es.nivel36.janus.util.Strings;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Mutable rule defining weekday ranges within a {@link Schedule}.
 * <p>
 * Optional inclusive start and end dates delimit the active period. A missing
 * bound leaves that side of the period open. Equality and hashing use the name
 * and parent schedule.
 * <p>
 * Range insertion preserves order and permits duplicates. This entity does not
 * enforce one range per weekday or prevent overlapping active periods.
 */
@Entity
public class ScheduleRule implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank
	@Column(columnDefinition = "text")
	private String name;

	@NotNull
	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "schedule_id", updatable = false)
	private Schedule schedule;

	private LocalDate startDate;

	private LocalDate endDate;

	@OneToMany(mappedBy = "scheduleRule", cascade = CascadeType.ALL, orphanRemoval = true)
	private final List<DayOfWeekTimeRange> dayOfWeekRanges = new ArrayList<>();

	/**
	 * Constructs an empty instance for persistence hydration.
	 */
	ScheduleRule() {
	}

	/**
	 * Creates a new {@code ScheduleRule} with the given name and parent schedule.
	 *
	 * @param  name                     the human-readable name of the rule; must
	 *                                  not be {@code null} or blank
	 * @param  schedule                 the {@link Schedule} to which this rule
	 *                                  belongs; must not be {@code null}
	 * @throws NullPointerException     if {@code name} or {@code schedule} is
	 *                                  {@code null}
	 * @throws IllegalArgumentException if {@code name} is blank
	 */
	public ScheduleRule(final String name, final Schedule schedule) {
		this.name = Strings.requireNonBlank(name, "name can't be null or blank");
		this.schedule = Objects.requireNonNull(schedule, "schedule can't be null");
	}

	/**
	 * Returns the surrogate identifier of this schedule rule.
	 *
	 * @return the internal identifier, or {@code null} if the entity has not yet
	 *         been persisted
	 */
	public Long getId() {
		return this.id;
	}

	/**
	 * Assigns the surrogate identifier of this schedule rule.
	 * <p>
	 * This method exists exclusively for testing purposes and must not be used in
	 * production code.
	 * </p>
	 *
	 * @param id the identifier to assign
	 */
	void setId(final Long id) {
		this.id = id;
	}

	/**
	 * Returns the human-readable name of this rule.
	 *
	 * @return the rule name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Replaces the rule name, which participates in equality and hashing.
	 *
	 * @param  name                     the new non-null, nonblank rule name
	 * @throws NullPointerException     if {@code name} is {@code null}
	 * @throws IllegalArgumentException if {@code name} is blank
	 */
	public void setName(final String name) {
		this.name = Strings.requireNonBlank(name, "name can't be null or blank");
	}

	/**
	 * Returns the schedule to which this rule belongs.
	 *
	 * @return the parent {@link Schedule}
	 */
	public Schedule getSchedule() {
		return this.schedule;
	}

	/**
	 * Returns the start date of the rule validity period.
	 *
	 * @return the start date, or {@code null} if the rule has no lower bound
	 */
	public LocalDate getStartDate() {
		return this.startDate;
	}

	/**
	 * Returns the end date of the rule validity period.
	 *
	 * @return the end date, or {@code null} if the rule has no upper bound
	 */
	public LocalDate getEndDate() {
		return this.endDate;
	}

	/**
	 * Defines the validity period of this schedule rule.
	 * <p>
	 * Either {@code startDate}, {@code endDate}, or both may be {@code null},
	 * indicating an open-ended validity range. When both dates are present,
	 * {@code endDate} must not be before {@code startDate}.
	 * </p>
	 *
	 * @param  startDate                the start date of the validity period, or
	 *                                  {@code null}
	 * @param  endDate                  the end date of the validity period, or
	 *                                  {@code null}
	 * @throws IllegalArgumentException if both dates are provided and
	 *                                  {@code endDate} is before {@code startDate}
	 */
	public void setActivePeriod(final LocalDate startDate, final LocalDate endDate) {
		if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
			throw new IllegalArgumentException("endDate must not be before startDate");
		}
		this.startDate = startDate;
		this.endDate = endDate;
	}

	/**
	 * Returns an unmodifiable view of the day-of-week time ranges defined by this
	 * rule.
	 *
	 * @return an unmodifiable list of {@link DayOfWeekTimeRange} instances
	 */
	public List<DayOfWeekTimeRange> getDayOfWeekRanges() {
		return Collections.unmodifiableList(this.dayOfWeekRanges);
	}

	/**
	 * Appends a day-specific range to this rule, including duplicate ranges.
	 * <p>
	 * The range's parent reference is not changed or checked.
	 *
	 * @param  range                the range to append; must not be {@code null}
	 * @return                      {@code true} when the range is appended
	 * @throws NullPointerException if {@code range} is {@code null}
	 */
	public boolean addRange(final DayOfWeekTimeRange range) {
		Objects.requireNonNull(range, "range can't be null");
		return this.dayOfWeekRanges.add(range);
	}

	/**
	 * Removes a day-of-week time range from this rule.
	 *
	 * @param  range                the {@link DayOfWeekTimeRange} to remove; must
	 *                              not be {@code null}
	 * @return                      {@code true} if the range was present and
	 *                              removed
	 * @throws NullPointerException if {@code range} is {@code null}
	 */
	public boolean removeRange(final DayOfWeekTimeRange range) {
		Objects.requireNonNull(range, "range can't be null");
		return this.dayOfWeekRanges.remove(range);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.name, this.schedule);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || this.getClass() != obj.getClass()) {
			return false;
		}
		final ScheduleRule other = (ScheduleRule) obj;
		return Objects.equals(this.name, other.name) && Objects.equals(this.schedule, other.schedule);
	}

	@Override
	public String toString() {
		return this.schedule + " - " + this.name;
	}
}
