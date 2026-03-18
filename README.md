# Janus

Janus is a time-tracking platform built as a monorepo with a **Spring Boot** backend and an **Angular** frontend. The backend already provides the business API, while the frontend is the Angular base from which the application UI is being built, with authentication delegated to **Keycloak**.

## Current architecture

The repository is organized into these main parts:

- `apps/backend`: Spring Boot 4 backend with REST API, PostgreSQL persistence, JWT validation, and business logic.
- `apps/frontend`: Angular 20 application base where the user-facing app is being developed.
- `deploy/docker`: Docker/Compose files for production-like and development infrastructure.
- `deploy/nginx`: Nginx configuration used in the containerized production-like setup.

## Project scope

### Frontend

The frontend should be understood as the **Angular application layer under construction**, not as a finished product.

At this stage, `apps/frontend` provides the base needed to keep building the app:

- Angular 20 project structure.
- Build and development scripts.
- Integration points for Keycloak authentication.
- Internationalization resources (`en`, `es`, `ca`).
- UI foundation for the future application experience.

### Backend API

The backend currently exposes endpoints for:

- `appusers`: application user preferences and settings.
- `employees`: employee management and worksite assignment.
- `worksites`: worksite catalog and timezone management.
- `schedules`: schedule definitions and schedule rules.
- `timelogs`: clock-in, clock-out, manual entries, searches, and duration queries.
- `clock-out-without-clock-in` event handling.

## Technology stack

- **Backend**: Java 25, Spring Boot 4, Spring Security, Spring Data JPA, Springdoc/OpenAPI.
- **Frontend**: Angular 20, PrimeNG, ngx-translate, Luxon, keycloak-js.
- **Auth**: Keycloak 26.
- **Database**: PostgreSQL 16.
- **Observability**: Grafana 12 in the Docker deployment.
- **Web server (containerized setup)**: Nginx.

## Prerequisites

Depending on how you want to run the project, you will need:

### Local development

- **Java 25**
- **Node.js 22** and **npm**
- **Docker/Compose** or **Podman Compose** for the supporting infrastructure

### Containerized production-like run

- **Docker Compose**, **docker-compose**, **podman compose**, or **podman-compose**

## Quick start

### Option 1: production-like containerized run

This mode builds the frontend and backend into containers and serves everything behind Nginx.

```bash
./start.sh
```

By default, the script uses:

- `deploy/docker/compose.yml`
- `deploy/docker/.env.prod`

To stop the environment:

```bash
./stop.sh
```

To restart it:

```bash
./restart.sh
```

Default URLs in this mode:

- App: `http://localhost:4200`
- Keycloak: `http://localhost:4200/auth`
- Grafana: `http://localhost:4200/grafana/`
- API base path: `http://localhost:4200/api/v1`

> Note: the exposed port comes from `deploy/docker/.env.prod` and is currently set to `4200`.

### Option 2: local development

This mode starts only the supporting services with Compose and runs frontend/backend directly from source.

#### 1. Start infrastructure

```bash
./start.sh -dev
```

This starts:

- PostgreSQL for the application on `localhost:5432`
- PostgreSQL for Keycloak on `localhost:5433`
- Keycloak on `http://localhost:8081/auth`
- Grafana on `http://localhost:3000`

#### 2. Run the backend

```bash
cd apps/backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

The backend runs on:

- API: `http://localhost:8080/api/v1`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- Health endpoint: `http://localhost:8080/actuator/health`

#### 3. Run the frontend

```bash
cd apps/frontend
npm ci
npm start
```

The Angular dev server runs on:

- Frontend: `http://localhost:4200`

## Authentication model

Authentication is **Keycloak-only**:

- The frontend handles login and logout against Keycloak.
- The backend is configured as an **OAuth2 Resource Server**.
- Protected `/api/**` endpoints require a valid bearer token.
- There is no backend logout REST endpoint.

## Realm configuration

Use **`janus-realm`** as the single source of truth across all components.

If the realm changes, update these files together:

- `deploy/docker/compose.yml`
- `apps/frontend/src/environments/environment.ts`
- `apps/frontend/src/environments/environment.prod.ts`
- `deploy/docker/keycloak/realm-export.json`

## Environment variables used in Docker

The containerized deployment supports overriding these values:

- `NGINX_PORT`
- `APP_DB_PASSWORD`
- `KEYCLOAK_DB_PASSWORD`
- `KEYCLOAK_ADMIN`
- `KEYCLOAK_ADMIN_PASSWORD`
- `JWT_ISSUER_URL`
- `SPRING_JWT_ISSUER_URI`
- `JANUS_CORS_ALLOWED_ORIGINS`

## Useful commands

### Backend

```bash
cd apps/backend
./mvnw test
./mvnw verify
./mvnw -Pcoverage verify
```

### Frontend

```bash
cd apps/frontend
npm ci
npm run build
npm test
```

## Obsolete information removed from previous versions of this README

The following are no longer correct and should not be used:

- Running `mvn clean install` from the repository root.
- Starting the application with `java -jar Janus.jar` from the repository root.
- Accessing the app at `http://localhost:8080/janus`.
- Describing the project as a generic “Spring/Angular” app without the current Keycloak, Nginx, Grafana, monorepo, and Angular 20/Spring Boot 4 setup.

## License

This project is licensed under the [Apache License 2.0](LICENSE).
