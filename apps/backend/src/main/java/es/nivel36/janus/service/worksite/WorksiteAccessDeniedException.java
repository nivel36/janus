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
package es.nivel36.janus.service.worksite;

/**
 * Exception thrown when an employee attempts to use a worksite that is not
 * allowed by the worksite scope rules.
 */
public class WorksiteAccessDeniedException extends RuntimeException {

	private static final long serialVersionUID = 3744668086520000141L;

	/**
	 * Creates a new exception with a custom message.
	 *
	 * @param message the detail message
	 */
	public WorksiteAccessDeniedException(final String message) {
		super(message);
	}
}
