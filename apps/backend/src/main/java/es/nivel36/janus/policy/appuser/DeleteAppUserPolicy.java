package es.nivel36.janus.policy.appuser;

import es.nivel36.janus.policy.RolePolicy;
import es.nivel36.janus.service.appuser.Role;

/**
 * Pure role policy permitting only JANUS_ADMIN to delete local profiles. A
 * nonnull actor is required; the operation context is unused. Evaluation
 * returns a boolean without persistence or identity-provider effects. Actor
 * provisioning and target existence are checked by the adapter and service,
 * respectively.
 */
public final class DeleteAppUserPolicy extends RolePolicy {

	/**
	 * Creates a policy accepting only JANUS_ADMIN. Construction requires no
	 * arguments and performs no delete operation.
	 */
	public DeleteAppUserPolicy() {
		super(Role.JANUS_ADMIN);
	}
}
