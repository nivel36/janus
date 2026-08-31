/* Copyright 2026 Abel Ferrer Jiménez */
package es.nivel36.janus.config;

/** Immutable OpenID Connect identity, scoped by its issuing realm. */
public record ExternalIdentity(String issuer, String subject) {
}
