package es.nivel36.janus.policy.applicationsettings;

import java.util.Objects;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import es.nivel36.janus.security.ActorResolver;

/**
 * Resolves provisioned JWT identities for global settings role policies.
 */
@Component("applicationSettingsAuthorization")
public class ApplicationSettingsAuthorizationAdapter {
	private final ActorResolver actors;
	private final ViewApplicationSettingsPolicy view = new ViewApplicationSettingsPolicy();
	private final UpdateApplicationSettingsPolicy update = new UpdateApplicationSettingsPolicy();

	public ApplicationSettingsAuthorizationAdapter(final ActorResolver actors) {
		this.actors = Objects.requireNonNull(actors, "actors cannot be null");
	}

	public boolean canView(final Authentication authentication) {
		return this.view.allows(this.actors.resolve(authentication), null);
	}

	public boolean canUpdate(final Authentication authentication) {
		return this.update.allows(this.actors.resolve(authentication), null);
	}
}
