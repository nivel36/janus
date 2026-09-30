package es.nivel36.janus.policy.worksite;

import es.nivel36.janus.policy.EmployeeSearchPolicy;
import es.nivel36.janus.policy.Policy;
import es.nivel36.janus.security.Actor;

public final class SearchWorksitePolicy implements Policy<EmployeeSearchPolicy.Context> {
	private final EmployeeSearchPolicy search = new EmployeeSearchPolicy();

	@Override
	public boolean allows(final Actor actor, final EmployeeSearchPolicy.Context context) {
		return this.search.allows(actor, context);
	}
}
