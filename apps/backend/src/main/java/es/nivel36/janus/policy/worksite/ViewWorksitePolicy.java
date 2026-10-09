package es.nivel36.janus.policy.worksite;

import java.util.Objects;

import es.nivel36.janus.policy.EmployeeAccessPolicy;
import es.nivel36.janus.policy.Policy;
import es.nivel36.janus.security.Actor;
import es.nivel36.janus.service.worksite.WorksiteScope;

/** Authorizes worksite visibility from resolved scope and assignment facts. */
public final class ViewWorksitePolicy implements Policy<ViewWorksitePolicy.Context> {
	@Override
	public boolean allows(final Actor actor, final Context context) {
		Objects.requireNonNull(actor, "actor can't be null");
		Objects.requireNonNull(context, "context can't be null");
		return EmployeeAccessPolicy.hasElevatedAccess(actor) || EmployeeAccessPolicy.isRestrictedToOwnEmployee(actor)
				&& actor.employeeId() != null && context.worksiteExists()
				&& (context.scope() == WorksiteScope.GLOBAL || context.assignedToWorksite());
	}

	/**
	 * Visibility facts for a worksite read.
	 *
	 * @param worksiteExists     whether the target worksite exists
	 * @param scope              the resolved worksite scope, or {@code null} if
	 *                           absent
	 * @param assignedToWorksite whether the actor is assigned to the target
	 *                           worksite
	 */
	public record Context(boolean worksiteExists, WorksiteScope scope, boolean assignedToWorksite) {
	}
}
