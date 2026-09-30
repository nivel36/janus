package es.nivel36.janus.policy.appuser;

import java.util.Objects;
import java.util.UUID;

import es.nivel36.janus.policy.Policy;
import es.nivel36.janus.security.Actor;
import es.nivel36.janus.service.appuser.Role;

/** Administrators may update any account; other recognized actors only their own. */
public final class UpdateCurrentAppUserPolicy implements Policy<UUID> {
	@Override
	public boolean allows(final Actor actor, final UUID targetUserId) {
		Objects.requireNonNull(actor, "actor can't be null");
		return actor.hasRole(Role.JANUS_ADMIN) || actor.id().equals(targetUserId)
				&& (actor.hasRole(Role.JANUS_EMPLOYEE) || actor.hasRole(Role.JANUS_USER));
	}
}
