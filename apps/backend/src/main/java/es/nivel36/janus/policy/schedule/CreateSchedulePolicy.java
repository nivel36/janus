package es.nivel36.janus.policy.schedule;

import es.nivel36.janus.policy.RolePolicy;
import es.nivel36.janus.service.appuser.Role;

/**
 * Allows users and administrators to create schedules.
 */
public final class CreateSchedulePolicy extends RolePolicy {
	public CreateSchedulePolicy() {
		super(Role.JANUS_USER, Role.JANUS_ADMIN);
	}
}
