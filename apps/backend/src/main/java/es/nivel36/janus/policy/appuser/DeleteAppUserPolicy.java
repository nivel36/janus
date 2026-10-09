package es.nivel36.janus.policy.appuser;

import es.nivel36.janus.policy.RolePolicy;
import es.nivel36.janus.service.appuser.Role;

/**
 * Allows administrators to delete local application profiles.
 * <p>
 * Evaluation requires a non-null actor and ignores the operation context. It
 * checks role membership without checking target existence.
 */
public final class DeleteAppUserPolicy extends RolePolicy {

	/**
	 * Creates a policy accepting only {@code JANUS_ADMIN}. Construction requires no
	 * arguments and performs no delete operation.
	 */
	public DeleteAppUserPolicy() {
		super(Role.JANUS_ADMIN);
	}
}
