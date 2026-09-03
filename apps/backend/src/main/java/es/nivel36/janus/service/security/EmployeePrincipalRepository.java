package es.nivel36.janus.service.security;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface EmployeePrincipalRepository extends CrudRepository<EmployeePrincipal, Long> {
	@Query("""
		select ep from EmployeePrincipal ep join fetch ep.employee
		where ep.principal.id = :principalId and ep.relationshipType = 'SELF'
		and ep.enabled = true and ep.validFrom <= :now
		and (ep.validUntil is null or ep.validUntil > :now)
		""")
	Optional<EmployeePrincipal> findActiveSelf(long principalId, Instant now);
}
