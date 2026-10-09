package es.nivel36.janus.policy.timelog;

import es.nivel36.janus.policy.EmployeeAccessPolicy;
import es.nivel36.janus.policy.Policy;
import es.nivel36.janus.security.Actor;

/**
 * Authorizes time-log reads using the shared {@link EmployeeAccessPolicy}
 * contract.
 * <p>
 * Users and administrators have elevated access. Employees require ownership of
 * the target employee. Actor and context must be non-null.
 */
public final class ViewTimeLogPolicy implements Policy<EmployeeAccessPolicy.Context> {
	private final EmployeeAccessPolicy access = new EmployeeAccessPolicy();

	@Override
	public boolean allows(final Actor actor, final EmployeeAccessPolicy.Context context) {
		return this.access.allows(actor, context);
	}
}
