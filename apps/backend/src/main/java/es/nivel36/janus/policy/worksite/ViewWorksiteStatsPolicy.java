package es.nivel36.janus.policy.worksite;

import java.util.Objects;

import es.nivel36.janus.policy.EmployeeAccessPolicy;
import es.nivel36.janus.policy.Policy;
import es.nivel36.janus.security.Actor;
import es.nivel36.janus.service.appuser.Role;

/**
 * Authorizes worksite statistics for users, administrators and assigned
 * employees.
 * <p>
 * Actor and context must be non-null. Global visibility alone does not grant
 * employees access to statistics.
 */
public final class ViewWorksiteStatsPolicy implements Policy<ViewWorksiteStatsPolicy.Context> {
	@Override
	public boolean allows(final Actor actor, final Context context) {
		Objects.requireNonNull(actor, "actor can't be null");
		Objects.requireNonNull(context, "context can't be null");
		return EmployeeAccessPolicy.hasElevatedAccess(actor)
				|| actor.hasRole(Role.JANUS_EMPLOYEE) && context.assignedToWorksite();
	}

	/**
	 * Assignment facts for worksite statistics.
	 *
	 * @param assignedToWorksite whether the actor is assigned to the target
	 *                           worksite
	 */
	public record Context(boolean assignedToWorksite) {
	}
}
