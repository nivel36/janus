package es.nivel36.janus.policy;

import java.util.Objects;
import java.util.Set;

import es.nivel36.janus.security.Actor;
import es.nivel36.janus.service.appuser.Role;

/** Pure policy for operations whose complete context is a set of accepted roles. */
public class RolePolicy implements Policy<Void> {
	private final Set<Role> roles;

	protected RolePolicy(final Role... roles) {
		this.roles = Set.of(roles);
	}

	@Override
	public boolean allows(final Actor actor, final Void context) {
		Objects.requireNonNull(actor, "actor can't be null");
		return this.roles.stream().anyMatch(actor::hasRole);
	}
}
