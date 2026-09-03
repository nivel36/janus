package es.nivel36.janus.service.security;

import java.time.Instant;

import es.nivel36.janus.util.Strings;
import jakarta.persistence.*;

@Entity
@Table(name = "SECURITY_PRINCIPAL", uniqueConstraints = @UniqueConstraint(name = "UK_SECURITY_PRINCIPAL_EXTERNAL_IDENTITY", columnNames = { "ISSUER", "SUBJECT" }))
public class SecurityPrincipal {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Enumerated(EnumType.STRING) @Column(nullable = false, updatable = false)
	private PrincipalType type;
	@Column(nullable = false, updatable = false)
	private String issuer;
	@Column(nullable = false, updatable = false)
	private String subject;
	@Column(nullable = false)
	private boolean enabled;
	@Column(nullable = false, updatable = false)
	private Instant createdAt;
	private Instant revokedAt;
	private String displayName;

	protected SecurityPrincipal() { }

	public SecurityPrincipal(PrincipalType type, String issuer, String subject, String displayName) {
		this.type = java.util.Objects.requireNonNull(type, "type can't be null");
		this.issuer = Strings.requireNonBlank(issuer, "issuer can't be blank").trim();
		this.subject = Strings.requireNonBlank(subject, "subject can't be blank").trim();
		this.displayName = displayName;
		this.enabled = true;
		this.createdAt = Instant.now();
	}

	public Long getId() { return id; }
	public PrincipalType getType() { return type; }
	public String getIssuer() { return issuer; }
	public String getSubject() { return subject; }
	public boolean isEnabled() { return enabled; }
	public Instant getRevokedAt() { return revokedAt; }
	public Instant getCreatedAt() { return createdAt; }
	public String getDisplayName() { return displayName; }
	public boolean isActive() { return enabled && revokedAt == null; }
	public void disable() { enabled = false; }
	public void revoke(Instant at) { revokedAt = java.util.Objects.requireNonNull(at); }
}
