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
package es.nivel36.janus.service.appuser;

import java.io.Serializable;
import java.util.Locale;
import java.util.Objects;

import org.hibernate.annotations.NaturalId;

import es.nivel36.janus.service.TimeFormat;
import es.nivel36.janus.util.Strings;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * Entity representing an application user within the Janus system.
 */
@Entity
public class AppUser implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NaturalId
	@NotEmpty
	@Column(updatable = false, unique = true)
	private String username;

	@NotNull
	private Locale locale;

	@NotNull
	@Enumerated(EnumType.STRING)
	private TimeFormat timeFormat;

	AppUser() {
	}

	public AppUser(final String username, final Locale locale, final TimeFormat timeFormat) {
		this.username = Strings.requireNonBlank(username, "username can't be null or blank");
		this.locale = Objects.requireNonNull(locale, "locale can't be null");
		this.timeFormat = Objects.requireNonNull(timeFormat, "timeFormat can't be null");
	}

	public Long getId() {
		return id;
	}

	void setId(final Long id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}

	public Locale getLocale() {
		return locale;
	}

	public void setLocale(final Locale locale) {
		this.locale = Objects.requireNonNull(locale, "locale can't be null");
	}

	public TimeFormat getTimeFormat() {
		return timeFormat;
	}

	public void setTimeFormat(final TimeFormat timeFormat) {
		this.timeFormat = Objects.requireNonNull(timeFormat, "timeFormat can't be null");
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if ((obj == null) || (this.getClass() != obj.getClass())) {
			return false;
		}
		final AppUser other = (AppUser) obj;
		return Objects.equals(this.username, other.username);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.username);
	}

	@Override
	public String toString() {
		return this.username;
	}
}
