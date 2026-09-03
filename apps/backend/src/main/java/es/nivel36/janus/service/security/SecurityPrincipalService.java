package es.nivel36.janus.service.security;

import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import es.nivel36.janus.config.AuthenticatedIdentity;
import es.nivel36.janus.config.ExternalIdentity;

@Service
public class SecurityPrincipalService {
	private final SecurityPrincipalRepository repository;
	public SecurityPrincipalService(SecurityPrincipalRepository repository) { this.repository = Objects.requireNonNull(repository); }
	public SecurityActor resolve(Authentication authentication) {
		ExternalIdentity identity = AuthenticatedIdentity.externalIdentity(authentication);
		SecurityPrincipal principal = repository.findByIssuerAndSubject(identity.issuer(), identity.subject())
				.orElseThrow(() -> new AccessDeniedException("Authenticated principal is not provisioned"));
		if (!principal.isActive()) throw new AccessDeniedException("Authenticated principal is not active");
		return new SecurityActor(principal.getId(), principal.getType(), identity,
				authentication.getAuthorities().stream().map(a -> a.getAuthority()).collect(Collectors.toSet()));
	}
}
