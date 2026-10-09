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
package es.nivel36.janus.service;

/**
 * Signals that an operation conflicts with an existing resource or unique
 * attribute.
 */
public class ResourceAlreadyExistsException extends RuntimeException {

	private static final long serialVersionUID = -396426195015138435L;

	/**
	 * Constructs a new exception with {@code null} as its detail message.
	 */
	public ResourceAlreadyExistsException() {
	}

	/**
	 * Constructs a new exception with the specified detail message.
	 *
	 * @param message the detail message providing more information about the cause
	 */
	public ResourceAlreadyExistsException(final String message) {
		super(message);
	}

	/**
	 * Constructs a new exception with the specified cause.
	 *
	 * @param cause the underlying cause of this exception; may be {@code null}
	 */
	public ResourceAlreadyExistsException(final Throwable cause) {
		super(cause);
	}

	/**
	 * Constructs a new exception with the specified detail message and cause.
	 *
	 * @param message the detail message providing more information about the cause
	 * @param cause   the underlying cause of this exception; may be {@code null}
	 */
	public ResourceAlreadyExistsException(final String message, final Throwable cause) {
		super(message, cause);
	}

	/**
	 * Constructs a new exception with full control over suppression and stack trace
	 * writability.
	 *
	 * @param message            the detail message providing more information about
	 *                           the cause
	 * @param cause              the underlying cause of this exception; may be
	 *                           {@code null}
	 * @param enableSuppression  whether suppression is enabled or disabled
	 * @param writableStackTrace whether the stack trace should be writable
	 */
	public ResourceAlreadyExistsException(
		final String message,
		final Throwable cause,
		final boolean enableSuppression,
		final boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
