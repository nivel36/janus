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

	/**
	 * Returns the start time of the time range.
	 * 
	 * @return the start time
	 */
	public LocalTime getStartTime() {
		return startTime;
	}

	/**
	 * Sets the start time of the time range.
	 * 
	 * @param startTime the new start time
	 */
	public void setStartTime(LocalTime startTime) {
		this.startTime = startTime;
	}

	/**
	 * Returns the end time of the time range.
	 * 
	 * @return the end time
	 */
	public LocalTime getEndTime() {
		return endTime;
	}

	/**
	 * Sets the end time of the time range.
	 * 
	 * @param endTime the new end time
	 */
	public void setEndTime(LocalTime endTime) {
		this.endTime = endTime;
	}

	@Override
	public int hashCode() {
		return Objects.hash(endTime, startTime);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TimeRange other = (TimeRange) obj;
		return Objects.equals(endTime, other.endTime) && Objects.equals(startTime, other.startTime);
	}

	@Override
	public String toString() {
		return "TimeRange [startTime=" + startTime + ", endTime=" + endTime + "]";
	}
}
