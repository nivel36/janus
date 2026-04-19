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
 * Scope of a {@link Worksite}.
 *
 * <p>
 * The scope determines who can see the worksite and whether an owner employee
 * relation is expected.
 * </p>
 */
public enum WorksiteScope {

	/**
	 * Worksite visible to every employee.
	 */
	GLOBAL,

	/**
	 * Worksite available only to employees explicitly assigned through the
	 * employee-worksite relation.
	 */
	ASSIGNED
}
