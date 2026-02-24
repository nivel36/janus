# Janus

Janus is a **Spring/Angular**-based application for managing company time records. It facilitates the tracking of employees' working hours, helping to comply with labor regulations and optimize time management.

## Features

- **Time Logging**: Allows employees to easily record their clock-in and clock-out times.
- **User Management**: Administer profiles with different roles and permissions.
- **Custom Reports**: Generate detailed reports on worked hours, overtime, and absences.
- **Integration**: Compatible with other systems via RESTful APIs.

## Prerequisites

- **Java 25** or higher.
- A PostgreSQL relational database.

## Installation

1. **Clone the repository**:

```bash
git clone https://github.com/nivel36/janus.git
```

3. **Build the project**:

```bash
mvn clean install
```

4. **Start the app**:

```bash
java -jar Janus.jar
```

## Realm configuration (single source of truth)

Use **`janus-realm`** as the unique Keycloak realm across all components to keep token issuer validation aligned:

- Docker backend issuer: `deploy/docker/compose.yml` (`SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI`)
- Frontend Keycloak config: `apps/frontend/src/environments/environment.ts` and `environment.prod.ts`
- Imported Keycloak realm: `deploy/docker/keycloak/realm-export.json`

If you ever change the realm, update all three places in the same commit.

## Docker (quick start/stop)

From the project root, you can use these scripts:

```bash
./start.sh    # docker compose up -d --build
./stop.sh     # docker compose down
./restart.sh  # stop + start
```

## Usage

Access the application via a web browser:

```
http://localhost:8080/janus
```

## License

This project is licensed under the [Apache License 2.0](LICENSE).
