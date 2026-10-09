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
import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.hibernate.annotations.NaturalId;

import es.nivel36.janus.service.employee.Employee;
import es.nivel36.janus.util.Strings;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * JPA entity representing a work schedule.
 * <p>
 * A {@code Schedule} defines a set of temporal rules that describe when work is
 * expected to be performed. These rules are expressed as {@link ScheduleRule}
 * instances and may vary by day of the week or by specific date ranges (for
 * example, seasonal or holiday schedules).
 * </p>
 * <p>
 * The entity is identified internally by a surrogate primary key, while the
 * {@code code} field acts as a natural identifier with business meaning.
 * Equality and hash code are therefore based exclusively on this natural
 * identifier.
 * </p>
 */
@Entity
public class Schedule implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank
	@Column(columnDefinition = "text")
	private String name;

	@NaturalId
	@NotBlank
	@Column(updatable = false, columnDefinition = "text")
	private String code;

	@NotNull
	@Column(nullable = false)
	private Duration entryTolerance = Duration.ZERO;

	@NotNull
	@Column(nullable = false)
	private Duration exitTolerance = Duration.ZERO;

	@OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL, orphanRemoval = true)
	private final Set<ScheduleRule> rules = new HashSet<>();

	@OneToMany(mappedBy = "schedule")
	private Set<Employee> employees = new HashSet<>();

	/**
	 * Constructs an empty instance for persistence hydration.
	 */
	Schedule() {
	}

	/**
	 * Creates a new {@code Schedule} with the given business code, name and
	 * tolerances.
	 *
	 * @param  code                     the unique business code of the schedule;
	 *                                  must not be {@code null} or blank
	 * @param  name                     the human-readable name of the schedule;
	 *                                  must not be {@code null}
	 * @param  entryTolerance           allowed tolerance for entry times; must not
	 *                                  be {@code null}
	 * @param  exitTolerance            allowed tolerance for exit times; must not
	 *                                  be {@code null}
	 * @throws NullPointerException     if any argument is {@code null}
	 * @throws IllegalArgumentException if {@code code} or {@code name} is blank
	 */
	public Schedule(final String code, final String name, final Duration entryTolerance, final Duration exitTolerance) {
		this.code = Strings.requireNonBlank(code, "code can't be null or blank");
		this.setName(name);
		this.setEntryTolerance(entryTolerance);
		this.setExitTolerance(exitTolerance);
	}

	/**
	 * Returns the surrogate identifier of the schedule.
	 *
	 * @return the internal identifier, or {@code null} if the entity has not yet
	 *         been persisted
	 */
	public Long getId() {
		return this.id;
	}

	/**
	 * Assigns the surrogate identifier of the schedule.
	 * <p>
	 * This method exists exclusively for testing purposes and must not be used in
	 * production code. It allows controlled assignment of the identifier when
	 * working with manually constructed or detached entities in tests.
	 * </p>
	 *
	 * @param id the identifier to assign
	 */
	void setId(final Long id) {
		this.id = id;
	}

	/**
	 * Returns the human-readable name of the schedule.
	 *
	 * @return the name of the schedule
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Updates the human-readable name of the schedule.
	 *
	 * @param  name                     the new name of the schedule; must not be
	 *                                  blank or {@code null}
	 * @throws NullPointerException     if {@code name} is {@code null}
	 * @throws IllegalArgumentException if {@code name} is blank
	 */
	public void setName(final String name) {
		this.name = Strings.requireNonBlank(name, "name can't be null");
	}

	/**
	 * Returns the unique business code of the schedule.
	 *
	 * @return the natural identifier of the schedule
	 */
	public String getCode() {
		return this.code;
	}

	/**
	 * Returns the allowed tolerance for entry times.
	 *
	 * @return the entry tolerance
	 */
	public Duration getEntryTolerance() {
		return this.entryTolerance;
	}

	/**
	 * Updates the allowed tolerance for entry times.
	 *
	 * @param  entryTolerance       the new entry tolerance; must not be
	 *                              {@code null}
	 * @throws NullPointerException if {@code entryTolerance} is {@code null}
	 */
	public void setEntryTolerance(final Duration entryTolerance) {
		this.entryTolerance = Objects.requireNonNull(entryTolerance, "entryTolerance can't be null");
	}

	/**
	 * Returns the allowed tolerance for exit times.
	 *
	 * @return the exit tolerance
	 */
	public Duration getExitTolerance() {
		return this.exitTolerance;
	}

	/**
	 * Updates the allowed tolerance for exit times.
	 *
	 * @param  exitTolerance        the new exit tolerance; must not be {@code null}
	 * @throws NullPointerException if {@code exitTolerance} is {@code null}
	 */
	public void setExitTolerance(final Duration exitTolerance) {
		this.exitTolerance = Objects.requireNonNull(exitTolerance, "exitTolerance can't be null");
	}

	/**
	 * Returns an unmodifiable view of the rules that define this schedule.
	 * <p>
	 * The returned collection must not be modified directly. Any change to the
	 * rules must be performed through the provided mutator methods.
	 * </p>
	 *
	 * @return an unmodifiable set of {@link ScheduleRule} instances
	 */
	public Set<ScheduleRule> getRules() {
		return Collections.unmodifiableSet(this.rules);
	}

	/**
	 * Adds a rule to this schedule.
	 *
	 * @param  rule                 the {@link ScheduleRule} to add; must not be
	 *                              {@code null}
	 * @return                      {@code true} if the rule was not already present
	 * @throws NullPointerException if {@code rule} is {@code null}
	 */
	public boolean addRule(final ScheduleRule rule) {
		Objects.requireNonNull(rule, "rule can't be null");
		return this.rules.add(rule);
	}

	/**
	 * Removes a rule from this schedule.
	 *
	 * @param  rule                 the {@link ScheduleRule} to remove; must not be
	 *                              {@code null}
	 * @return                      {@code true} if the rule was present and removed
	 * @throws NullPointerException if {@code rule} is {@code null}
	 */
	public boolean removeRule(final ScheduleRule rule) {
		Objects.requireNonNull(rule, "rule can't be null");
		return this.rules.remove(rule);
	}

	/**
	 * Removes all rules from this schedule.
	 * <p>
	 * After invocation, the schedule will contain no temporal definitions.
	 * </p>
	 */
	public void clearRules() {
		this.rules.clear();
	}

	/**
	 * Returns an unmodifiable view of the employees assigned to this schedule.
	 * <p>
	 * The view reflects changes to the backing set. Related entity changes do not
	 * automatically synchronize this collection unless the association is updated
	 * on both sides.
	 *
	 * @return the unmodifiable association view
	 */
	public Set<Employee> getEmployees() {
		return Collections.unmodifiableSet(this.employees);
	}

	/**
	 * Replaces the set of employees assigned to this schedule.
	 * <p>
	 * This method exists exclusively for testing purposes and must not be used in
	 * production code.
	 * </p>
	 *
	 * @param  employees            the set of employees to associate with this
	 *                              schedule; must not be {@code null}
	 * @throws NullPointerException if {@code employees} is {@code null}
	 */
	void setEmployees(final Set<Employee> employees) {
		this.employees = Objects.requireNonNull(employees, "employees can't be null");
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
