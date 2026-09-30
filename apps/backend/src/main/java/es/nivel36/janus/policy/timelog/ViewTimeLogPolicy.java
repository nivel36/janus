package es.nivel36.janus.policy.timelog;

import es.nivel36.janus.policy.EmployeeAccessPolicy;
import es.nivel36.janus.policy.Policy;
import es.nivel36.janus.security.Actor;

public final class ViewTimeLogPolicy implements Policy<EmployeeAccessPolicy.Context> {
	private final EmployeeAccessPolicy access = new EmployeeAccessPolicy();

	@Override
	public boolean allows(final Actor actor, final EmployeeAccessPolicy.Context context) {
		return this.access.allows(actor, context);
	}
}
