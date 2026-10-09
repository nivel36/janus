package es.nivel36.janus.policy.employee;

import es.nivel36.janus.policy.RolePolicy;
import es.nivel36.janus.service.appuser.Role;

/**
 * Allows users and administrators to delete employee records.
 */
public class DeleteEmployeePolicy extends RolePolicy {

	public DeleteEmployeePolicy() {
		super(Role.JANUS_ADMIN, Role.JANUS_USER);
	}
}
