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
 * Exception thrown when attempting to modify a {@code TimeLog} record that has
 * exceeded its allowed modification window.
 */
public class TimeLogModificationNotAllowedException extends RuntimeException {

	private static final long serialVersionUID = 3798698507343190506L;

	/**
	 * Creates a new exception with a default message.
	 */
	public TimeLogModificationNotAllowedException() {
		super("The TimeLog record cannot be modified because the modification window has expired.");
	}

	/**
	 * Creates a new exception with a custom message.
	 *
	 * @param message the detail message
	 */
	public TimeLogModificationNotAllowedException(String message) {
		super(message);
	}

	/**
	 * Creates a new exception with a custom message and cause.
	 *
	 * @param message the detail message
	 * @param cause   the cause of the exception
	 */
	public TimeLogModificationNotAllowedException(String message, Throwable cause) {
		super(message, cause);
	}
}
