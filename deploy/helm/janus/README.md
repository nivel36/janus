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
    jwtIssuerUrl: "https://your-domain/auth/realms/Nivel36"
    springJwtIssuerUri: "https://your-domain/auth/realms/Nivel36"
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

HTTPS is required when users access Janus through a non-local hostname or IP address. The frontend
uses secure-context browser APIs, including `crypto.randomUUID()`, which browsers do not expose to
pages served over plain HTTP. `http://localhost` remains valid for local development because
browsers treat loopback origins as potentially trustworthy.

Then install:

```bash
helm upgrade --install janus ./deploy/helm/janus -n janus --create-namespace -f values-prod.yaml
```


Realm source of truth: `deploy/helm/janus/files/realm-export.json`.
