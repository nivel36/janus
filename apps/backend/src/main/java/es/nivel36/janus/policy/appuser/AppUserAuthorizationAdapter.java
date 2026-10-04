package es.nivel36.janus.policy.appuser;

import org.springframework.security.access.AccessDeniedException;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import es.nivel36.janus.security.Actor;
import es.nivel36.janus.security.ActorResolver;

/**
 * Spring Security adapter for application-profile operation policies. The
 * injected resolver must be nonnull. Each decision requires trusted
 * resource-server authentication resolving to an already provisioned actor. The
 * adapter reads actor identity and returns policy decisions without
 * provisioning, changing preferences or altering employee associations.
 */
@Component("appUserAuthorization")
public class AppUserAuthorizationAdapter {
	private final ActorResolver actors;
	private final UpdateCurrentAppUserPolicy updateCurrent = new UpdateCurrentAppUserPolicy();
	private final DeleteAppUserPolicy delete = new DeleteAppUserPolicy();
	private final SearchAppUserPolicy search = new SearchAppUserPolicy();

	/**
	 * Stores the actor resolver used by later decisions without resolving an actor.
	 * The caller must supply a nonnull resolver; construction does not validate it.
	 *
	 * @param actors nonnull resolver of previously provisioned actors
	 */
	public AppUserAuthorizationAdapter(final ActorResolver actors) {
		this.actors = actors;
	}

	/**
	 * Checks administrative search permission without changing any profile.
	 *
	 * @param  authentication        trusted authentication for an existing local
	 *                               profile
	 * @return                       true exactly when the resolved actor has
	 *                               JANUS_ADMIN
	 * @throws AccessDeniedException if the actor cannot be resolved from
	 *                               authentication
	 */
	public boolean canSearch(final Authentication authentication) {
		return this.search.allows(this.actors.resolve(authentication), null);
	}

	/**
	 * Checks persistent ownership or the administrative override without mutation.
	 *
	 * @param  a                     trusted authentication for an existing local
	 *                               profile
	 * @param  id                    target UUID; null cannot match personal
	 *                               ownership
	 * @return                       true for JANUS_ADMIN, or for
	 *                               JANUS_USER/JANUS_EMPLOYEE targeting their own
	 *                               persistent UUID; false otherwise
	 * @throws AccessDeniedException if the actor cannot be resolved from
	 *                               authentication
	 */
	public boolean canUpdate(final Authentication a, final UUID id) {
		final Actor actor = this.actors.resolve(a);
		return this.updateCurrent.allows(actor, id);
	}

	/**
	 * Checks administrative deletion permission without deleting any profile.
	 *
	 * @param  a                     trusted authentication for an existing local
	 *                               profile
	 * @return                       true exactly when the resolved actor has
	 *                               JANUS_ADMIN
	 * @throws AccessDeniedException if the actor cannot be resolved from
	 *                               authentication
	 */
	public boolean canDelete(final Authentication a) {
		return this.delete.allows(this.actors.resolve(a), null);
	}
}
