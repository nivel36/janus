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
package es.nivel36.janus.service.auth;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

import org.hibernate.annotations.NaturalId;

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
 * Entity representing an authentication account within the Janus system.
 */
@Entity
public class Account implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * Surrogate database identifier for the account.
	 * <p>
	 * This value is auto-generated and has no business meaning.
	 * </p>
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * Unique username used to identify the account.
	 *
	 * <p>
	 * Acts as a natural identifier. This field is mandatory, must be unique, and
	 * cannot be updated once the entity is persisted.
	 * </p>
	 */
	@NaturalId
	@NotEmpty
	@Column(updatable = false)
	private String username;

	/**
	 * Hashed password for the account.
	 *
	 * <p>
	 * This value should never store plain text passwords.
	 * </p>
	 */
	@NotEmpty
	private String password;

	/**
	 * Role assigned to the account.
	 */
	@NotNull
	@Enumerated(EnumType.STRING)
	private Role role;

	/**
	 * The instant at which the user last logged in.
	 */
	private Instant lastLogin;

	/**
	 * Protected no-argument constructor required by persistence frameworks.
	 *
	 * <p>
	 * This constructor should not be used directly in application code. It exists
	 * solely to allow frameworks such as JPA to instantiate the entity.
	 * </p>
	 */
	Account() {
	}

	/**
	 * Creates a new account with the provided credentials and role.
	 *
	 * @param username the unique username of the account. Can't be {@code null} or
	 *                 blank.
	 * @param password the hashed password of the account. Can't be {@code null} or
	 *                 blank.
	 * @param role     the assigned {@link Role}. Can't be {@code null}.
	 *
	 * @throws NullPointerException     if {@code role} is {@code null}
	 * @throws IllegalArgumentException if {@code username} or {@code password} is
	 *                                  blank
	 */
	public Account(final String username, final String password, final Role role) {
		this.username = Strings.requireNonBlank(username, "username can't be null or blank");
		this.password = Strings.requireNonBlank(password, "password can't be null or blank");
		this.role = Objects.requireNonNull(role, "role can't be null");
	}

	/**
	 * Returns the database identifier of the account.
	 *
	 * @return the account ID, or {@code null} if the entity has not been persisted yet
	 */
	public Long getId() {
		return id;
	}

	/**
	 * Assigns the database identifier of the account.
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

	/**
	 * Returns the username of the account.
	 *
	 * @return the unique username
	 */
	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	void setPassword(final String password) {
		this.password = Strings.requireNonBlank(password, "password can't be null or blank");
	}

	/**
	 * Returns the role assigned to the account.
	 *
	 * @return the {@link Role}
	 */
	public Role getRole() {
		return role;
	}

	/**
	 * Updates the role assigned to the account.
	 *
	 * @param role the new {@link Role}. Can't be {@code null}.
	 *
	 * @throws NullPointerException if {@code role} is {@code null}
	 */
	public void setRole(final Role role) {
		this.role = Objects.requireNonNull(role, "role can't be null");
	}

	/**
	 * Returns the instant when the account last logged in.
	 *
	 * @return the last login instant, or {@code null} if the account has never logged in
	 */
	public Instant getLastLogin() {
		return lastLogin;
	}

	/**
	 * Records a login event for the account.
	 *
	 * @param lastLogin the login instant to record. Can't be {@code null}.
	 *
	 * @throws NullPointerException if {@code lastLogin} is {@code null}
	 */
	public void recordLogin(final Instant lastLogin) {
		this.lastLogin = Objects.requireNonNull(lastLogin, "lastLogin can't be null");
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if ((obj == null) || (this.getClass() != obj.getClass())) {
			return false;
		}
		final Account other = (Account) obj;
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
