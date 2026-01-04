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
package es.nivel36.janus.util;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utility for generating random salts.
 */
public final class SaltGenerator {

	private static final SecureRandom secureRandom = new SecureRandom();

	private SaltGenerator() {
	}

	/**
	 * Generates a Base64 URL-safe salt with the requested number of bytes.
	 *
	 * @param bytes number of random bytes to generate. Must be greater than zero.
	 *
	 * @return Base64 URL-safe salt string without padding.
	 *
	 * @throws IllegalArgumentException if {@code bytes} is less than 1.
	 */
	public static String generateBase64UrlSalt(final int bytes) {
		if (bytes < 1) {
			throw new IllegalArgumentException("bytes must be greater than zero.");
		}
		final byte[] salt = new byte[bytes];
		secureRandom.nextBytes(salt);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(salt);
	}
}
