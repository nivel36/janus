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

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository class for managing {@link AppUser} entities.
 */
@Repository
interface AppUserRepository extends CrudRepository<AppUser, Long> {

	/**
	 * Checks whether a {@link AppUser} exists for the specified username.
	 *
	 * @param usermane the username to check for
	 * @return {@code true} if the application user with the specified username
	 *         exists, {@code false} otherwise.
	 */
	boolean existsByAccountUsername(final String username);

	/**
	 * Finds an {@link AppUser} by username.
	 *
	 * @param username the username of the employee to find
	 * @return the application user with the specified username, or {@code null} if
	 *         no user is found
	 */
	AppUser findByAccountUsername(final String username);
}
