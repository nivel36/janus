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
package es.nivel36.janus.service.appuser;

/**
 * Recognized Janus authorities granted by the trusted identity provider. A role
 * alone does not provision an actor or imply employee ownership; operation
 * policies combine these values with persisted profile context.
 */
public enum Role {
	/**
	 * Administrative role; resource policies define the permitted operations.
	 */
	JANUS_ADMIN,
	/**
	 * General application role; personal operations require profile ownership.
	 */
	JANUS_USER,
	/**
	 * Employee role; employee operations require a persisted employee link.
	 */
	JANUS_EMPLOYEE
}
