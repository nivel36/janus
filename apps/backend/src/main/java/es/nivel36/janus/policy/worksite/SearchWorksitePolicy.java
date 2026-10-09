package es.nivel36.janus.policy.worksite;

import es.nivel36.janus.policy.EmployeeSearchPolicy;
import es.nivel36.janus.policy.Policy;
import es.nivel36.janus.security.Actor;

/**
 * Authorizes searches for worksites using {@link EmployeeSearchPolicy}.
 * <p>
 * Users and administrators may request any employee filter. Restricted
 * employees must have a positive persistent employee identifier and may request
 * only their own employee or omit the filter. Actor and context must be
 * non-null.
 */
public final class SearchWorksitePolicy implements Policy<EmployeeSearchPolicy.Context> {
	private final EmployeeSearchPolicy search = new EmployeeSearchPolicy();

	@Override
	public boolean allows(final Actor actor, final EmployeeSearchPolicy.Context context) {
		return this.search.allows(actor, context);
	}
}
