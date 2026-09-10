package es.nivel36.janus.policy.worksite;

import es.nivel36.janus.policy.RolePolicy;
import es.nivel36.janus.service.appuser.Role;

/** Pure authorization policy for this resource operation. */
public final class ManageWorksiteAssignmentsPolicy extends RolePolicy {
	public ManageWorksiteAssignmentsPolicy() {
		super(Role.JANUS_USER, Role.JANUS_ADMIN);
	}
}
