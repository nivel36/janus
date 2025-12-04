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
package es.nivel36.janus.service.appuser;

import java.io.Serializable;
import java.util.Locale;
import java.util.Objects;

import org.hibernate.annotations.NaturalId;

import es.nivel36.janus.service.TimeFormat;
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
 * <p>
 * This entity stores basic identification and preference data for a user,
 * including their username, personal information, locale settings, and
 * preferred time format.
 * </p>
 *
 * <p>
 * Each user is uniquely identified by their {@code username}, which acts as a
 * natural ID and cannot be modified once created. The {@link Locale} and
 * {@link TimeFormat} fields define user-specific configuration preferences that
 * influence the behavior and presentation layer of the application.
 * </p>
 */
@Entity
public class AppUser implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * Unique identifier for the user. Auto-generated.
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * Unique username used to identify the user.
	 * <p>
	 * This field is mandatory, immutable once persisted, and must be unique across
	 * all users.
	 * </p>
	 */
	@NaturalId
	@NotEmpty
	@Column(updatable = false)
	private String username;

	/**
	 * The first name of the user.
	 * <p>
	 * This field is mandatory and cannot be empty.
	 * </p>
	 */
	@NotEmpty
	private String name;

	/**
	 * The surname of the user.
	 * <p>
	 * This field is mandatory and cannot be empty.
	 * </p>
	 */
	@NotEmpty
	private String surname;

	/**
	 * Preferred locale for this user.
	 * <p>
	 * Determines language and regional formatting rules used by the system when
	 * interacting with this user. This field is mandatory.
	 * </p>
	 */
	@NotNull
	private Locale locale;

	/**
	 * Preferred time format for the user.
	 * <p>
	 * This enum controls how time values are displayed (e.g., 24-hour vs 12-hour
	 * format). This field is mandatory.
	 * </p>
	 */
	@NotNull
	@Enumerated(EnumType.STRING)
	private TimeFormat timeFormat;

	/**
	 * Default constructor for JPA. Initializes an empty application user.
	 */
	public AppUser() {
	}

	/**
	 * Constructs a new application user with the specified username, name, surname,
	 * locale and timeFormat.
	 *
	 * @param username   the unique username of the application user
	 * @param name       the name of the application user
	 * @param surname    the surname of the application user
	 * @param locale     the preferred locale of the application user
	 * @param timeFormat the preferred time format of the application user
	 */
	public AppUser(final String username, final String name, final String surname, final Locale locale,
			final TimeFormat timeFormat) {
		this.username = username;
		this.name = name;
		this.surname = surname;
		this.locale = locale;
		this.timeFormat = timeFormat;
	}

	/**
	 * Gets the unique identifier of the user.
	 *
	 * @return the ID of the user
	 */
	public Long getId() {
		return id;
	}

	/**
	 * Sets the unique identifier of the user.
	 *
	 * @param id the ID to assign to the user
	 */
	public void setId(final Long id) {
		this.id = id;
	}

	/**
	 * Gets the username of the user.
	 *
	 * @return the username of the user
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * Sets the username of the user.
	 * <p>
	 * Note: this field is immutable once persisted and should not be modified in
	 * normal application flows.
	 * </p>
	 *
	 * @param username the username to assign
	 */
	public void setUsername(final String username) {
		this.username = username;
	}

	/**
	 * Gets the first name of the user.
	 *
	 * @return the first name of the user
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the first name of the user.
	 *
	 * @param name the first name to assign
	 */
	public void setName(final String name) {
		this.name = name;
	}

	/**
	 * Gets the surname of the user.
	 *
	 * @return the surname of the user
	 */
	public String getSurname() {
		return surname;
	}

	/**
	 * Sets the surname of the user.
	 *
	 * @param surname the surname to assign
	 */
	public void setSurname(final String surname) {
		this.surname = surname;
	}

	/**
	 * Gets the preferred locale of the user.
	 *
	 * @return the {@link Locale} used by this user
	 */
	public Locale getLocale() {
		return locale;
	}

	/**
	 * Sets the preferred locale for the user.
	 *
	 * @param locale the {@link Locale} to assign
	 */
	public void setLocale(final Locale locale) {
		this.locale = locale;
	}

	/**
	 * Gets the time format preference of the user.
	 *
	 * @return the {@link TimeFormat} configured for this user
	 */
	public TimeFormat getTimeFormat() {
		return timeFormat;
	}

	/**
	 * Sets the preferred time format for the user.
	 *
	 * @param timeFormat the {@link TimeFormat} to assign
	 */
	public void setTimeFormat(final TimeFormat timeFormat) {
		this.timeFormat = timeFormat;
	}

	/**
	 * Determines whether two users are equal by comparing their usernames.
	 *
	 * @param obj the object to compare
	 * @return {@code true} if both objects represent the same user
	 */
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

	/**
	 * Computes the hash code of this user based on its username.
	 *
	 * @return the hash code of the user
	 */
	@Override
	public int hashCode() {
		return Objects.hash(this.username);
	}

	/**
	 * Returns a string representation of this user.
	 *
	 * @return the username of the user
	 */
	@Override
	public String toString() {
		return this.username;
	}
}
