package es.nivel36.janus.policy.schedule;

import es.nivel36.janus.policy.RolePolicy;
import es.nivel36.janus.service.appuser.Role;

/**
 * Allows users and administrators to delete schedules.
 */
public final class DeleteSchedulePolicy extends RolePolicy {
	public DeleteSchedulePolicy() {
		super(Role.JANUS_USER, Role.JANUS_ADMIN);
	}
}
