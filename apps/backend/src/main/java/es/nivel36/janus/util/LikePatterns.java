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
package es.nivel36.janus.util;

/**
 * Utilities for safely constructing literal {@code LIKE} patterns.
 */
public final class LikePatterns {

	private static final String ESCAPE_CHARACTER = "!";

	private LikePatterns() {
	}

	/**
	 * Escapes characters interpreted specially by SQL {@code LIKE}. The matching
	 * query must declare {@code ESCAPE '!'}.
	 *
	 * @param  value literal text to escape; must not be {@code null}
	 * @return       text safe to place inside a parameterized {@code LIKE} pattern
	 */
	public static String escape(final String value) {
		return value.replace(ESCAPE_CHARACTER, ESCAPE_CHARACTER + ESCAPE_CHARACTER).replace("%", ESCAPE_CHARACTER + "%")
				.replace("_", ESCAPE_CHARACTER + "_");
	}
}
