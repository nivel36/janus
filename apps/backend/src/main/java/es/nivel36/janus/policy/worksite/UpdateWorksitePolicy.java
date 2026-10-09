package es.nivel36.janus.policy.worksite;

import java.util.Objects;

import es.nivel36.janus.policy.EmployeeAccessPolicy;
import es.nivel36.janus.policy.Policy;
import es.nivel36.janus.security.Actor;
import es.nivel36.janus.service.appuser.Role;

/**
 * Authorizes worksite updates from role, feature, scope and assignment facts.
 * <p>
 * Users and administrators may update any worksite. Employees require employee
 * creation to be enabled, an assigned target scope and a worksite assignment.
 * Actor and context must be non-null.
 */
public final class UpdateWorksitePolicy implements Policy<UpdateWorksitePolicy.Context> {
	@Override
	public boolean allows(final Actor actor, final UpdateWorksitePolicy.Context context) {
		Objects.requireNonNull(actor, "actor can't be null");
		Objects.requireNonNull(context, "context can't be null");
		return EmployeeAccessPolicy.hasElevatedAccess(actor) || actor.hasRole(Role.JANUS_EMPLOYEE)
				&& context.employeeCreationAllowed() && context.assignedScope() && context.assignedToWorksite();
	}

	/**
	 * Feature, scope and assignment facts for a worksite update.
	 *
	 * @param employeeCreationAllowed whether employee worksite creation is enabled
	 * @param assignedScope           whether the requested scope is assigned
	 * @param assignedToWorksite      whether the actor is assigned to the target
	 *                                worksite
	 */
	public record Context(boolean employeeCreationAllowed, boolean assignedScope, boolean assignedToWorksite) {
	}
}
