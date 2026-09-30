package es.nivel36.janus.policy.appuser;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import es.nivel36.janus.security.Actor;
import es.nivel36.janus.security.ActorResolver;

@Component("appUserAuthorization")
public class AppUserAuthorizationAdapter {
	private final ActorResolver actors;
	private final UpdateCurrentAppUserPolicy updateCurrent = new UpdateCurrentAppUserPolicy();
	private final DeleteAppUserPolicy delete = new DeleteAppUserPolicy();

	public AppUserAuthorizationAdapter(final ActorResolver actors) {
		this.actors = actors;
	}

	public boolean canUpdate(final Authentication a, final UUID id) {
		final Actor actor = this.actors.resolve(a);
		return this.updateCurrent.allows(actor, id);
	}

	public boolean canDelete(final Authentication a) {
		return this.delete.allows(this.actors.resolve(a), null);
	}
}
