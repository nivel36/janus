package es.nivel36.janus.service.security;

import java.time.Instant;
import es.nivel36.janus.service.employee.Employee;
import jakarta.persistence.*;

@Entity
@Table(name = "EMPLOYEE_PRINCIPAL")
public class EmployeePrincipal {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "EMPLOYEE_ID", nullable = false)
	private Employee employee;
	@ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "SECURITY_PRINCIPAL_ID", nullable = false)
	private SecurityPrincipal principal;
	@Enumerated(EnumType.STRING) @Column(nullable = false)
	private RelationshipType relationshipType;
	@Column(nullable = false) private boolean enabled;
	@Column(nullable = false) private Instant validFrom;
	private Instant validUntil;
	@Column(nullable = false, updatable = false) private Instant createdAt;
	@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "CREATED_BY_PRINCIPAL_ID")
	private SecurityPrincipal createdBy;

	protected EmployeePrincipal() { }
	public EmployeePrincipal(Employee employee, SecurityPrincipal principal, RelationshipType type, SecurityPrincipal createdBy) {
		this.employee = java.util.Objects.requireNonNull(employee);
		this.principal = java.util.Objects.requireNonNull(principal);
		this.relationshipType = java.util.Objects.requireNonNull(type);
		this.createdBy = createdBy;
		this.enabled = true;
		this.validFrom = Instant.now();
		this.createdAt = this.validFrom;
	}
	public Employee getEmployee() { return employee; }
	public boolean isActiveAt(Instant at) { return enabled && !validFrom.isAfter(at) && (validUntil == null || validUntil.isAfter(at)); }
	public void revoke(Instant at) { enabled = false; validUntil = java.util.Objects.requireNonNull(at); }
}
