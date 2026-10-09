package es.nivel36.janus.policy.employee;

import es.nivel36.janus.policy.RolePolicy;
import es.nivel36.janus.service.appuser.Role;

/**
 * Allows users and administrators to create employee records.
 */
public final class CreateEmployeePolicy extends RolePolicy {

	public CreateEmployeePolicy() {
		super(Role.JANUS_ADMIN, Role.JANUS_USER);
	}
}
