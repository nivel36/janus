# Janus Helm Chart

Single deployment of the full Janus platform (nginx + backend + keycloak + 2x postgres + grafana).

## Requirements

- Helm 3
- A Kubernetes cluster with a default StorageClass (when persistence is enabled)
- Published images for:
  - `janus/nginx`
  - `janus/backend`

## Installation

```bash
helm upgrade --install janus ./deploy/helm/janus -n janus --create-namespace
```

## Recommended minimum configuration

Create a `values-prod.yaml` file with real secrets and hostname:

```yaml
global:
  appDbPassword: "<password-app-db>"
  keycloakDbPassword: "<password-keycloak-db>"
  keycloakAdminUser: "admin"
  keycloakAdminPassword: "<password-admin>"

backend:
  env:
    # Required together with global.appDbPassword.
    datasourceUsername: "app"
    jwtIssuerUrl: "https://your-domain/auth/realms/Nivel36"
    securityClientId: "janus-api"
    corsAllowedOrigins: "https://your-domain"

keycloak:
  hostname: "https://your-domain/auth"

nginx:
  ingress:
    enabled: true
    className: nginx
    hosts:
      - host: your-domain
        paths:
          - path: /
            pathType: Prefix
    tls:
      - secretName: janus-tls
        hosts:
          - your-domain
```

The production backend requires `backend.env.datasourceUsername`,
`global.appDbPassword`, `backend.env.jwtIssuerUrl`, and `backend.env.securityClientId`. The chart
passes these as `JANUS_DATASOURCE_USERNAME`, `JANUS_DATASOURCE_PASSWORD`, `JWT_ISSUER_URL`, and
`JANUS_SECURITY_CLIENT_ID`. Spring discovers the JWKS endpoint from the issuer's OIDC metadata; do
not configure a second issuer or a derived JWKS URL.

HTTPS is required when users access Janus through a non-local hostname or IP address. The frontend
uses secure-context browser APIs, including `crypto.randomUUID()`, which browsers do not expose to
pages served over plain HTTP. `http://localhost` remains valid for local development because
browsers treat loopback origins as potentially trustworthy.

Then install:

```bash
helm upgrade --install janus ./deploy/helm/janus -n janus --create-namespace -f values-prod.yaml
```


Realm source of truth: `deploy/helm/janus/files/realm-export.json`.
