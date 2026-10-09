package es.nivel36.janus.policy;

import java.util.Objects;

import es.nivel36.janus.security.Actor;

/**
 * Authorizes optional employee filters without rewriting explicit requests.
 * <p>
 * Users and administrators may search any employee. Restricted employees
 * require a positive persistent employee identifier and may omit the filter or
 * request their own employee. Actor and context must be non-null.
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

	/**
	 * Filter facts for an employee-scoped search.
	 *
	 * @param employeeFilterPresent whether an explicit employee filter was supplied
	 * @param ownsEmployee          whether the explicit filter identifies the
	 *                              actor's employee
	 */
	public record Context(boolean employeeFilterPresent, boolean ownsEmployee) {
	}
}
