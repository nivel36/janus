package es.nivel36.janus.policy;

import java.util.Objects;

import es.nivel36.janus.security.Actor;
import es.nivel36.janus.service.appuser.Role;

/** Shared role and ownership rules for employee-related resources. */
public final class EmployeeAccessPolicy implements Policy<EmployeeAccessPolicy.Context> {

	@Override
	public boolean allows(final Actor actor, final Context context) {
		Objects.requireNonNull(actor, "actor can't be null");
		Objects.requireNonNull(context, "context can't be null");
		return hasElevatedAccess(actor) || actor.hasRole(Role.JANUS_EMPLOYEE) && context.ownsEmployee();
	}

	public static boolean hasElevatedAccess(final Actor actor) {
		return actor.hasRole(Role.JANUS_USER) || actor.hasRole(Role.JANUS_ADMIN);
	}

	public static boolean isRestrictedToOwnEmployee(final Actor actor) {
		return actor.hasRole(Role.JANUS_EMPLOYEE) && !hasElevatedAccess(actor);
	}

	public record Context(boolean ownsEmployee) {
	}
}
