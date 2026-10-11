package es.nivel36.janus.policy.schedule;

import es.nivel36.janus.policy.RolePolicy;
import es.nivel36.janus.service.appuser.Role;

/**
 * Allows users and administrators to search schedules.
 */
public final class SearchSchedulePolicy extends RolePolicy {
	public SearchSchedulePolicy() {
		super(Role.JANUS_USER, Role.JANUS_ADMIN);
	}
}
