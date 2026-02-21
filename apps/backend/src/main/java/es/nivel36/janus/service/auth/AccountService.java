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

import java.time.Instant;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.nivel36.janus.util.Strings;

/**
 * Service responsible for authentication against {@link Account} entities.
 */
@Service
public class AccountService {

	private static final Logger logger = LoggerFactory.getLogger(AccountService.class);

	private final AccountRepository accountRepository;
	private final PasswordEncoder passwordEncoder;

	public AccountService(final AccountRepository accountRepository, final PasswordEncoder passwordEncoder) {
		this.accountRepository = Objects.requireNonNull(accountRepository, "accountRepository cannot be null");
		this.passwordEncoder = Objects.requireNonNull(passwordEncoder, "passwordEncoder cannot be null");
	}

	@Transactional
	public Account login(final String username, final String password, final Instant loginInstant) {
		Strings.requireNonBlank(username, "username cannot be null or blank.");
		Strings.requireNonBlank(password, "password cannot be null or blank.");
		Objects.requireNonNull(loginInstant, "loginInstant cannot be null.");

		logger.debug("Authenticating Account {}", username);
		final Account account = this.accountRepository.findByUsername(username);
		if (account == null) {
			throw new AuthenticationFailedException("Invalid username or password.");
		}
		final boolean matches = this.passwordEncoder.matches(password, account.getPassword());
		if (!matches) {
			throw new AuthenticationFailedException("Invalid username or password.");
		}
		account.recordLogin(loginInstant);
		return this.accountRepository.save(account);
	}
}
