package es.nivel36.janus.policy.appuser;

import java.util.Objects;
import java.util.UUID;

import es.nivel36.janus.policy.Policy;
import es.nivel36.janus.security.Actor;
import es.nivel36.janus.service.appuser.Role;

/**
 * Pure policy for editing profile preferences. The actor must be nonnull.
 * Administrators may target any UUID; {@code JANUS_USER} and
 * {@code JANUS_EMPLOYEE} may target only their own persistent UUID. A
 * {@code null} target cannot match personal ownership, but does not remove the
 * administrator override. Evaluation returns a decision without modifying a
 * profile or checking whether the target exists; resource validation and the
 * service handle those checks.
 */
public final class UpdateCurrentAppUserPolicy implements Policy<UUID> {

	/**
	 * Creates the stateless ownership policy with the administrator override.
	 * Construction requires no context and does not update a profile.
	 */
	public UpdateCurrentAppUserPolicy() {
	}

	@Override
	public boolean allows(final Actor actor, final UUID targetUserId) {
		Objects.requireNonNull(actor, "actor can't be null");
		return actor.hasRole(Role.JANUS_ADMIN) || actor.id().equals(targetUserId)
				&& (actor.hasRole(Role.JANUS_EMPLOYEE) || actor.hasRole(Role.JANUS_USER));
	}
}
