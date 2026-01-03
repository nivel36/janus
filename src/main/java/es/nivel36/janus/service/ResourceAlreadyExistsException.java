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
package es.nivel36.janus.service;

/**
 * Exception thrown to indicate that a requested resource already exists.
 * <p>
 * This exception is a generic alternative to persistence-specific exceptions
 * (such as {@code EntityExistsException}) and can be used across different
 * layers of the application, including service and web layers, without
 * introducing dependencies on JPA or other persistence frameworks.
 * <p>
 * Typical use cases include scenarios where a creation attempt fails because a
 * resource with the same identifier or unique attribute already exists.
 *
 * <p>
 * <b>Example:</b>
 * </p>
 *
 * <pre>{@code
 * // Service layer example
 * public void registerEmployee(String email) {
 * 	if (employeeRepository.existsByEmail(email)) {
 * 		throw new ResourceAlreadyExistsException("Employee already exists: " + email);
 * 	}
 * 	employeeRepository.save(new Employee(email));
 * }
 * }</pre>
 *
 * @see RuntimeException
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
	public ResourceAlreadyExistsException(String message) {
		super(message);
	}

	/**
	 * Constructs a new exception with the specified cause.
	 *
	 * @param cause the underlying cause of this exception; may be {@code null}
	 */
	public ResourceAlreadyExistsException(Throwable cause) {
		super(cause);
	}

	/**
	 * Constructs a new exception with the specified detail message and cause.
	 *
	 * @param message the detail message providing more information about the cause
	 * @param cause   the underlying cause of this exception; may be {@code null}
	 */
	public ResourceAlreadyExistsException(String message, Throwable cause) {
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
	public ResourceAlreadyExistsException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
