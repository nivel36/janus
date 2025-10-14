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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.hibernate.annotations.NaturalId;

import es.nivel36.janus.service.employee.Employee;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotEmpty;

/**
 * Represents a work schedule for a person, containing a set of rules that
 * define time ranges for each day of the week or a specific period.
 *
 * <p>
 * This entity is uniquely identified by its code, and it allows defining custom
 * schedules that can vary across days of the week or specific time periods
 * (e.g., different schedules for summer or holidays).
 * </p>
 *
 * <p>
 * Each {@code Schedule} has a list of {@link ScheduleRule} objects, which
 * specify the actual rules for the time ranges, and a list of {@link Employee}
 * entities representing the employees assigned to this schedule.
 * </p>
 */
@Entity
@Table(indexes = { //
		@Index(name = "idx_schedule_code", columnList = "code") //
}, uniqueConstraints = { //
		@UniqueConstraint(name = "uk_schedule_code", columnNames = { "code" }) //
})
public class Schedule implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * Unique identifier for the schedule. Auto-generated.
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * Unique name of the schedule. Cannot be null and must be unique across all
	 * schedules.
	 */
	@NotEmpty
	@Column(nullable = false, length = 255)
	private String name;

	/**
	 * Unique business code of the schedule.
	 * <p>
	 * Acts as a natural identifier for lookups and external references.
	 * </p>
	 */
	@NaturalId
	@NotEmpty
	@Column(nullable = false, unique = true, updatable = false, length = 50)
	private String code;

	/**
	 * List of rules that define the time ranges for this schedule. Each rule
	 * represents a specific time configuration (e.g., Monday to Thursday 9:00 to
	 * 18:00, Friday 8:00 to 15:00).
	 */
	@OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ScheduleRule> rules = new ArrayList<>();

	/**
	 * List of employees assigned to this schedule. Each employee has a reference to
	 * their work schedule, and this field establishes the relationship from the
	 * schedule's perspective.
	 */
	@OneToMany(mappedBy = "schedule")
	private List<Employee> employees = new ArrayList<>();

	/**
	 * Returns the unique identifier of the schedule.
	 * 
	 * @return the schedule's unique identifier
	 */
	public Long getId() {
		return this.id;
	}

	/**
	 * Sets the unique identifier of the schedule.
	 * 
	 * @param id the new identifier of the schedule
	 */
	public void setId(final Long id) {
		this.id = id;
	}

	/**
	 * Returns the unique name of the schedule.
	 * 
	 * @return the schedule's name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Sets the unique name of the schedule. The name cannot be null and must be
	 * unique.
	 * 
	 * @param name the new name of the schedule
	 */
	public void setName(final String name) {
		this.name = name;
	}

	/**
	 * Gets the unique business code of the schedule.
	 *
	 * @return the code of the schedule
	 */
	public String getCode() {
		return code;
	}

	/**
	 * Sets the unique business code of the schedule.
	 *
	 * @param code the code to assign
	 */
	public void setCode(String code) {
		this.code = code;
	}

	/**
	 * Returns the list of {@link ScheduleRule} objects that define this schedule's
	 * time rules.
	 * 
	 * @return the list of schedule rules
	 */
	public List<ScheduleRule> getRules() {
		return this.rules;
	}

	/**
	 * Sets the list of {@link ScheduleRule} objects that define this schedule's
	 * time rules.
	 * 
	 * @param rules the new list of schedule rules
	 */
	public void setRules(final List<ScheduleRule> rules) {
		this.rules = rules;
	}

	/**
	 * Returns the list of employees assigned to this schedule.
	 * 
	 * @return the list of employees associated with this schedule
	 */
	public List<Employee> getEmployees() {
		return this.employees;
	}

	/**
	 * Sets the list of employees assigned to this schedule.
	 * 
	 * @param employees the new list of employees to associate with this schedule
	 */
	public void setEmployees(final List<Employee> employees) {
		this.employees = employees;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.code);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if ((obj == null) || (this.getClass() != obj.getClass())) {
			return false;
		}
		final Schedule other = (Schedule) obj;
		return Objects.equals(this.code, other.code);
	}

	@Override
	public String toString() {
		return this.code;
	}
}
