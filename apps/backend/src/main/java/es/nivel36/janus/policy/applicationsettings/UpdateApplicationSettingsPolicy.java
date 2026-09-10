package es.nivel36.janus.policy.applicationsettings;

import es.nivel36.janus.policy.RolePolicy;
import es.nivel36.janus.service.appuser.Role;

/** Pure authorization policy for this resource operation. */
public final class UpdateApplicationSettingsPolicy extends RolePolicy {
	public UpdateApplicationSettingsPolicy() {
		super(Role.JANUS_ADMIN);
	}
}
