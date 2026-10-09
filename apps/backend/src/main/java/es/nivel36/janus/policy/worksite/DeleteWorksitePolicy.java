package es.nivel36.janus.policy.worksite;

import es.nivel36.janus.policy.RolePolicy;
import es.nivel36.janus.service.appuser.Role;

/**
 * Allows users and administrators to delete worksites.
 */
public final class DeleteWorksitePolicy extends RolePolicy {
	public DeleteWorksitePolicy() {
		super(Role.JANUS_USER, Role.JANUS_ADMIN);
	}
}
