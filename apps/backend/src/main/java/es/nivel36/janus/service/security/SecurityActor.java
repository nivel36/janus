package es.nivel36.janus.service.security;

import java.util.Set;
import es.nivel36.janus.config.ExternalIdentity;

public record SecurityActor(long principalId, PrincipalType type, ExternalIdentity externalIdentity,
		Set<String> authorities) {
	public SecurityActor { authorities = Set.copyOf(authorities); }
}
