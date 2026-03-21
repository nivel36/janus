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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Entity that stores application-wide administrative configuration values.
 */
@Entity
@Table(name = "APPLICATION_SETTINGS")
public class ApplicationSettings implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@PositiveOrZero
	@Column(name = "DAYS_UNTIL_LOCKED", nullable = false)
	private int daysUntilLocked;

	ApplicationSettings() {
	}

	public ApplicationSettings(final int daysUntilLocked) {
		this.setDaysUntilLocked(daysUntilLocked);
	}

	public Long getId() {
		return this.id;
	}

	void setId(final Long id) {
		this.id = id;
	}

	public int getDaysUntilLocked() {
		return this.daysUntilLocked;
	}

	public void setDaysUntilLocked(final int daysUntilLocked) {
		if (daysUntilLocked < 0) {
			throw new IllegalArgumentException("daysUntilLocked cannot be negative");
		}
		this.daysUntilLocked = daysUntilLocked;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if ((obj == null) || (this.getClass() != obj.getClass())) {
			return false;
		}
		final ApplicationSettings other = (ApplicationSettings) obj;
		return (this.id != null) && Objects.equals(this.id, other.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.id);
	}
}
