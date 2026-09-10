package es.nivel36.janus.policy.worksite;

import es.nivel36.janus.policy.RolePolicy;
import es.nivel36.janus.service.appuser.Role;

/** Pure authorization policy for this resource operation. */
public final class ViewWorksitePolicy extends RolePolicy {
	public ViewWorksitePolicy() {
		super(Role.JANUS_EMPLOYEE, Role.JANUS_USER, Role.JANUS_ADMIN);
	}
}
