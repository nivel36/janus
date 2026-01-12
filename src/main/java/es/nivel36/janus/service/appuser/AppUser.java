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

import es.nivel36.janus.service.TimeFormat;
import es.nivel36.janus.service.account.Account;
import es.nivel36.janus.util.Strings;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * Entity representing an application user within the Janus system.
 *
 * <p>
 * This class models a user of the application, storing identification data and
 * configuration preferences such as personal information, locale, and preferred
 * time format.
 * </p>
 *
 * <p>
 * Each user is uniquely identified by its {@link Account}, which stores the
 * authentication credentials. User preferences like {@link Locale} and
 * {@link TimeFormat} define how information is presented to the user across the
 * system.
 * </p>
 */
@Entity
public class AppUser implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * Surrogate database identifier for the user.
	 * <p>
	 * This value is auto-generated and has no business meaning.
	 * </p>
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * The first name of the user.
	 *
	 * <p>
	 * This field is mandatory and must not be empty.
	 * </p>
	 */
	@NotEmpty
	private String name;

	/**
	 * The surname of the user.
	 *
	 * <p>
	 * This field is mandatory and must not be empty.
	 * </p>
	 */
	@NotEmpty
	private String surname;

	/**
	 * Authentication account associated with the user.
	 */
	@NotNull
	@OneToOne(optional = false, fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn(name = "account_id", updatable = false, unique = true)
	private Account account;

	/**
	 * Preferred locale of the user.
	 *
	 * <p>
	 * Determines language, regional settings, and formatting rules applied when
	 * interacting with this user. Can't be {@code null}.
	 * </p>
	 */
	@NotNull
	private Locale locale;

	/**
	 * Preferred time format of the user.
	 *
	 * <p>
	 * Defines how time values are displayed to the user (for example, 24-hour or
	 * 12-hour format). Can't be {@code null}.
	 * </p>
	 */
	@NotNull
	@Enumerated(EnumType.STRING)
	private TimeFormat timeFormat;

	/**
	 * Protected no-argument constructor required by persistence frameworks.
	 *
	 * <p>
	 * This constructor should not be used directly in application code. It exists
	 * solely to allow frameworks such as JPA to instantiate the entity.
	 * </p>
	 */
	AppUser() {
	}

	/**
	 * Creates a new application user with the provided personal data and
	 * preferences.
	 *
	 * @param account    the {@link Account} associated with the user. Can't be
	 *                   {@code null}.
	 * @param name       the first name of the user. Can't be {@code null} or blank.
	 * @param surname    the surname of the user. Can't be {@code null} or blank.
	 * @param locale     the preferred {@link Locale} of the user. Can't be
	 *                   {@code null}.
	 * @param timeFormat the preferred {@link TimeFormat} of the user. Can't be
	 *                   {@code null}.
	 *
	 * @throws NullPointerException     if any parameter is {@code null}
	 * @throws IllegalArgumentException if {@code name} or {@code surname} is blank
	 */
	public AppUser(final Account account, final String name, final String surname, final Locale locale,
			final TimeFormat timeFormat) {
		this.account = Objects.requireNonNull(account, "account can't be null");
		this.name = Strings.requireNonBlank(name, "name can't be null or blank");
		this.surname = Strings.requireNonBlank(surname, "surname can't be null or blank");
		this.locale = Objects.requireNonNull(locale, "locale can't be null");
		this.timeFormat = Objects.requireNonNull(timeFormat, "timeFormat can't be null");
	}

	/**
	 * Returns the database identifier of the user.
	 *
	 * @return the user ID, or {@code null} if the entity has not been persisted yet
	 */
	public Long getId() {
		return id;
	}

	/**
	 * Assigns the database identifier of the user.
	 *
	 * <p>
	 * This method is intended for testing purposes only and should not be used in
	 * production code. It exists to allow controlled assignment of the identifier
	 * when creating or manipulating entity instances in tests.
	 * </p>
	 *
	 * @param id the identifier to assign
	 */
	void setId(final Long id) {
		this.id = id;
	}

	public Account getAccount() {
		return account;
	}

	/**
	 * Returns the username of the account.
	 *
	 * @return the account username
	 */
	public String getUsername() {
		return account.getUsername();
	}

	/**
	 * Returns the first name of the user.
	 *
	 * @return the first name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Updates the full name of the user.
	 *
	 * @param name    the new first name. Can't be {@code null} or blank.
	 * @param surname the new surname. Can't be {@code null} or blank.
	 *
	 * @throws NullPointerException     if any parameter is {@code null}
	 * @throws IllegalArgumentException if {@code name} or {@code surname} is blank
	 */
	public void setFullName(final String name, final String surname) {
		this.name = Strings.requireNonBlank(name, "name can't be null or blank");
		this.surname = Strings.requireNonBlank(surname, "surname can't be null or blank");
	}

	/**
	 * Returns the surname of the user.
	 *
	 * @return the surname
	 */
	public String getSurname() {
		return surname;
	}

	/**
	 * Returns the preferred locale of the user.
	 *
	 * @return the {@link Locale} configured for this user
	 */
	public Locale getLocale() {
		return locale;
	}

	/**
	 * Sets the preferred locale of the user.
	 *
	 * @param locale the {@link Locale} to assign. Can't be {@code null}.
	 *
	 * @throws NullPointerException if {@code locale} is {@code null}
	 */
	public void setLocale(final Locale locale) {
		this.locale = Objects.requireNonNull(locale, "locale can't be null");
	}

	/**
	 * Returns the preferred time format of the user.
	 *
	 * @return the configured {@link TimeFormat}
	 */
	public TimeFormat getTimeFormat() {
		return timeFormat;
	}

	/**
	 * Sets the preferred time format of the user.
	 *
	 * @param timeFormat the {@link TimeFormat} to assign. Can't be {@code null}.
	 *
	 * @throws NullPointerException if {@code timeFormat} is {@code null}
	 */
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
		return Objects.equals(this.account, other.account);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.account);
	}

	@Override
	public String toString() {
		return this.account.getUsername();
	}
}
