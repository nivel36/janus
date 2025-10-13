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
package es.nivel36.janus.service.worksite;

import java.io.Serializable;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import es.nivel36.janus.service.employee.Employee;
import es.nivel36.janus.service.timelog.TimeLog;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;

/**
 * Entity representing a physical or virtual worksite where employees can
 * register their {@link TimeLog} entries.
 * <p>
 * Each worksite is uniquely identified by its {@code code}, has a
 * human-readable {@code name}, and is associated with a
 * {@link java.time.ZoneId} that defines its local working time zone.
 * </p>
 *
 * <p>
 * <b>Soft delete:</b> this entity uses logical deletion via the {@code deleted}
 * flag. The Hibernate annotations {@link org.hibernate.annotations.SQLDelete}
 * and {@link org.hibernate.annotations.SQLRestriction} ensure that delete
 * operations update the flag instead of physically removing the record, and
 * that queries exclude logically deleted rows by default.
 * </p>
 *
 * @see Employee
 * @see TimeLog
 */
@SQLDelete(sql = "UPDATE WORKSITE SET deleted = true WHERE id = ?")
@SQLRestriction("deleted = false")
@Entity
@Table(indexes = { @Index(name = "idx_worksite_code", columnList = "code") }, uniqueConstraints = {
		@UniqueConstraint(name = "uk_worksite_code", columnNames = { "code" }) })
public class Worksite implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * Unique identifier of the worksite. Auto-generated.
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * Human-readable name of the worksite.
	 */
	@NotNull
	@Column(nullable = false, length = 250)
	private String name;

	/**
	 * Unique business code of the worksite.
	 * <p>
	 * Acts as a natural identifier for lookups and external references.
	 * </p>
	 */
	@NotNull
	@Column(nullable = false, unique = true, length = 50)
	private String code;

	/**
	 * Time zone associated with the worksite.
	 * <p>
	 * Persisted as a string (e.g., {@code "Europe/Madrid"}) using a JPA
	 * {@link jakarta.persistence.Converter} (see {@code ZoneIdConverter}).
	 * </p>
	 */
	@NotNull
	@Column(nullable = false, length = 80)
	private ZoneId timeZone;

	/**
	 * Logical deletion flag.
	 * <p>
	 * When {@code true}, the worksite is considered deleted but remains in the
	 * database for auditing purposes.
	 * </p>
	 */
	@NotNull
	@Column(nullable = false)
	private boolean deleted = false;

	/**
	 * Employees assigned to this worksite.
	 */
	@ManyToMany(mappedBy = "worksites")
	private Set<Employee> employees = new HashSet<>();

	/**
	 * Time logs registered at this worksite.
	 */
	@OneToMany(mappedBy = "worksite", fetch = FetchType.LAZY)
	private Set<TimeLog> timeLogs = new HashSet<>();

	/**
	 * Default constructor required by JPA.
	 */
	public Worksite() {
	}

	/**
	 * Constructs a new {@code Worksite} with the required attributes.
	 *
	 * @param code     the unique code of the worksite
	 * @param name     the human-readable name of the worksite
	 * @param timeZone the time zone associated with the worksite
	 */
	public Worksite(final String code, final String name, final ZoneId timeZone) {
		this.code = code;
		this.name = name;
		this.timeZone = timeZone;
	}

	/**
	 * Gets the unique identifier of the worksite.
	 *
	 * @return the ID of the worksite
	 */
	public Long getId() {
		return id;
	}

	/**
	 * Sets the unique identifier of the worksite.
	 *
	 * @param id the ID to assign
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Gets the human-readable name of the worksite.
	 *
	 * @return the name of the worksite
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the human-readable name of the worksite.
	 *
	 * @param name the name to assign
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Gets the unique business code of the worksite.
	 *
	 * @return the code of the worksite
	 */
	public String getCode() {
		return code;
	}

	/**
	 * Sets the unique business code of the worksite.
	 *
	 * @param code the code to assign
	 */
	public void setCode(String code) {
		this.code = code;
	}

	/**
	 * Gets the time zone of the worksite.
	 *
	 * @return the {@link ZoneId} of the worksite
	 */
	public ZoneId getTimeZone() {
		return timeZone;
	}

	/**
	 * Sets the time zone of the worksite.
	 *
	 * @param timeZone the {@link ZoneId} to assign
	 */
	public void setTimeZone(ZoneId timeZone) {
		this.timeZone = timeZone;
	}

	/**
	 * Checks whether the worksite is logically deleted.
	 *
	 * @return {@code true} if deleted; {@code false} otherwise
	 */
	public boolean isDeleted() {
		return deleted;
	}

	/**
	 * Sets the logical deletion flag of the worksite.
	 *
	 * @param deleted {@code true} to mark as deleted; {@code false} otherwise
	 */
	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}

	/**
	 * Gets the employees assigned to this worksite.
	 *
	 * @return the set of {@link Employee} entities
	 */
	public Set<Employee> getEmployees() {
		return employees;
	}

	/**
	 * Sets the employees assigned to this worksite.
	 *
	 * @param employees the set of {@link Employee} entities to assign
	 */
	public void setEmployees(Set<Employee> employees) {
		this.employees = employees;
	}

	/**
	 * Gets the time logs registered at this worksite.
	 *
	 * @return the set of {@link TimeLog} entities
	 */
	public Set<TimeLog> getTimeLogs() {
		return timeLogs;
	}

	/**
	 * Sets the time logs registered at this worksite.
	 *
	 * @param timeLogs the set of {@link TimeLog} entities to assign
	 */
	public void setTimeLogs(Set<TimeLog> timeLogs) {
		this.timeLogs = timeLogs;
	}

	@Override
	public int hashCode() {
		return Objects.hash(code);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if ((obj == null) || (getClass() != obj.getClass())) {
			return false;
		}
		Worksite other = (Worksite) obj;
		return Objects.equals(code, other.code);
	}

	@Override
	public String toString() {
		return "Worksite [" + code + " :: " + name + "]";
	}
}
