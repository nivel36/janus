package es.nivel36.janus.policy.appuser;

import es.nivel36.janus.policy.RolePolicy;
import es.nivel36.janus.service.appuser.Role;

/** Pure authorization policy for this resource operation. */
public final class UpdateAppUserPolicy extends RolePolicy {
	public UpdateAppUserPolicy() {
		super(Role.JANUS_ADMIN);
	}
}
