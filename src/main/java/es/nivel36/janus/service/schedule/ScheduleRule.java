package es.nivel36.janus.service.schedule;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;

/**
 * Represents a specific rule within a schedule, defining time ranges for
 * different days of the week or a specific period.
 *
 * <p>
 * A {@code ScheduleRule} is part of a {@link Schedule} and contains details
 * about time configurations for specific days or periods. It can include a
 * start and end date to represent temporary or seasonal changes in the
 * schedule.
 * </p>
 *
 * <p>
 * Each rule is uniquely identified by its name and belongs to a specific
 * schedule, allowing flexible time range configurations such as work hours for
 * different days or special periods like summer hours.
 * </p>
 */
@Entity(name = "SCHEDULE_RULE")
@Table(indexes = { //
		@Index(name = "idx_schedule_rule_name", columnList = "name") //
}, uniqueConstraints = { //
		@UniqueConstraint(name = "uk_schedule_rule_name", columnNames = { "name" }) //
})
public class ScheduleRule implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * Unique identifier for the schedule rule. Auto-generated.
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * Unique name of the schedule rule. Cannot be null and must be unique across
	 * all schedule rules.
	 */
	@NotNull
	@Column(nullable = false, unique = true)
	private String name;

	/**
	 * The schedule to which this rule belongs.
	 */
	@ManyToOne
	@JoinColumn(name = "schedule_id")
	private Schedule schedule;

	/**
	 * The start date of the schedule rule. It can be null if the rule applies
	 * indefinitely or to the entire schedule.
	 */
	@Column(name = "START_DATE")
	private LocalDate startDate;

	/**
	 * The end date of the schedule rule. It can be null if the rule applies
	 * indefinitely or to the entire schedule.
	 */
	@Column(name = "END_DATE")
	private LocalDate endDate;

	/**
	 * Many-to-Many relationship with {@link DayOfWeekTimeRange}. A schedule rule
	 * can have multiple time ranges, and the same time range can belong to multiple
	 * schedule rules.
	 */
	@ManyToMany(cascade = CascadeType.ALL)
	@JoinTable(name = "schedule_rule_day_of_week_time_range", joinColumns = @JoinColumn(name = "schedule_rule_id"), inverseJoinColumns = @JoinColumn(name = "day_of_week_time_range_id"))
	private List<DayOfWeekTimeRange> dayOfWeekRanges = new ArrayList<>();

	/**
	 * Returns the unique identifier of the schedule rule.
	 *
	 * @return the rule's unique identifier
	 */
	public Long getId() {
		return this.id;
	}

	/**
	 * Sets the unique identifier of the schedule rule.
	 *
	 * @param id the new identifier of the schedule rule
	 */
	public void setId(final Long id) {
		this.id = id;
	}

	/**
	 * Returns the unique name of the schedule rule.
	 *
	 * @return the rule's name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Sets the unique name of the schedule rule. The name cannot be null and must
	 * be unique.
	 *
	 * @param name the new name of the schedule rule
	 */
	public void setName(final String name) {
		this.name = name;
	}

	/**
	 * Returns the schedule to which this rule belongs.
	 *
	 * @return the schedule
	 */
	public Schedule getSchedule() {
		return this.schedule;
	}

	/**
	 * Sets the schedule to which this rule belongs.
	 *
	 * @param schedule the schedule to associate with this rule
	 */
	public void setSchedule(final Schedule schedule) {
		this.schedule = schedule;
	}

	/**
	 * Returns the start date of the schedule rule.
	 *
	 * @return the start date of the rule, or null if it applies indefinitely
	 */
	public LocalDate getStartDate() {
		return this.startDate;
	}

	/**
	 * Sets the start date of the schedule rule.
	 *
	 * @param startDate the new start date of the rule
	 */
	public void setStartDate(final LocalDate startDate) {
		this.startDate = startDate;
	}

	/**
	 * Returns the end date of the schedule rule.
	 *
	 * @return the end date of the rule, or null if it applies indefinitely
	 */
	public LocalDate getEndDate() {
		return this.endDate;
	}

	/**
	 * Sets the end date of the schedule rule.
	 *
	 * @param endDate the new end date of the rule
	 */
	public void setEndDate(final LocalDate endDate) {
		this.endDate = endDate;
	}

	/**
	 * Returns the list of {@link DayOfWeekTimeRange} objects that define the time
	 * configurations for different days of the week.
	 *
	 * @return the list of time ranges for each day
	 */
	public List<DayOfWeekTimeRange> getDayOfWeekRanges() {
		return this.dayOfWeekRanges;
	}

	/**
	 * Sets the list of {@link DayOfWeekTimeRange} objects that define the time
	 * configurations for different days of the week.
	 *
	 * @param dayOfWeekRanges the new list of time ranges for each day
	 */
	public void setDayOfWeekRanges(final List<DayOfWeekTimeRange> dayOfWeekRanges) {
		this.dayOfWeekRanges = dayOfWeekRanges;
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
		final ScheduleRule other = (ScheduleRule) obj;
		return Objects.equals(this.name, other.name);
	}

	@Override
	public String toString() {
		return this.name;
	}
}
