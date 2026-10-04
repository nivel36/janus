package es.nivel36.janus.policy;

import java.util.Objects;
import java.util.Set;

import es.nivel36.janus.security.Actor;
import es.nivel36.janus.service.appuser.Role;

/**
 * Pure policy accepting actors with at least one configured role. Construction
 * requires distinct nonnull roles; evaluation requires a nonnull actor and
 * ignores its Void context. Evaluation never provisions an actor or changes
 * domain state; a boolean result expresses role membership only.
 */
public class RolePolicy implements Policy<Void> {
	private final Set<Role> roles;

	/**
	 * Creates a policy with an immutable set of accepted roles.
	 *
	 * @param  roles                    nonnull array of distinct nonnull accepted
	 *                                  roles; an empty array creates a policy that
	 *                                  denies every actor
	 * @throws NullPointerException     if the array or any role is null
	 * @throws IllegalArgumentException if a role is repeated
	 */
	protected RolePolicy(final Role... roles) {
		this.roles = Set.of(roles);
	}

	@Override
	public boolean allows(final Actor actor, final Void context) {
		Objects.requireNonNull(actor, "actor can't be null");
		return this.roles.stream().anyMatch(actor::hasRole);
	}
}
