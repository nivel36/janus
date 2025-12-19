/*
 * Copyright 2025 Abel Ferrer Jiménez
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
 * Exception thrown when attempting to modify a {@code TimeLog} record that was
 * deleted.
 */
public class TimeLogDeletedException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new exception with a default message.
	 */
	public TimeLogDeletedException() {
		super("The TimeLog record cannot be modified because it was deleted.");
	}

	/**
	 * Creates a new exception with a custom message.
	 *
	 * @param message the detail message
	 */
	public TimeLogDeletedException(String message) {
		super(message);
	}

	/**
	 * Creates a new exception with a custom message and cause.
	 *
	 * @param message the detail message
	 * @param cause   the cause of the exception
	 */
	public TimeLogDeletedException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Creates a new exception with full control over suppression and stack trace
	 * writability.
	 *
	 * @param message            the detail message explaining why the
	 *                           {@code TimeLog} modification is not allowed. Can be
	 *                           {@code null}.
	 * @param cause              the underlying cause of this exception. Can be
	 *                           {@code null}.
	 * @param enableSuppression  whether suppression is enabled or disabled.
	 * @param writableStackTrace whether the stack trace should be writable.
	 */
	public TimeLogDeletedException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
