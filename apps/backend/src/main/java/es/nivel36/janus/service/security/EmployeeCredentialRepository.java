package es.nivel36.janus.service.security;

import java.util.Optional;
import org.springframework.data.repository.CrudRepository;

interface EmployeeCredentialRepository extends CrudRepository<EmployeeCredential, Long> {
	Optional<EmployeeCredential> findByTypeAndIdentifierHash(String type, String identifierHash);
}
