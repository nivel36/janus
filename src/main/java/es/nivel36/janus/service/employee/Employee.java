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
package es.nivel36.janus.service.employee;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.hibernate.annotations.NaturalId;

import es.nivel36.janus.service.schedule.Schedule;
import es.nivel36.janus.service.timelog.TimeLog;
import es.nivel36.janus.service.worksite.Worksite;
import es.nivel36.janus.util.Strings;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * Entity representing an employee. This entity contains personal information
 * about the employee and maintains a list of time logs associated with the
 * employee.
 *
 * <p>
 * Each employee is uniquely identified by their email address. In addition to
 * time logs, the employee also has an associated work schedule represented by a
 * {@link Schedule} entity, which is mandatory and cannot be null.
 * </p>
 *
 * @see TimeLog
 */
@Entity
public class Employee implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * Unique identifier for the employee. Auto-generated.
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * The first name of the employee.
	 * 
	 * <p>
	 * This field is mandatory and must not be empty.
	 * </p>
	 */
	@NotEmpty
	private String name;

	/**
	 * The surname of the employee.
	 * 
	 * <p>
	 * This field is mandatory and must not be empty.
	 * </p>
	 */
	@NotEmpty
	private String surname;

	/**
	 * The email of the employee.
	 * 
	 * <p>
	 * Acts as a natural identifier. This field is mandatory, must be unique, and
	 * cannot be updated once the entity is persisted.
	 * </p>
	 */
	@NaturalId
	@NotEmpty
	@Email
	@Column(updatable = false)
	private String email;

	/**
	 * The work schedule associated with this employee.
	 * 
	 * <p>
	 * This field is mandatory and cannot be {@code null}.
	 * </p>
	 */
	@NotNull
	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "schedule_id")
	private Schedule schedule;

	/**
	 * The worksites where the employee is allowed to record their time logs.
	 */
	@ManyToMany
	@JoinTable(name = "employee_worksite", //
			joinColumns = @JoinColumn(name = "employee_id"), //
			inverseJoinColumns = @JoinColumn(name = "worksite_id"))
	private Set<Worksite> worksites = new HashSet<>();

	/**
	 * The set of time logs registered by this employee.
	 */
	@OneToMany(mappedBy = "employee", fetch = FetchType.LAZY)
	private Set<TimeLog> timeLogs = new HashSet<>();

	/**
	 * Default constructor for JPA. Initializes an empty employee.
	 */
	Employee() {
	}

	/**
	 * Constructs a new employee with the specified name, surname, email and
	 * schedule.
	 *
	 * @param name     the name of the employee. Can't be {@code null} or blank.
	 * @param surname  the surname of the employee. Can't be {@code null} or blank.
	 * @param email    the unique email of the employee. Can't be {@code null} or
	 *                 blank.
	 * @param schedule the work schedule assigned to the employee. Can't be
	 *                 {@code null}.
	 */
	public Employee(final String name, final String surname, final String email, final Schedule schedule) {
		this.name = Strings.requireNonBlank(name, "name can't be null or blank");
		this.surname = Strings.requireNonBlank(surname, "surname can't be null or blank");
		this.email = Strings.requireNonBlank(email, "email can't be null or blank");
		this.schedule = Objects.requireNonNull(schedule, "schedule can't be null");
	}

	/**
	 * Gets the email of the employee.
	 *
	 * @return the email of the employee
	 */
	public String getEmail() {
		return this.email;
	}

	/**
	 * Gets the unique identifier of the employee.
	 *
	 * @return the ID of the employee
	 */
	public Long getId() {
		return this.id;
	}

	/**
	 * Gets the first name of the employee.
	 *
	 * @return the first name of the employee
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Gets the work schedule associated with this employee.
	 *
	 * @return the {@link Schedule} of the employee
	 */
	public Schedule getSchedule() {
		return this.schedule;
	}

	/**
	 * Gets the surname of the employee.
	 *
	 * @return the surname of the employee
	 */
	public String getSurname() {
		return this.surname;
	}

	/**
	 * Gets the unmodifiable set of time logs recorded by this employee.
	 *
	 * @return the unmodifiable set of {@link TimeLog} entries associated with this
	 *         employee
	 */
	public Set<TimeLog> getTimeLogs() {
		return Collections.unmodifiableSet(timeLogs);
	}

	/**
	 * Gets the unmodifiable set of worksites where this employee can register their
	 * time logs.
	 *
	 * @return the unmodifiable set of {@link Worksite} entities linked to this
	 *         employee
	 */
	public Set<Worksite> getWorksites() {
		return Collections.unmodifiableSet(this.worksites);
	}

	/**
	 * Sets the unique identifier of the employee.
	 *
	 * @param id the ID to assign to the employee
	 */
	void setId(final Long id) {
		this.id = id;
	}

	/**
	 * Sets the work schedule of the employee.
	 *
	 * @param schedule the {@link Schedule} to assign to the employee
	 */
	public void setSchedule(final Schedule schedule) {
		this.schedule = Objects.requireNonNull(schedule, "schedule can't be null");
	}

	/**
	 * Sets the surname of the employee.
	 *
	 * @param surname the surname to assign to the employee
	 */
	public void setFullName(final String name, final String surname) {
		this.name = Strings.requireNonBlank(name, "name can't be null");
		this.surname = Strings.requireNonBlank(surname, "surname can't be null");
	}

	/**
	 * Sets the set of time logs for this employee.
	 *
	 * @param timeLogs the set of {@link TimeLog} entries to assign
	 */
	void setTimeLogs(final Set<TimeLog> timeLogs) {
		this.timeLogs = timeLogs;
	}

	public boolean assignToWorksite(final Worksite worksite) {
		Objects.requireNonNull(worksite, "worksite can't be null");
		return this.worksites.add(worksite);
	}

	public boolean removeFromWorksite(final Worksite worksite) {
		Objects.requireNonNull(worksite, "worksite can't be null");
		return this.worksites.remove(worksite);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || this.getClass() != obj.getClass()) {
			return false;
		}
		final Employee other = (Employee) obj;
		return Objects.equals(this.email, other.email);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.email);
	}

	@Override
	public String toString() {
		return this.email;
	}
}
