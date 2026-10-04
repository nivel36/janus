package es.nivel36.janus.policy.applicationsettings;

import es.nivel36.janus.policy.RolePolicy;
import es.nivel36.janus.service.appuser.Role;

/**
 * Allows employees, users and administrators to read global settings.
 */
public final class ViewApplicationSettingsPolicy extends RolePolicy {
	public ViewApplicationSettingsPolicy() {
		super(Role.JANUS_EMPLOYEE, Role.JANUS_USER, Role.JANUS_ADMIN);
	}
}
