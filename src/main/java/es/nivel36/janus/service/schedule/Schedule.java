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
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.hibernate.annotations.NaturalId;

import es.nivel36.janus.service.employee.Employee;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
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
public class Schedule implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * Unique identifier for the schedule. Auto-generated.
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * Name of the schedule. Cannot be empty.
	 */
	@NotEmpty
	private String name;

	/**
	 * Unique business code of the schedule.
	 * <p>
	 * Acts as a natural identifier for lookups and external references.
	 * </p>
	 */
	@NaturalId
	@NotEmpty
	@Column(updatable = false)
	private String code;

	/**
	 * List of rules that define the time ranges for this schedule. Each rule
	 * represents a specific time configuration (e.g., Monday to Thursday 9:00 to
	 * 18:00, Friday 8:00 to 15:00).
	 */
	@OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL, orphanRemoval = true)
	private final Set<ScheduleRule> rules = new HashSet<>();

	/**
	 * List of employees assigned to this schedule. Each employee has a reference to
	 * their work schedule, and this field establishes the relationship from the
	 * schedule's perspective.
	 */
	@OneToMany(mappedBy = "schedule")
	private Set<Employee> employees = new HashSet<>();

	Schedule() {
	}

	/**
	 * Creates a new {@code Schedule} with the given business code and name.
	 *
	 * <p>
	 * The {@code code} acts as a natural identifier for the schedule and must be
	 * unique. The {@code name} represents the human-readable name of the schedule.
	 * </p>
	 *
	 * @param code the unique business code of the schedule. Can't be {@code null}.
	 * @param name the unique name of the schedule. Can't be {@code null}.
	 * @throws NullPointerException if {@code code} or {@code name} is {@code null}
	 */
	public Schedule(final String code, final String name) {
		this.code = Objects.requireNonNull(code, "code can't be null");
		this.name = Objects.requireNonNull(name, "name can't be null");
	}

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
	void setId(final Long id) {
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
		return this.code;
	}

	/**
	 * Returns the set of {@link ScheduleRule} objects that define this schedule's
	 * time rules.
	 *
	 * @return the set of schedule rules
	 */
	public Set<ScheduleRule> getRules() {
		return Collections.unmodifiableSet(this.rules);
	}

	public boolean addRule(final ScheduleRule rule) {
		Objects.requireNonNull(rule, "rule can't be null");
		return this.rules.add(rule);
	}

	public boolean removeRule(final ScheduleRule rule) {
		Objects.requireNonNull(rule, "rule can't be null");
		return this.rules.remove(rule);
	}

	public void clearRules() {
		this.rules.clear();
	}

	/**
	 * Returns the set of employees assigned to this schedule.
	 *
	 * @return the set of employees associated with this schedule
	 */
	public Set<Employee> getEmployees() {
		return Collections.unmodifiableSet(this.employees);
	}

	/**
	 * Sets the set of employees assigned to this schedule.
	 *
	 * @param employees the new set of employees to associate with this schedule
	 */
	public void setEmployees(final Set<Employee> employees) {
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
		if (obj == null || this.getClass() != obj.getClass()) {
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
