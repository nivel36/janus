package es.nivel36.janus.policy.catalog;
import org.springframework.security.core.Authentication; import org.springframework.stereotype.Component; import es.nivel36.janus.security.ActorResolver;
@Component("catalogAuthorization") public class CatalogAuthorizationAdapter { private final ActorResolver actors; private final ViewCatalogPolicy view=new ViewCatalogPolicy(); public CatalogAuthorizationAdapter(ActorResolver actors){this.actors=actors;} public boolean canView(Authentication a){return view.allows(actors.resolve(a),null);} }
