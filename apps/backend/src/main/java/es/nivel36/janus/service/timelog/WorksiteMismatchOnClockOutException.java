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
 * distributed under this License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package es.nivel36.janus.service.timelog;

import es.nivel36.janus.service.worksite.Worksite;

/**
 * Exception thrown when a mismatch occurs between the expected {@link Worksite}
 * and the actual {@link Worksite} during a clock-out operation.
 *
 * <p>
 * This exception is typically used to enforce consistency by ensuring that an
 * employee clocks out from the same worksite where they originally clocked in.
 * If the worksites do not match, this exception is raised containing both
 * values for further handling or logging.
 * </p>
 */
public class WorksiteMismatchOnClockOutException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * The expected {@link Worksite} where the clock-out operation should occur.
	 * Can't be {@code null}.
	 */
	private final Worksite expected;

	/**
	 * The actual {@link Worksite} where the clock-out operation was attempted.
	 * Can't be {@code null}.
	 */
	private final Worksite actual;

	/**
	 * Constructs a new {@code WorksiteMismatchOnClockOutException} with the
	 * specified expected and actual worksites.
	 *
	 * @param expected the expected {@link Worksite} where the user should clock out
	 * @param actual   the actual {@link Worksite} where the user attempted to clock
	 *                 out
	 */
	public WorksiteMismatchOnClockOutException(final Worksite expected, final Worksite actual) {
		this.expected = expected;
		this.actual = actual;
	}

	/**
	 * Returns the expected {@link Worksite} where the clock-out operation should
	 * occur.
	 *
	 * @return the expected {@link Worksite}, never {@code null}
	 */
	public Worksite getExpected() {
		return this.expected;
	}

	/**
	 * Returns the actual {@link Worksite} where the clock-out operation was
	 * attempted.
	 *
	 * @return the actual {@link Worksite}, never {@code null}
	 */
	public Worksite getActual() {
		return this.actual;
	}
}
