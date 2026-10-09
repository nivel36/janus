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
package es.nivel36.janus.service.applicationsettings;

import java.io.Serializable;
import java.time.ZoneId;
import java.util.Objects;

import es.nivel36.janus.service.timelog.TimeLog;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Entity that stores application-wide administrative configuration values.
 * <p>
 * This class is mapped to the {@code APPLICATION_SETTINGS} table and represents
 * global configuration parameters that affect the behavior of the application.
 * The single configuration has the fixed identifier 1 and is used to control
 * administrative rules such as modification limits and feature enablement.
 * </p>
 */
@Entity
@Table(name = "APPLICATION_SETTINGS")
public class ApplicationSettings implements Serializable {

	private static final long serialVersionUID = 1L;

	static final Long GLOBAL_SETTINGS_ID = 1L;

	@Id
	@Column(name = "ID", nullable = false, updatable = false)
	private Long id = GLOBAL_SETTINGS_ID;

	@PositiveOrZero
	@Column(name = "DAYS_UNTIL_LOCKED", nullable = false)
	private int daysUntilLocked;

	@Column(name = "EMPLOYEE_WORKPLACE_CREATION_ALLOWED", nullable = false)
	private boolean employeeWorksiteCreationAllowed;

	@Column(name = "WORKSITE_CHANGE_DURING_SHIFT_ALLOWED", nullable = false)
	private boolean worksiteChangeDuringShiftAllowed;

	@Column(name = "EMPLOYEE_MANUAL_TIMELOG_ENTRY_ALLOWED", nullable = false)
	private boolean employeeManualTimeLogEntryAllowed;

	@NotNull
	@Column(name = "DEFAULT_TIMEZONE", nullable = false, columnDefinition = "text")
	private ZoneId defaultTimezone = ZoneId.of("Europe/Madrid");

	/**
	 * Protected no-argument constructor required by persistence frameworks.
	 * <p>
	 * This constructor should not be used directly in application code. It exists
	 * solely to allow frameworks such as JPA to instantiate the entity.
	 * </p>
	 */
	protected ApplicationSettings() {
	}

	/**
	 * Creates a global configuration with a non-negative modification window and a
	 * non-null stored time zone.
	 *
	 * @param  daysUntilLocked                   number of days a {@link TimeLog}
	 *                                           can be modified; must be greater
	 *                                           than or equal to {@code 0}
	 * @param  employeeWorksiteCreationAllowed   whether employees are allowed to
	 *                                           create their own worksite
	 * @param  worksiteChangeDuringShiftAllowed  whether employees are allowed to
	 *                                           change their worksite during an
	 *                                           active shift.
	 * @param  employeeManualTimeLogEntryAllowed whether employees are allowed to
	 *                                           create manual timelog entries with
	 *                                           explicit timestamps.
	 * @param  defaultTimezone                   default application time zone.
	 * @throws IllegalArgumentException          if {@code daysUntilLocked} is
	 *                                           negative
	 * @throws NullPointerException              if defaultTimezone is {@code null}
	 */
	public ApplicationSettings(
		final int daysUntilLocked,
		final boolean employeeWorksiteCreationAllowed,
		final boolean worksiteChangeDuringShiftAllowed,
		final boolean employeeManualTimeLogEntryAllowed,
		final ZoneId defaultTimezone) {
		this.update(
				daysUntilLocked,
				employeeWorksiteCreationAllowed,
				worksiteChangeDuringShiftAllowed,
				employeeManualTimeLogEntryAllowed,
				defaultTimezone);
	}

	/**
	 * Returns the number of days during which a {@link TimeLog} remains modifiable
	 * before it becomes locked.
	 *
	 * @return the configured modification window in days; always greater than or
	 *         equal to {@code 0}
	 */
	public int getDaysUntilLocked() {
		return this.daysUntilLocked;
	}

	/**
	 * Returns the unique identifier of this entity.
	 *
	 * @return the fixed global identifier, {@code 1}
	 */
	public Long getId() {
		return this.id;
	}

	/**
	 * Indicates whether employees are allowed to create their own worksite.
	 *
	 * @return {@code true} if personal worksite creation by employees is allowed;
	 *         {@code false} otherwise
	 */
	public boolean isEmployeeWorksiteCreationAllowed() {
		return this.employeeWorksiteCreationAllowed;
	}

	/**
	 * Indicates whether employees are allowed to change their worksite during an
	 * active shift.
	 *
	 * @return {@code true} if worksite changes during a shift are allowed;
	 *         {@code false} otherwise
	 */
	public boolean isWorksiteChangeDuringShiftAllowed() {
		return this.worksiteChangeDuringShiftAllowed;
	}

	/**
	 * Indicates whether employees are allowed to provide custom timestamps when
	 * creating or updating time logs.
	 *
	 * @return {@code true} if manual timelog entry is allowed; {@code false}
	 *         otherwise
	 */
	public boolean isEmployeeManualTimeLogEntryAllowed() {
		return this.employeeManualTimeLogEntryAllowed;
	}

	/**
	 * Returns the stored global time zone; does not override worksite or account
	 * zones.
	 *
	 * @return the stored global time zone
	 */
	public ZoneId getDefaultTimezone() {
		return this.defaultTimezone;
	}

	/**
	 * Replaces all configuration values after validating the complete input.
	 *
	 * @param  daysUntilLocked                   non-negative modification window in
	 *                                           days
	 * @param  employeeWorksiteCreationAllowed   whether personal worksite creation
	 *                                           is allowed
	 * @param  worksiteChangeDuringShiftAllowed  whether clock-out at another
	 *                                           worksite is allowed
	 * @param  employeeManualTimeLogEntryAllowed whether manual timestamps are
	 *                                           allowed
	 * @param  defaultTimezone                   stored global time zone
	 * @throws IllegalArgumentException          if daysUntilLocked is negative
	 * @throws NullPointerException              if defaultTimezone is {@code null}
	 */
	void update(
			final int daysUntilLocked,
			final boolean employeeWorksiteCreationAllowed,
			final boolean worksiteChangeDuringShiftAllowed,
			final boolean employeeManualTimeLogEntryAllowed,
			final ZoneId defaultTimezone) {
		if (daysUntilLocked < 0) {
			throw new IllegalArgumentException("daysUntilLocked cannot be negative");
		}
		Objects.requireNonNull(defaultTimezone, "defaultTimezone cannot be null");
		this.daysUntilLocked = daysUntilLocked;
		this.employeeWorksiteCreationAllowed = employeeWorksiteCreationAllowed;
		this.worksiteChangeDuringShiftAllowed = worksiteChangeDuringShiftAllowed;
		this.employeeManualTimeLogEntryAllowed = employeeManualTimeLogEntryAllowed;
		this.defaultTimezone = defaultTimezone;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || this.getClass() != obj.getClass()) {
			return false;
		}
		final ApplicationSettings other = (ApplicationSettings) obj;
		return this.id != null && Objects.equals(this.id, other.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.id);
	}
}
