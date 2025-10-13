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
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import es.nivel36.janus.service.schedule.Schedule;
import es.nivel36.janus.service.timelog.TimeLog;
import es.nivel36.janus.service.worksite.Worksite;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
@Table(indexes = { //
		@Index(name = "idx_employee_email", columnList = "email") //
}, uniqueConstraints = { //
		@UniqueConstraint(name = "uk_employee_email", columnNames = { "email" }) //
})
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
	 */
	@NotEmpty
	@Column(length = 255)
	private String name;

	/**
	 * The surname of the employee.
	 */
	@NotEmpty
	@Column(length = 255)
	private String surname;

	/**
	 * The email of the employee.
	 * <p>
	 * This field is mandatory, must be unique, and cannot be {@code null}.
	 * </p>
	 */
	@NotEmpty
	@Email
	@Column(nullable = false, unique = true, length = 254)
	private String email;

	/**
	 * The work schedule associated with this employee.
	 * <p>
	 * This field is mandatory and cannot be {@code null}.
	 * </p>
	 */
	@NotNull
	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "schedule_id", nullable = false)
	private Schedule schedule;

	/**
	 * The worksites where the employee is allowed to record their time logs.
	 */
	@ManyToMany
	@JoinTable(name = "employee_worksite", joinColumns = @JoinColumn(name = "employee_id"), inverseJoinColumns = @JoinColumn(name = "worksite_id"))
	private Set<Worksite> worksites = new HashSet<>();

	/**
	 * The set of time logs registered by this employee.
	 */
	@OneToMany(mappedBy = "employee", fetch = FetchType.LAZY)
	private Set<TimeLog> timeLogs = new HashSet<>();

	/**
	 * Default constructor for JPA. Initializes an empty employee.
	 */
	public Employee() {
	}

	/**
	 * Constructs a new employee with the specified name, surname, email and
	 * schedule.
	 *
	 * @param name     the name of the employee
	 * @param surname  the surname of the employee
	 * @param email    the unique email of the employee
	 * @param schedule the work schedule assigned to the employee
	 */
	public Employee(final String name, final String surname, final String email, final Schedule schedule) {
		this.name = name;
		this.surname = surname;
		this.email = email;
		this.schedule = schedule;
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
	 * Gets the set of time logs recorded by this employee.
	 *
	 * @return the set of {@link TimeLog} entries associated with this employee
	 */
	public Set<TimeLog> getTimeLogs() {
		return timeLogs;
	}

	/**
	 * Gets the worksites where this employee can register their time logs.
	 *
	 * @return the set of {@link Worksite} entities linked to this employee
	 */
	public Set<Worksite> getWorksites() {
		return worksites;
	}

	/**
	 * Sets the email of the employee.
	 *
	 * @param email the email to assign to the employee
	 */
	public void setEmail(final String email) {
		this.email = email;
	}

	/**
	 * Sets the unique identifier of the employee.
	 *
	 * @param id the ID to assign to the employee
	 */
	public void setId(final Long id) {
		this.id = id;
	}

	/**
	 * Sets the first name of the employee.
	 *
	 * @param name the first name to assign to the employee
	 */
	public void setName(final String name) {
		this.name = name;
	}

	/**
	 * Sets the work schedule of the employee.
	 *
	 * @param schedule the {@link Schedule} to assign to the employee
	 */
	public void setSchedule(final Schedule schedule) {
		this.schedule = schedule;
	}

	/**
	 * Sets the surname of the employee.
	 *
	 * @param surname the surname to assign to the employee
	 */
	public void setSurname(final String surname) {
		this.surname = surname;
	}

	/**
	 * Sets the set of time logs for this employee.
	 *
	 * @param timeLogs the set of {@link TimeLog} entries to assign
	 */
	public void setTimeLogs(Set<TimeLog> timeLogs) {
		this.timeLogs = timeLogs;
	}

	/**
	 * Sets the worksites available for this employee.
	 *
	 * @param worksites the set of {@link Worksite} entities to assign
	 */
	public void setWorksites(Set<Worksite> worksites) {
		this.worksites = worksites;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if ((obj == null) || (this.getClass() != obj.getClass())) {
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
