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
import java.time.LocalTime;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Represents a time range with a start and end time.
 *
 * <p>
 * A {@code TimeRange} defines a specific period during the day, represented by
 * a start time and an end time. It is used within a {@link DayOfWeekTimeRange}
 * to specify the working hours or other time configurations for a given day.
 * </p>
 *
 * <p>
 * This class is {@code @Embeddable} and can be embedded within other entities
 * to represent time ranges as part of their structure.
 * </p>
 */
@Embeddable
public class TimeRange implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * The start time of the time range.
	 */
	@Column(name = "START_TIME")
	private LocalTime startTime;

	/**
	 * The end time of the time range.
	 */
	@Column(name = "END_TIME")
	private LocalTime endTime;
	
	public TimeRange() {
	}

	public TimeRange(final LocalTime startTime, final LocalTime endTime) {
		this.startTime = Objects.requireNonNull(startTime, "StartTime can't be null");
		this.endTime = Objects.requireNonNull(endTime, "EndTime can't be null");
	}

	/**
	 * Returns the start time of the time range.
	 *
	 * @return the start time
	 */
	public LocalTime getStartTime() {
		return this.startTime;
	}

	/**
	 * Sets the start time of the time range.
	 *
	 * @param startTime the new start time
	 */
	public void setStartTime(final LocalTime startTime) {
		this.startTime = startTime;
	}

	/**
	 * Returns the end time of the time range.
	 *
	 * @return the end time
	 */
	public LocalTime getEndTime() {
		return this.endTime;
	}

	/**
	 * Sets the end time of the time range.
	 *
	 * @param endTime the new end time
	 */
	public void setEndTime(final LocalTime endTime) {
		this.endTime = endTime;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.endTime, this.startTime);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if ((obj == null) || (this.getClass() != obj.getClass())) {
			return false;
		}
		final TimeRange other = (TimeRange) obj;
		return Objects.equals(this.endTime, other.endTime) && Objects.equals(this.startTime, other.startTime);
	}

	@Override
	public String toString() {
		return "TimeRange [startTime=" + this.startTime + ", endTime=" + this.endTime + "]";
	}
}
