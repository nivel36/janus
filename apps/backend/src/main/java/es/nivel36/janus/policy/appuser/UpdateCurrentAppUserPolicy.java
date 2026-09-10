package es.nivel36.janus.policy.appuser;

import es.nivel36.janus.policy.RolePolicy;
import es.nivel36.janus.service.appuser.Role;

/** Pure authorization policy for this resource operation. */
public final class UpdateCurrentAppUserPolicy extends RolePolicy {
	public UpdateCurrentAppUserPolicy() {
		super(Role.JANUS_EMPLOYEE, Role.JANUS_USER, Role.JANUS_ADMIN);
	}
}
