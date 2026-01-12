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
package es.nivel36.janus.service.account;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository class for managing {@link Account} entities.
 */
@Repository
interface AccountRepository extends CrudRepository<Account, Long> {

	/**
	 * Checks whether an {@link Account} exists for the specified username.
	 *
	 * @param username the username to check for
	 * @return {@code true} if the account with the specified username exists,
	 *         {@code false} otherwise.
	 */
	boolean existsByUsername(final String username);

	/**
	 * Finds an {@link Account} by username.
	 *
	 * @param username the username of the account to find
	 * @return the account with the specified username, or {@code null} if no account
	 *         is found
	 */
	Account findByUsername(final String username);
}
