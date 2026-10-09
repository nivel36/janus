package es.nivel36.janus.policy.worksite;

import java.util.Objects;

import es.nivel36.janus.policy.EmployeeAccessPolicy;
import es.nivel36.janus.policy.Policy;
import es.nivel36.janus.security.Actor;
import es.nivel36.janus.service.appuser.Role;

/**
 * Authorizes worksite creation from role, feature and scope facts.
 * <p>
 * Users and administrators may create any scope. Employees may create assigned
 * worksites only when employee creation is enabled. Actor and context must be
 * non-null.
 */
public final class CreateWorksitePolicy implements Policy<CreateWorksitePolicy.Context> {
	@Override
	public boolean allows(final Actor actor, final CreateWorksitePolicy.Context context) {
		Objects.requireNonNull(actor, "actor can't be null");
		Objects.requireNonNull(context, "context can't be null");
		return EmployeeAccessPolicy.hasElevatedAccess(actor)
				|| actor.hasRole(Role.JANUS_EMPLOYEE) && context.employeeCreationAllowed() && context.assignedScope();
	}

	/**
	 * Feature and scope facts for worksite creation.
	 *
	 * @param employeeCreationAllowed whether employee worksite creation is enabled
	 * @param assignedScope           whether the requested scope is assigned
	 */
	public record Context(boolean employeeCreationAllowed, boolean assignedScope) {
	}
}
