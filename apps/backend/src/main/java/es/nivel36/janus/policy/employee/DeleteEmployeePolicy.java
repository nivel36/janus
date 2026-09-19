package es.nivel36.janus.policy.employee;

import es.nivel36.janus.policy.RolePolicy;
import es.nivel36.janus.service.appuser.Role;

public class DeleteEmployeePolicy  extends RolePolicy {

	public DeleteEmployeePolicy() {
		super(Role.JANUS_ADMIN, Role.JANUS_USER);
	}
}