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
package es.nivel36.janus.service.appuser;

import java.io.Serializable;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.UuidGenerator;

import es.nivel36.janus.service.TimeFormat;
import es.nivel36.janus.service.employee.Employee;
import es.nivel36.janus.util.EmailAddresses;
import es.nivel36.janus.util.Strings;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Local application profile with an immutable provider subject and mutable
 * contact information, preferences and optional employee association. Public
 * constructors require valid nonnull identity/contact values and preferences. A
 * new profile is unpersisted, unlinked and uses DARK theme. Persistence
 * enforces one profile per subject and at most one per employee; email is not
 * unique. Association changes synchronize both entity references but callers
 * must arrange authorization and transactional persistence.
 */
@Entity
public class AppUser implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * Timezone used by the convenience constructor when none is supplied.
	 */
	public static final ZoneId DEFAULT_TIMEZONE = ZoneId.of("UTC");

	/**
	 * Persistence-generated profile UUID; null before assignment.
	 */
	@Id
	@GeneratedValue
	@UuidGenerator
	private UUID id;

	/**
	 * Normalized mutable contact email; nonblank and not a unique identity key.
	 */
	@NotBlank
	@Email
	private String email;

	/**
	 * Immutable nonblank provider subject, unique within the configured issuer.
	 */
	@NaturalId
	@NotBlank
	@Size(max = 255)
	@Column(updatable = false, unique = true)
	private String keycloakSubject;

	/**
	 * Nonnull preferred locale once constructed or hydrated.
	 */
	@NotNull
	private Locale locale;

	/**
	 * Nonnull preferred time display format once constructed or hydrated.
	 */
	@NotNull
	@Enumerated(EnumType.STRING)
	private TimeFormat timeFormat;

	/**
	 * Nonnull preferred color scheme, initially DARK.
	 */
	@NotNull
	@Enumerated(EnumType.STRING)
	private Theme theme = Theme.DARK;

	/**
	 * Nonnull preferred timezone once constructed or hydrated.
	 */
	@NotNull
	private ZoneId defaultTimezone;

	/**
	 * Optional one-to-one employee association; may require lazy initialization.
	 */
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "employee_id")
	private Employee employee;

	/**
	 * Creates the empty persistence shell required by JPA. Identity, preferences
	 * and associations must be hydrated before normal use; construction performs no
	 * persistence or input validation.
	 */
	AppUser() {
	}

	/**
	 * Creates an unpersisted, unlinked DARK profile with UTC timezone. All
	 * arguments must be nonnull; email and subject must be nonblank and at most 255
	 * characters after email normalization. Subject is opaque and retained exactly.
	 *
	 * @param  email                    contact email to trim and lowercase
	 * @param  keycloakSubject          immutable provider subject
	 * @param  locale                   initial locale
	 * @param  timeFormat               initial time display format
	 * @throws NullPointerException     if any argument is null
	 * @throws IllegalArgumentException if email or subject is blank or oversized
	 */
	public AppUser(final String email, final String keycloakSubject, final Locale locale, final TimeFormat timeFormat) {
		this(email, keycloakSubject, locale, timeFormat, DEFAULT_TIMEZONE);
	}

	/**
	 * Creates an unpersisted, unlinked DARK profile with the supplied preferences.
	 * All arguments must be nonnull; contact email and subject must be nonblank and
	 * at most 255 characters after email normalization. This constructor does not
	 * reserve the subject or employee in the database.
	 *
	 * @param  email                    contact email to trim and lowercase
	 * @param  keycloakSubject          opaque immutable subject retained exactly as
	 *                                  supplied
	 * @param  locale                   initial locale
	 * @param  timeFormat               initial time display format
	 * @param  defaultTimezone          initial timezone
	 * @throws NullPointerException     if any argument is null
	 * @throws IllegalArgumentException if email or subject is blank or oversized
	 */
	public AppUser(
		final String email,
		final String keycloakSubject,
		final Locale locale,
		final TimeFormat timeFormat,
		final ZoneId defaultTimezone) {
		this.email = validateEmail(email);
		this.keycloakSubject = validateKeycloakSubject(keycloakSubject);
		this.locale = Objects.requireNonNull(locale, "locale can't be null");
		this.timeFormat = Objects.requireNonNull(timeFormat, "timeFormat can't be null");
		this.defaultTimezone = Objects.requireNonNull(defaultTimezone, "defaultTimezone can't be null or blank");
	}

	/**
	 * Returns the profile's persistence identifier without changing the profile.
	 *
	 * @return generated UUID, or null before persistence assigns it
	 */
	public UUID getId() {
		return this.id;
	}

	/**
	 * Assigns an identifier for package-internal hydration or test setup. This
	 * method does not persist the profile or check identifier uniqueness.
	 *
	 * @param id assigned UUID, or null to represent an unpersisted profile
	 */
	void setId(final UUID id) {
		this.id = id;
	}

	/**
	 * Returns normalized contact information without changing the profile.
	 *
	 * @return nonblank trimmed, lowercase email; uniqueness is not guaranteed
	 */
	public String getEmail() {
		return this.email;
	}

	/**
	 * Replaces contact information after normalization without changing identity.
	 * The input must be nonnull, nonblank and at most 255 normalized characters.
	 * Invalid input leaves the previous email unchanged.
	 *
	 * @param  email                    contact email to trim and lowercase
	 * @throws NullPointerException     if email is null
	 * @throws IllegalArgumentException if normalized email is blank or oversized
	 */
	void setEmail(final String email) {
		this.email = validateEmail(email);
	}

	/**
	 * Returns the immutable identity key without changing the profile.
	 *
	 * @return nonblank opaque subject scoped to the configured issuer
	 */
	public String getKeycloakSubject() {
		return this.keycloakSubject;
	}

	/**
	 * Checks the opaque subject without trimming it or changing a profile.
	 *
	 * @param  keycloakSubject          nonnull, nonblank subject of at most 255
	 *                                  characters
	 * @return                          the exact supplied subject
	 * @throws NullPointerException     if subject is null
	 * @throws IllegalArgumentException if subject is blank or oversized
	 */
	static String validateKeycloakSubject(final String keycloakSubject) {
		final String subject = Strings.requireNonBlank(keycloakSubject, "keycloakSubject can't be null or blank");
		if (subject.length() > 255) {
			throw new IllegalArgumentException("keycloakSubject can't exceed 255 characters");
		}
		return subject;
	}

	/**
	 * Normalizes and checks contact information without changing a profile.
	 *
	 * @param  email                    nonnull, nonblank email to trim and
	 *                                  lowercase
	 * @return                          normalized nonblank email of at most 255
	 *                                  characters
	 * @throws NullPointerException     if email is null
	 * @throws IllegalArgumentException if normalized email is blank or oversized
	 */
	static String validateEmail(final String email) {
		final String normalized = EmailAddresses.canonicalize(email);
		if (normalized.length() > 255) {
			throw new IllegalArgumentException("email can't exceed 255 characters");
		}
		return normalized;
	}

	/**
	 * Validates all preference values before changing any of them. All arguments
	 * must be nonnull. On success all preferences are replaced while identity,
	 * contact email and association are preserved; invalid input leaves all
	 * existing values unchanged. Persistence remains the caller's responsibility.
	 *
	 * @param  locale               replacement locale
	 * @param  timeFormat           replacement time display format
	 * @param  defaultTimezone      replacement timezone
	 * @param  theme                replacement color theme
	 * @throws NullPointerException if any argument is null
	 */
	void updatePreferences(
			final Locale locale,
			final TimeFormat timeFormat,
			final ZoneId defaultTimezone,
			final Theme theme) {
		Objects.requireNonNull(locale, "locale can't be null");
		Objects.requireNonNull(timeFormat, "timeFormat can't be null");
		Objects.requireNonNull(defaultTimezone, "defaultTimezone can't be null");
		Objects.requireNonNull(theme, "theme can't be null");
		this.setLocale(locale);
		this.setTimeFormat(timeFormat);
		this.setDefaultTimezone(defaultTimezone);
		this.setTheme(theme);
	}

	/**
	 * Returns the current locale without changing preferences.
	 *
	 * @return nonnull preferred locale
	 */
	public Locale getLocale() {
		return this.locale;
	}

	/**
	 * Replaces the locale without changing other profile attributes. The value must
	 * be nonnull; invalid input leaves the previous value unchanged. This method
	 * does not write to the database.
	 *
	 * @param  locale               nonnull replacement locale
	 * @throws NullPointerException if locale is null
	 */
	void setLocale(final Locale locale) {
		this.locale = Objects.requireNonNull(locale, "locale can't be null");
	}

	/**
	 * Returns the current time display preference without changing it.
	 *
	 * @return nonnull preferred time format
	 */
	public TimeFormat getTimeFormat() {
		return this.timeFormat;
	}

	/**
	 * Replaces the time display format without changing other profile attributes.
	 * The value must be nonnull; invalid input leaves the previous value unchanged.
	 * This method does not write to the database.
	 *
	 * @param  timeFormat           nonnull replacement time display format
	 * @throws NullPointerException if timeFormat is null
	 */
	void setTimeFormat(final TimeFormat timeFormat) {
		this.timeFormat = Objects.requireNonNull(timeFormat, "timeFormat can't be null");
	}

	/**
	 * Returns the current color preference without changing it.
	 *
	 * @return nonnull preferred theme
	 */
	public Theme getTheme() {
		return this.theme;
	}

	/**
	 * Replaces the color theme without changing other profile attributes. The value
	 * must be nonnull; invalid input leaves the previous value unchanged. This
	 * method does not write to the database.
	 *
	 * @param  theme                nonnull replacement color theme
	 * @throws NullPointerException if theme is null
	 */
	void setTheme(final Theme theme) {
		this.theme = Objects.requireNonNull(theme, "theme can't be null");
	}

	/**
	 * Returns the current timezone preference without changing it.
	 *
	 * @return nonnull preferred timezone
	 */
	public ZoneId getDefaultTimezone() {
		return this.defaultTimezone;
	}

	/**
	 * Replaces the timezone without changing other profile attributes. The value
	 * must be nonnull; invalid input leaves the previous value unchanged. This
	 * method does not write to the database.
	 *
	 * @param  defaultTimezone      nonnull replacement timezone
	 * @throws NullPointerException if defaultTimezone is null
	 */
	void setDefaultTimezone(final ZoneId defaultTimezone) {
		this.defaultTimezone = Objects.requireNonNull(defaultTimezone, "defaultTimezone can't be null or blank");
	}

	/**
	 * Returns the optional employee association without changing it. Callers
	 * accessing a lazy association must have an initialized association or an
	 * active persistence context.
	 *
	 * @return associated employee, or null when unlinked
	 */
	public Employee getEmployee() {
		return this.employee;
	}

	/**
	 * Changes the employee association and synchronizes both entity references.
	 * <p>
	 * The caller must authorize the change and ensure that the employee is
	 * available for association. This entity method does not check database
	 * uniqueness and may initialize lazy relationships.
	 * </p>
	 * <p>
	 * The previous employee no longer references this profile; the supplied
	 * employee references it. If that employee referenced another profile, that
	 * profile is detached from the employee. Passing the current employee is a
	 * no-op. No database write occurs until the caller persists or flushes the
	 * enclosing transaction.
	 * </p>
	 *
	 * @param employee employee to associate, or null to clear the association
	 */
	public void setEmployee(final Employee employee) {
		if (this.employee == employee) {
			return;
		}
		final Employee previousEmployee = this.employee;
		this.employee = employee;
		if (previousEmployee != null && previousEmployee.getAppUser() == this) {
			previousEmployee.setAppUser(null);
		}
		if (employee != null && employee.getAppUser() != this) {
			employee.setAppUser(this);
		}
	}

	/**
	 * Compares persisted identity without changing either profile.
	 *
	 * @param  obj any object, including null
	 * @return     true for the same instance, or an AppUser of the same runtime
	 *             class with the same nonnull UUID; distinct unpersisted profiles
	 *             are unequal
	 */
	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || this.getClass() != obj.getClass()) {
			return false;
		}
		final AppUser other = (AppUser) obj;
		return this.id != null && Objects.equals(this.id, other.id);
	}

	/**
	 * Returns the identity-based hash without changing the profile.
	 *
	 * @return UUID hash when assigned, otherwise this instance's identity hash; the
	 *         value can change when persistence assigns the UUID
	 */
	@Override
	public int hashCode() {
		return this.id == null ? System.identityHashCode(this) : this.id.hashCode();
	}

	/**
	 * Returns the contact email as a diagnostic representation.
	 *
	 * @return current email; this mutable value is not an identity key
	 */
	@Override
	public String toString() {
		return this.email;
	}
}
