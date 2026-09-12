package es.nivel36.janus.policy.appuser;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import es.nivel36.janus.security.ActorResolver;

@Component("appUserAuthorization")
public class AppUserAuthorizationAdapter {
	private final ActorResolver actors;
	private final ViewAppUserPolicy view = new ViewAppUserPolicy();
	private final CreateAppUserPolicy create = new CreateAppUserPolicy();
	private final UpdateAppUserPolicy update = new UpdateAppUserPolicy();
	private final UpdateCurrentAppUserPolicy updateCurrent = new UpdateCurrentAppUserPolicy();
	private final DeleteAppUserPolicy delete = new DeleteAppUserPolicy();

	public AppUserAuthorizationAdapter(final ActorResolver actors) {
		this.actors = actors;
	}

	public boolean canView(final Authentication a) {
		return this.view.allows(this.actors.resolve(a), null);
	}

	public boolean canCreate(final Authentication a) {
		return this.create.allows(this.actors.resolve(a), null);
	}

	public boolean canUpdate(final Authentication a) {
		return this.update.allows(this.actors.resolve(a), null);
	}

	public boolean canUpdateCurrent(final Authentication a) {
		return this.updateCurrent.allows(this.actors.resolve(a), null);
	}

	public boolean canDelete(final Authentication a) {
		return this.delete.allows(this.actors.resolve(a), null);
	}
}
