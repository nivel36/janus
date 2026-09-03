package es.nivel36.janus.service.security;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import org.springframework.stereotype.Service;
import es.nivel36.janus.service.employee.Employee;

@Service
public class EmployeeAuthorizationPolicy {
	private final EmployeePrincipalRepository relationships;
	private final Clock clock;
	public EmployeeAuthorizationPolicy(EmployeePrincipalRepository relationships) { this(relationships, Clock.systemUTC()); }
	EmployeeAuthorizationPolicy(EmployeePrincipalRepository relationships, Clock clock) {
		this.relationships = Objects.requireNonNull(relationships); this.clock = Objects.requireNonNull(clock);
	}
	public boolean canActAsEmployee(SecurityActor actor, Employee employee) {
		if (actor.authorities().contains("ROLE_JANUS_ADMIN")) return true;
		if (actor.type() != PrincipalType.HUMAN) return false;
		return relationships.findActiveSelf(actor.principalId(), Instant.now(clock))
				.map(link -> Objects.equals(link.getEmployee().getId(), employee.getId())).orElse(false);
	}
	public Employee requireSelf(SecurityActor actor) {
		if (actor.type() != PrincipalType.HUMAN) throw new org.springframework.security.access.AccessDeniedException("Personal endpoints require a human principal");
		return relationships.findActiveSelf(actor.principalId(), Instant.now(clock)).map(EmployeePrincipal::getEmployee)
				.orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("No active SELF relationship"));
	}
}
