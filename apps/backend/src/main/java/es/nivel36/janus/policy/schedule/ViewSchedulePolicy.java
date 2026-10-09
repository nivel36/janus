package es.nivel36.janus.policy.schedule;

import java.util.Objects;

import es.nivel36.janus.policy.EmployeeAccessPolicy;
import es.nivel36.janus.policy.Policy;
import es.nivel36.janus.security.Actor;
import es.nivel36.janus.service.appuser.Role;

/**
 * Authorizes schedule reads for users, administrators and assigned employees.
 * <p>
 * Actor and context must be non-null.
 */
public final class ViewSchedulePolicy implements Policy<ViewSchedulePolicy.Context> {
	@Override
	public boolean allows(final Actor actor, final Context context) {
		Objects.requireNonNull(actor, "actor can't be null");
		Objects.requireNonNull(context, "context can't be null");
		return EmployeeAccessPolicy.hasElevatedAccess(actor)
				|| actor.hasRole(Role.JANUS_EMPLOYEE) && context.assignedToSchedule();
	}

	/**
	 * Assignment facts for a schedule read.
	 *
	 * @param assignedToSchedule whether the actor is assigned to the target
	 *                           schedule
	 */
	public record Context(boolean assignedToSchedule) {
	}
}
