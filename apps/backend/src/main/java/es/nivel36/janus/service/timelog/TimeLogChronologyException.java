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
package es.nivel36.janus.service.timelog;

/**
 * Exception thrown to indicate that a {@code TimeLog} entity has an invalid
 * chronological order between its {@code entryTime} and {@code exitTime}.
 * <p>
 * Typical scenarios where this exception may be raised:
 * <ul>
 * <li>When {@code entryTime} is set after {@code exitTime}.</li>
 * <li>When updating a {@code TimeLog} would result in an inconsistent
 * timeline.</li>
 * </ul>
 * <p>
 * This is an unchecked exception (subclass of {@link RuntimeException}) because
 * it usually represents a violation of business rules rather than a recoverable
 * condition.
 */
public class TimeLogChronologyException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new {@code TimeLogChronologyException} with no detail message or
	 * cause.
	 */
	public TimeLogChronologyException() {
		super("The TimeLog record cannot be modified or created because has an invalid chronological order.");
	}

	/**
	 * Creates a new {@code TimeLogChronologyException} with the specified detail
	 * message.
	 *
	 * @param message the detail message (saved for later retrieval by
	 *                {@link Throwable#getMessage()})
	 */
	public TimeLogChronologyException(final String message) {
		super(message);
	}

	/**
	 * Creates a new {@code TimeLogChronologyException} with the specified cause.
	 *
	 * @param cause the cause (saved for later retrieval by
	 *              {@link Throwable#getCause()}). A {@code null} value is
	 *              permitted.
	 */
	public TimeLogChronologyException(final Throwable cause) {
		super(cause);
	}

	/**
	 * Creates a new {@code TimeLogChronologyException} with the specified detail
	 * message and cause.
	 *
	 * @param message the detail message
	 * @param cause   the cause (a {@code null} value is permitted)
	 */
	public TimeLogChronologyException(final String message, final Throwable cause) {
		super(message, cause);
	}

	/**
	 * Creates a new {@code TimeLogChronologyException} with the specified detail
	 * message, cause, suppression enabled or disabled, and writable stack trace
	 * enabled or disabled.
	 *
	 * @param message            the detail message
	 * @param cause              the cause (a {@code null} value is permitted)
	 * @param enableSuppression  whether or not suppression is enabled
	 * @param writableStackTrace whether or not the stack trace should be writable
	 */
	public TimeLogChronologyException(final String message, final Throwable cause, final boolean enableSuppression,
			final boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
