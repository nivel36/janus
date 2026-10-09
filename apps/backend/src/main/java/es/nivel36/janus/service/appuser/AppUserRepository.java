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

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import es.nivel36.janus.service.employee.Employee;

/**
 * Internal persistence contract for application profiles.
 */
@Repository
interface AppUserRepository extends JpaRepository<AppUser, UUID> {

	/**
	 * Queries profiles using an already escaped literal email fragment. No profile
	 * or association is changed. Email matches case-insensitively and filters
	 * combine with AND; empty email disables the email restriction.
	 *
	 * @param  emailFilter    nonnull fragment escaped with LikePatterns.escape for
	 *                        SQL LIKE
	 * @param  employeeNumber exact validated number, or {@code null} to disable
	 *                        this filter
	 * @param  pageable       nonnull paging with entity property paths for sorting
	 * @return                matching page with employee associations loaded,
	 *                        possibly empty
	 */
	@EntityGraph(attributePaths = "employee")
	@Query("""
			SELECT u FROM AppUser u LEFT JOIN u.employee e
			WHERE LOWER(u.email) LIKE LOWER(CONCAT('%', :emailFilter, '%')) ESCAPE '!'
			AND (:employeeNumber IS NULL OR e.employeeNumber = :employeeNumber)
			""")
	Page<AppUser> search(String emailFilter, String employeeNumber, Pageable pageable);

	/**
	 * Looks up an exact subject without creating or changing a profile.
	 *
	 * @param  keycloakSubject nonnull validated opaque subject
	 * @return                 matching profile with employee loaded, or an empty
	 *                         optional
	 */
	@EntityGraph(attributePaths = "employee")
	Optional<AppUser> findByKeycloakSubject(String keycloakSubject);

	/**
	 * {@inheritDoc} The optional employee association is loaded with the returned
	 * profile.
	 */
	@Override
	@EntityGraph(attributePaths = "employee")
	Optional<AppUser> findById(UUID id);

	/**
	 * Looks up the profile owning an employee without changing its association. The
	 * employee must be nonnull and have a persistence identifier.
	 *
	 * @param  employee persisted employee whose owner is requested
	 * @return          owning profile, or an empty optional when the employee is
	 *                  unlinked
	 */
	Optional<AppUser> findByEmployee(Employee employee);

}
