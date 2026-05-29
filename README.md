# KWATERA

**KWATERA** is a web-based system for managing holiday accommodation bookings, availability, utility settlements, payments, reporting, and administration.

The project name expands to Polish:

**K**ompleksowy **W**ebowy **A**systent **T**erminarza, **E**nergii, **R**ezerwacji i **A**dministracji

## Project Overview

KWATERA is being developed as a semester project focused on building a realistic accommodation management platform rather than a simple CRUD application. The system is intended to support the full reservation lifecycle, including guest booking flows, availability handling, deposit and payment tracking, final settlement of stays, and administrative supervision.

In addition to standard booking features, the project includes AI-assisted modules such as OCR-based meter reading from uploaded images, pricing or occupancy support based on historical reservation data, and weather-informed reservation analysis.

## Planned Scope

The target system is expected to include:

- guest-facing booking and availability search
- administration and reception workflows
- reservation status management
- deposits, payments, balances, and billing
- utility settlement based on meter readings
- notifications and reminders
- reporting and operational dashboards
- AI OCR for meter reading support
- predictive analytics for pricing or demand
- weather-based scoring support

## Target Architecture

The project is planned as a microservice-based solution with:

- Java Spring Boot as the main backend technology
- a separate Python AI OCR service
- PostgreSQL as the primary database
- Spring Cloud Eureka for service discovery
- Spring Cloud Config Server for centralized configuration
- JWT-based authentication and authorization in auth-service
- Docker and Docker Compose for local development and deployment
- OpenAPI-based API documentation

## Roles

The currently implemented roles in the authentication layer are:

- ADMIN
- OWNER
- GUEST

## Repository Workflow

The project is intended to be developed with a team workflow based on:

- feature branches
- pull requests
- code review
- CODEOWNERS
- GitHub Issues and Kanban-based task tracking

See [CONTRIBUTING.md](./CONTRIBUTING.md) for issue and pull request workflow conventions.

## Local run

Run the local development environment with Docker Compose:

```bash
docker compose -f infra/compose/docker-compose.yml up --build
```

### Local prerequisites

- Java 25
- Maven 3.9+
- Bun
- Docker with Compose support

### Local quality checks before pull request

Before opening a pull request, run local quality checks:

```powershell
.\scripts\quality\pre-PR-check.ps1
```

This script runs the current local quality gate for the backend services and the frontend application.

For Java services it runs:

- `spotless:check`
- `clean verify`
- `spotbugs:check`

For the frontend it runs:

- `bun install --frozen-lockfile`
- `bun run lint`
- `bun run build`

JaCoCo coverage reports are generated locally during `verify` and can be opened in a browser from:

- `services/config-server/target/site/jacoco/index.html`
- `services/service-registry/target/site/jacoco/index.html`
- `services/reservation-service/target/site/jacoco/index.html`

### Useful URLs after startup:

#### Backend
- Config Server health: [http://localhost:8888/actuator/health](http://localhost:8888/actuator/health)
- Eureka dashboard: [http://localhost:8761](http://localhost:8761)
- Reservation service ping: [http://localhost:8080/api/ping](http://localhost:8080/api/ping)
- Reservation service health: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)
- OCR service health: [http://localhost:8085/health](http://localhost:8085/health)

#### Frontend
- Vite dev server for React [http://localhost:5173](http://localhost:5173)

#### Local email testing
- Mailpit UI: [http://localhost:8025](http://localhost:8025)
- Mailpit SMTP: `localhost:1025`

Reservation and billing services send development email notifications through Mailpit. Docker Compose
sets `SPRING_MAIL_HOST=mailpit`, `SPRING_MAIL_PORT=1025`, empty SMTP credentials, and
`KWATERA_MAIL_FROM=no-reply@kwatera.local`.

To verify emails locally:

```bash
docker compose -f infra/compose/docker-compose.yml up --build
```

Open http://localhost:8025, then trigger one of the supported business events. The generated e-mail is sent to the event recipient and should appear in the Mailpit inbox.

| Event                               | Service               | Recipient source                                                         | Mail subject                 |
| ----------------------------------- | --------------------- | ------------------------------------------------------------------------ | ---------------------------- |
| Reservation created                 | `reservation-service` | Authenticated guest e-mail stored as `Reservation.guestEmail`            | `Reservation created`        |
| Reservation status changed          | `reservation-service` | `Reservation.guestEmail`                                                 | `Reservation status changed` |
| Settlement created / issued         | `billing-service`     | Guest e-mail resolved from reservation data                              | `Settlement issued`          |
| Payment / settlement status changed | `billing-service`     | `recipientEmail` propagated through Stripe metadata and webhook handling | `Payment status changed`     |

`KWATERA_MAIL_TEST_RECIPIENT` (`guest@kwatera.local` by default) is only a development fallback for flows where no recipient e-mail is available. Fallback usage is logged as a warning.


#### Auth / local testing

Current authentication endpoints:
- Auth service login: [http://localhost:8081/api/auth/login](http://localhost:8081/api/auth/login)
- Auth service register: [http://localhost:8081/api/auth/register](http://localhost:8081/api/auth/register)

#### OpenAPI / Swagger UI
- Auth Service Swagger UI: [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html)
- Property Service Swagger UI: [http://localhost:8083/swagger-ui.html](http://localhost:8083/swagger-ui.html)
- Reservation Service Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

> Demo users are seeded through database migrations for local testing.

## Additional documentation

- [CI quality pipeline](docs/ci/CI_QUALITY_PIPELINE.md)
- [Stage 2 API contract and local demo setup](docs/stage-2-api-and-demo.md)
- [OCR service](services/ocr-service/README.md)
