package es.nivel36.janus.policy.employee;

import java.util.Objects;

import es.nivel36.janus.policy.Policy;
import es.nivel36.janus.security.Actor;
import es.nivel36.janus.service.appuser.Role;

public final class CreateEmployeePolicy implements Policy<Void> {

	@Override
	public boolean allows(final Actor actor, Void context) {
		Objects.requireNonNull(actor, "actor can't be null");

		if (actor.hasRole(Role.JANUS_ADMIN) || actor.hasRole(Role.JANUS_USER)) {
			return true;
		}
		return false;
	}
}