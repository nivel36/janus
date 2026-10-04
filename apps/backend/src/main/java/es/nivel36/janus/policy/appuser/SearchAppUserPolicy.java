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
package es.nivel36.janus.policy.appuser;

import es.nivel36.janus.policy.RolePolicy;
import es.nivel36.janus.service.appuser.Role;

/**
 * Pure role policy permitting only JANUS_ADMIN to search local profiles. A
 * nonnull actor is required; the operation context is unused. Evaluation
 * returns a boolean without persistence or identity-provider effects. Actor
 * provisioning and target existence are checked by the adapter and service,
 * respectively.
 */
public final class SearchAppUserPolicy extends RolePolicy {
	/**
	 * Creates a policy accepting only JANUS_ADMIN. Construction requires no
	 * arguments and performs no search operation.
	 */
	public SearchAppUserPolicy() {
		super(Role.JANUS_ADMIN);
	}
}
