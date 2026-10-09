package es.nivel36.janus.policy;

import java.util.Objects;

import es.nivel36.janus.security.Actor;
import es.nivel36.janus.service.appuser.Role;

/**
 * Shared role and ownership policy for employee-related operations.
 * <p>
 * Users and administrators have elevated access; employees require the context
 * to identify their own employee. Actor and context must be non-null.
 */
public final class EmployeeAccessPolicy implements Policy<EmployeeAccessPolicy.Context> {

	@Override
	public boolean allows(final Actor actor, final Context context) {
		Objects.requireNonNull(actor, "actor can't be null");
		Objects.requireNonNull(context, "context can't be null");
		return hasElevatedAccess(actor) || actor.hasRole(Role.JANUS_EMPLOYEE) && context.ownsEmployee();
	}

	/**
	 * Returns whether the actor has a user or administrator role.
	 *
	 * @param  actor                the actor whose roles are inspected; must not be
	 *                              {@code null}
	 * @return                      {@code true} for {@code JANUS_USER} or
	 *                              {@code JANUS_ADMIN}; {@code false} otherwise
	 * @throws NullPointerException if {@code actor} is {@code null}
	 */
	public static boolean hasElevatedAccess(final Actor actor) {
		return actor.hasRole(Role.JANUS_USER) || actor.hasRole(Role.JANUS_ADMIN);
	}

	/**
	 * Returns whether the actor is an employee without an elevated role.
	 *
	 * @param  actor                the actor whose roles are inspected; must not be
	 *                              {@code null}
	 * @return                      {@code true} if the actor has
	 *                              {@code JANUS_EMPLOYEE} without a user or
	 *                              administrator role; {@code false} otherwise
	 * @throws NullPointerException if {@code actor} is {@code null}
	 */
	public static boolean isRestrictedToOwnEmployee(final Actor actor) {
		return actor.hasRole(Role.JANUS_EMPLOYEE) && !hasElevatedAccess(actor);
	}

	/**
	 * Ownership facts for an employee operation.
	 *
	 * @param ownsEmployee whether the target is the actor's linked employee
	 */
	public record Context(boolean ownsEmployee) {
	}
}
