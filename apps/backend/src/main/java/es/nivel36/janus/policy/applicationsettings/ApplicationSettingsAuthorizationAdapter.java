package es.nivel36.janus.policy.applicationsettings;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import es.nivel36.janus.security.ActorResolver;

@Component("applicationSettingsAuthorization")
public class ApplicationSettingsAuthorizationAdapter {
	private final ActorResolver actors;
	private final ViewApplicationSettingsPolicy view = new ViewApplicationSettingsPolicy();
	private final UpdateApplicationSettingsPolicy update = new UpdateApplicationSettingsPolicy();

	public ApplicationSettingsAuthorizationAdapter(final ActorResolver actors) {
		this.actors = actors;
	}

	public boolean canView(final Authentication a) {
		return this.view.allows(this.actors.resolve(a), null);
	}

	public boolean canUpdate(final Authentication a) {
		return this.update.allows(this.actors.resolve(a), null);
	}
}
