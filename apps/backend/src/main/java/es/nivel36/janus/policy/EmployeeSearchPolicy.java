package es.nivel36.janus.policy;

import java.util.Objects;

import es.nivel36.janus.security.Actor;

/**
 * Authorizes optional employee filters without rewriting an explicit request.
 */
public final class EmployeeSearchPolicy implements Policy<EmployeeSearchPolicy.Context> {

	@Override
	public boolean allows(final Actor actor, final Context context) {
		Objects.requireNonNull(actor, "actor can't be null");
		Objects.requireNonNull(context, "context can't be null");
		return EmployeeAccessPolicy.hasElevatedAccess(actor)
				|| EmployeeAccessPolicy.isRestrictedToOwnEmployee(actor) && actor.employeeId() != null
						&& actor.employeeId() > 0 && (!context.employeeFilterPresent() || context.ownsEmployee());
	}

	public record Context(boolean employeeFilterPresent, boolean ownsEmployee) {
	}
}
