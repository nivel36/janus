package es.nivel36.janus.policy.catalog;

import es.nivel36.janus.policy.RolePolicy;
import es.nivel36.janus.service.appuser.Role;

/** Pure authorization policy for this resource operation. */
public final class ViewCatalogPolicy extends RolePolicy {
	public ViewCatalogPolicy() {
		super(Role.JANUS_EMPLOYEE, Role.JANUS_USER, Role.JANUS_ADMIN);
	}
}
