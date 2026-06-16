# KWATERA

**KWATERA** is a web-based system for managing holiday accommodation bookings, availability, utility settlements, payments, reporting, and administration.

The project name expands to Polish:

**K**ompleksowy **W**ebowy **A**systent **T**erminarza, **E**nergii, **R**ezerwacji i **A**dministracji

KWATERA is a semester project focused on building a realistic accommodation management platform rather than a simple CRUD application. The current Stage 3 repository includes guest booking flows, availability handling, role-based access, utility settlement support, payment checkout integration, email notifications, OCR-assisted water meter readings, operational dashboards, and AI-assisted unit price suggestions.

## Quickstart

### Local prerequisites

- Java 25
- Maven 3.9+
- Bun
- Docker with Compose support

Run the local environment from the repository root:

```bash
docker compose -f infra/compose/docker-compose.yml up --build
```

For repeated local work, you can warm the Docker build cache without starting containers:

```bash
docker compose -f infra/compose/docker-compose.yml build
```

The Dockerfiles use BuildKit cache mounts for Maven, Bun, and pip package caches where supported by your Docker installation. The full-stack startup command above remains the canonical local run command.

Then open the frontend:

- [http://localhost:5173](http://localhost:5173)

## Target Architecture

The implemented local Stage 3 system is a Docker Compose based microservice application:

- React frontend served through the `frontend` container.
- Spring Cloud API Gateway routes frontend API calls to backend services.
- Spring Cloud Config Server loads shared configuration from `config-repo/`.
- Eureka Service Registry provides service discovery for Spring services.
- Java Spring Boot services handle authentication, property/catalog data, reservations, billing, settlements, and AI pricing.
- A Python FastAPI OCR service reads uploaded water meter images.
- PostgreSQL stores application data and Flyway migrations/seed data are applied by `db-migrations`.
- Kafka is used by billing and reservation services for settlement status events.
- Mailpit captures local reservation, settlement, and payment notification emails.
- OpenAPI/Swagger UI is available on services that include the Springdoc UI dependency, and FastAPI docs are available for the OCR service.

## Roles

The currently implemented roles in the authentication layer are:

- `ADMIN`
- `OWNER`
- `GUEST`

Seeded local demo users are created by database migrations. The known demo password is `pass`.

| Role | Email |
| --- | --- |
| Admin | `admin@example.com` |
| Owner | `owner1@example.com`, `owner2@example.com` |
| Guest | `guest1@example.com`, `guest2@example.com` |

## Repository Workflow

The project is developed with a team workflow based on feature branches, pull requests, code review, CODEOWNERS, GitHub Issues, and Kanban-based task tracking.

See [CONTRIBUTING.md](./CONTRIBUTING.md) for issue and pull request workflow conventions.

## Local Quality Checks Before Pull Request

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

JaCoCo coverage reports are generated locally during `verify` and can be opened in a browser from each Java service under:

```text
services/<service-name>/target/site/jacoco/index.html
```

## Useful URLs After Startup

### Platform

- Config Server health: [http://localhost:8888/actuator/health](http://localhost:8888/actuator/health)
- Eureka dashboard: [http://localhost:8761](http://localhost:8761)
- API Gateway health: [http://localhost:8090/actuator/health](http://localhost:8090/actuator/health)
- Frontend: [http://localhost:5173](http://localhost:5173)

### Services

- Auth Service login: [http://localhost:8081/api/auth/login](http://localhost:8081/api/auth/login)
- Property Service: [http://localhost:8083/actuator/health](http://localhost:8083/actuator/health)
- Reservation Service health: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)
- Reservation Service ping: [http://localhost:8080/api/ping](http://localhost:8080/api/ping)
- OCR Service health: [http://localhost:8085/health](http://localhost:8085/health)
- Billing Service: [http://localhost:8086/actuator/health](http://localhost:8086/actuator/health)
- AI Pricing Service: [http://localhost:8087/actuator/health](http://localhost:8087/actuator/health)

### Infrastructure

- Mailpit UI: [http://localhost:8025](http://localhost:8025)
- Mailpit SMTP: [smtp://localhost:1025](smtp://localhost:1025)
- PostgreSQL: [postgresql://localhost:5433/database](postgresql://localhost:5433/database)
- Kafka broker: `kafka:9092` for local containers; port `9092` is published on the host by Docker Compose.

## Local Email Testing

Reservation and billing services send development email notifications through Mailpit. Docker Compose sets `SPRING_MAIL_HOST=mailpit`, `SPRING_MAIL_PORT=1025`, empty SMTP credentials, and `KWATERA_MAIL_FROM=no-reply@kwatera.local`.

After local startup, generated emails can be inspected in the Mailpit UI at [http://localhost:8025](http://localhost:8025).

| Event | Service | Recipient source | Mail subject |
| --- | --- | --- | --- |
| Reservation created | `reservation-service` | Authenticated guest e-mail stored as `Reservation.guestEmail` | `Reservation created` |
| Reservation status changed | `reservation-service` | `Reservation.guestEmail` | `Reservation status changed` |
| Settlement created / issued | `billing-service` | Guest e-mail resolved from reservation data | `Settlement issued` |
| Payment / settlement status changed | `billing-service` | `recipientEmail` propagated through Stripe metadata and webhook handling | `Payment status changed` |

`KWATERA_MAIL_TEST_RECIPIENT` (`guest@kwatera.local` by default) is only a development fallback for flows where no recipient e-mail is available. Fallback usage is logged as a warning.

## Auth / Local Testing

Current authentication endpoints through the API Gateway:

- Login: [http://localhost:8090/api/auth/login](http://localhost:8090/api/auth/login)
- Register: [http://localhost:8090/api/auth/register](http://localhost:8090/api/auth/register)

Direct service URLs are also available during local development:

- Auth Service login: [http://localhost:8081/api/auth/login](http://localhost:8081/api/auth/login)
- Auth Service register: [http://localhost:8081/api/auth/register](http://localhost:8081/api/auth/register)

## OpenAPI / Swagger UI

- Auth Service Swagger UI: [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html)
- Property Service Swagger UI: [http://localhost:8083/swagger-ui.html](http://localhost:8083/swagger-ui.html)
- Reservation Service Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- Billing Service Swagger UI: [http://localhost:8086/swagger-ui.html](http://localhost:8086/swagger-ui.html)
- AI Pricing Service Swagger UI: [http://localhost:8087/swagger-ui.html](http://localhost:8087/swagger-ui.html)
- OCR Service FastAPI docs: [http://localhost:8085/docs](http://localhost:8085/docs)

## Additional Documentation

- [Stage 3 demo flow](docs/stage-3-demo-flow.md)
- [CI quality pipeline](docs/ci/CI_QUALITY_PIPELINE.md)
- [Stage 2 API contract and local demo setup](docs/stage-2-api-and-demo.md)
- [OCR service](services/ocr-service/README.md)
