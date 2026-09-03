package es.nivel36.janus.service.security;

import java.time.Instant;
import es.nivel36.janus.service.employee.Employee;
import jakarta.persistence.*;

/** A one-way representation of a credential presented to an authenticated device. */
@Entity
@Table(name = "EMPLOYEE_CREDENTIAL", uniqueConstraints = @UniqueConstraint(name = "UK_EMPLOYEE_CREDENTIAL_IDENTIFIER", columnNames = { "TYPE", "IDENTIFIER_HASH" }))
public class EmployeeCredential {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
	@ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "EMPLOYEE_ID") private Employee employee;
	@Column(nullable = false) private String type;
	@Column(name = "IDENTIFIER_HASH", nullable = false, updatable = false) private String identifierHash;
	@Column(nullable = false) private boolean enabled;
	@Column(nullable = false) private Instant validFrom;
	private Instant validUntil;
	@Column(nullable = false, updatable = false) private Instant createdAt;
	private Instant revokedAt;

	protected EmployeeCredential() { }
	public EmployeeCredential(Employee employee, String type, String identifierHash, Instant validFrom) {
		this.employee = java.util.Objects.requireNonNull(employee);
		this.type = es.nivel36.janus.util.Strings.requireNonBlank(type, "type can't be blank");
		this.identifierHash = es.nivel36.janus.util.Strings.requireNonBlank(identifierHash, "identifierHash can't be blank");
		this.validFrom = java.util.Objects.requireNonNull(validFrom);
		this.createdAt = Instant.now(); this.enabled = true;
	}
	public Employee getEmployee() { return employee; }
	public boolean isActiveAt(Instant at) { return enabled && revokedAt == null && !validFrom.isAfter(at) && (validUntil == null || validUntil.isAfter(at)); }
	public void revoke(Instant at) { enabled = false; revokedAt = java.util.Objects.requireNonNull(at); validUntil = at; }
}
