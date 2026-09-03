package es.nivel36.janus.service.security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import es.nivel36.janus.service.employee.Employee;

/** Resolves the personal employee exclusively through principal and active SELF link. */
@Service
public class AuthenticatedEmployeeResolver {
	private final SecurityPrincipalService principals;
	private final EmployeeAuthorizationPolicy policy;
	public AuthenticatedEmployeeResolver(SecurityPrincipalService principals, EmployeeAuthorizationPolicy policy) {
		this.principals = principals; this.policy = policy;
	}
	public Employee resolve(Authentication authentication) {
		return policy.requireSelf(principals.resolve(authentication));
	}
}
