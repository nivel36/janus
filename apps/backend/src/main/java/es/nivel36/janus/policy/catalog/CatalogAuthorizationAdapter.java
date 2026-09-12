package es.nivel36.janus.policy.catalog;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import es.nivel36.janus.security.ActorResolver;

@Component("catalogAuthorization")
public class CatalogAuthorizationAdapter {
	private final ActorResolver actors;
	private final ViewCatalogPolicy view = new ViewCatalogPolicy();

	public CatalogAuthorizationAdapter(final ActorResolver actors) {
		this.actors = actors;
	}

	public boolean canView(final Authentication a) {
		return this.view.allows(this.actors.resolve(a), null);
	}
}
