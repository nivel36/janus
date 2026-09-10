package es.nivel36.janus.policy.schedule;

import es.nivel36.janus.policy.RolePolicy;
import es.nivel36.janus.service.appuser.Role;

/** Pure authorization policy for this resource operation. */
public final class UpdateSchedulePolicy extends RolePolicy {
	public UpdateSchedulePolicy() {
		super(Role.JANUS_USER, Role.JANUS_ADMIN);
	}
}
