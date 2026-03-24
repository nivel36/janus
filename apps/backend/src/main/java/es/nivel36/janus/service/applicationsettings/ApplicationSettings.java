/*
 * Copyright 2026 Abel Ferrer Jiménez
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
package es.nivel36.janus.service.applicationsettings;

import java.io.Serializable;
import java.util.Objects;

import es.nivel36.janus.service.timelog.TimeLog;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Entity that stores application-wide administrative configuration values.
 *
 * <p>
 * This class is mapped to the {@code APPLICATION_SETTINGS} table and represents
 * global configuration parameters that affect the behavior of the application.
 * These settings are typically unique within the system and are used to control
 * administrative rules such as modification limits and feature enablement.
 * </p>
 */
@Entity
@Table(name = "APPLICATION_SETTINGS")
public class ApplicationSettings implements Serializable {

	private static final long serialVersionUID = 1L;
	public static final Long GLOBAL_SETTINGS_ID = 1L;

	/**
	 * Unique identifier of the application settings entity.
	 * <p>
	 * As there can only be one setting per app and it cannot be created or deleted,
	 * the ID value is fixed at 1
	 * </p>
	 */
	@Id
	private Long id = GLOBAL_SETTINGS_ID;

	/**
	 * Number of days during which a {@link TimeLog} remains modifiable.
	 * <p>
	 * Must be greater than or equal to {@code 0}. After this period, the
	 * {@link TimeLog} becomes locked and cannot be modified.
	 * </p>
	 */
	@PositiveOrZero
	private int daysUntilLocked;

	/**
	 * Indicates whether employees are allowed to create their own workplace.
	 * <p>
	 * When {@code true}, employees can create workplace entries. When
	 * {@code false}, this action is restricted.
	 * </p>
	 */
	private boolean employeeWorkplaceCreationAllowed;

	/**
	 * Indicates whether employees are allowed to change their worksite during an
	 * active shift.
	 * <p>
	 * When {@code true}, worksite changes during a shift are permitted. When
	 * {@code false}, employees must remain in the same worksite for the duration of
	 * the shift.
	 * </p>
	 */
	private boolean worksiteChangeDuringShiftAllowed;

	/**
	 * Protected no-argument constructor required by persistence frameworks.
	 *
	 * <p>
	 * This constructor should not be used directly in application code. It exists
	 * solely to allow frameworks such as JPA to instantiate the entity.
	 * </p>
	 */
	ApplicationSettings() {
	}

	/**
	 * Creates a new instance with the specified modification window.
	 *
	 * @param daysUntilLocked                  number of days a {@link TimeLog} can
	 *                                         be modified; must be greater than or
	 *                                         equal to {@code 0}
	 * @param employeeWorkplaceCreationAllowed whether employees are allowed to
	 *                                         create their own workplace
	 * @param worksiteChangeDuringShiftAllowed whether employees are allowed to
	 *                                         change their worksite during an
	 *                                         active shift.
	 * @throws IllegalArgumentException if {@code daysUntilLocked} is negative
	 */
	public ApplicationSettings(final int daysUntilLocked, final boolean employeeWorkplaceCreationAllowed,
			final boolean worksiteChangeDuringShiftAllowed) {
		this.setDaysUntilLocked(daysUntilLocked);
		this.employeeWorkplaceCreationAllowed = employeeWorkplaceCreationAllowed;
		this.worksiteChangeDuringShiftAllowed = worksiteChangeDuringShiftAllowed;
	}

	/**
	 * Returns the number of days during which a {@link TimeLog} remains modifiable
	 * before it becomes locked.
	 *
	 * @return the number of days left until the {@link TimeLog} can no longer be
	 *         modified; always greater than or equal to {@code 0}
	 */
	public int getDaysUntilLocked() {
		return this.daysUntilLocked;
	}

	/**
	 * Returns the unique identifier of this entity.
	 *
	 * @return the identifier, or {@code null} if the entity has not yet been
	 *         persisted
	 */
	public Long getId() {
		return this.id;
	}

	/**
	 * Indicates whether employees are allowed to create their own workplace.
	 *
	 * @return {@code true} if workplace creation by employees is allowed;
	 *         {@code false} otherwise
	 */
	public boolean isEmployeeWorkplaceCreationAllowed() {
		return this.employeeWorkplaceCreationAllowed;
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
	 * Sets the identifier of this application settings.
	 * 
	 * <p>
	 * This method is intended for testing purposes only and should not be used in
	 * production code. It exists to allow controlled assignment of the identifier
	 * when creating or manipulating entity instances in tests.
	 * </p>
	 *
	 * @param id the identifier to assign
	 */
	void setId(Long id) {
		this.id = id;
	}

	/**
	 * Sets the number of days during which a {@link TimeLog} remains modifiable.
	 *
	 * @param daysUntilLocked number of days; must be greater than or equal to
	 *                        {@code 0}
	 * @throws IllegalArgumentException if {@code daysUntilLocked} is negative
	 */
	public void setDaysUntilLocked(final int daysUntilLocked) {
		if (daysUntilLocked < 0) {
			throw new IllegalArgumentException("daysUntilLocked cannot be negative");
		}
		this.daysUntilLocked = daysUntilLocked;
	}

	/**
	 * Sets whether employees are allowed to create their own workplace.
	 *
	 * @param employeeWorkplaceCreationAllowed {@code true} to allow workplace
	 *                                         creation by employees; {@code false}
	 *                                         to restrict it
	 */
	public void setEmployeeWorkplaceCreationAllowed(final boolean employeeWorkplaceCreationAllowed) {
		this.employeeWorkplaceCreationAllowed = employeeWorkplaceCreationAllowed;
	}

	/**
	 * Sets whether employees are allowed to change their worksite during an active
	 * shift.
	 *
	 * @param worksiteChangeDuringShiftAllowed {@code true} to allow worksite
	 *                                         changes during a shift; {@code false}
	 *                                         otherwise
	 */
	public void setWorksiteChangeDuringShiftAllowed(final boolean worksiteChangeDuringShiftAllowed) {
		this.worksiteChangeDuringShiftAllowed = worksiteChangeDuringShiftAllowed;
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
