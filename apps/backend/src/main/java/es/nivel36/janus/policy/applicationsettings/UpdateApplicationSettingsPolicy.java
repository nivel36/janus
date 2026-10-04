package es.nivel36.janus.policy.applicationsettings;

import es.nivel36.janus.policy.RolePolicy;
import es.nivel36.janus.service.appuser.Role;

/**
 * Allows only administrators to replace global settings.
 */
public final class UpdateApplicationSettingsPolicy extends RolePolicy {
	public UpdateApplicationSettingsPolicy() {
		super(Role.JANUS_ADMIN);
	}
}
