package es.nivel36.janus.policy.worksite;

import java.util.Objects;

import es.nivel36.janus.policy.EmployeeAccessPolicy;
import es.nivel36.janus.policy.Policy;
import es.nivel36.janus.security.Actor;
import es.nivel36.janus.service.appuser.Role;

public final class UpdateWorksitePolicy implements Policy<UpdateWorksitePolicy.Context> {
	@Override
	public boolean allows(final Actor actor, final UpdateWorksitePolicy.Context context) {
		Objects.requireNonNull(actor, "actor can't be null");
		Objects.requireNonNull(context, "context can't be null");
		return EmployeeAccessPolicy.hasElevatedAccess(actor) || actor.hasRole(Role.JANUS_EMPLOYEE) && context.employeeCreationAllowed()
				&& context.assignedScope() && context.assignedToWorksite();
	}

	public record Context(boolean employeeCreationAllowed, boolean assignedScope, boolean assignedToWorksite) {
	}
}
