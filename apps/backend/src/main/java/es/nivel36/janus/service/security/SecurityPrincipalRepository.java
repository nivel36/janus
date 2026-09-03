package es.nivel36.janus.service.security;

import java.util.Optional;
import org.springframework.data.repository.CrudRepository;

public interface SecurityPrincipalRepository extends CrudRepository<SecurityPrincipal, Long> {
	Optional<SecurityPrincipal> findByIssuerAndSubject(String issuer, String subject);
}
