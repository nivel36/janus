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
    jwtIssuerUrl: "https://your-domain/auth/realms/janus-realm"
    springJwtIssuerUri: "https://your-domain/auth/realms/janus-realm"
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
```

Then install:

```bash
helm upgrade --install janus ./deploy/helm/janus -n janus --create-namespace -f values-prod.yaml
```


Realm source of truth: `deploy/keycloak/realm-export.json` (the chart file in `files/` points to this location).
