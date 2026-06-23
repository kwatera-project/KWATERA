# KWATERA

**KWATERA** is a web-based system for holiday accommodation bookings, availability, utility settlements, payments, reporting, and administration. It also supports OCR-assisted meter readings, AI-assisted accommodation pricing, and LLM-supported newsletter recommendations for rental destinations.

> **[Open the GitHub Pages demo](https://kwatera-project.github.io/KWATERA/)**

The name expands to Polish: **K**ompleksowy **W**ebowy **A**systent **T**erminarza, **E**nergii, **R**ezerwacji i **A**dministracji.

## Architecture

KWATERA is a Docker Compose-based microservice application:

- a React and TypeScript frontend served by nginx;
- Spring Cloud API Gateway, Config Server, and Eureka service discovery;
- Spring Boot services for authentication, properties, reservations, billing, and AI pricing;
- a Python FastAPI service for OCR-assisted water meter readings;
- PostgreSQL with Flyway migrations and seeded demo data;
- Kafka for settlement status events and Mailpit for local email testing.

Architecture references:

- [Database diagram](docs/database/KWATERA_database-diagram.png)
- [Component diagram](docs/architecture/KWATERA_component-diagram.png)
- [System architecture diagram](docs/architecture/KWATERA_system-architecture.png)

## Run locally

### Requirements

For the normal full-stack startup, install:

- Docker with Docker Compose support;
- the two model assets described below.

Docker Compose starts PostgreSQL, Kafka, and Mailpit, so a local PostgreSQL installation is not required. Java 25, Maven, Bun, and Python 3.12 are only needed when developing or running quality checks outside Docker.

### Environment

Create `infra/compose/.env` from the provided template and replace the placeholder values:

```powershell
Copy-Item infra/compose/.env.example infra/compose/.env
```

On macOS or Linux:

```bash
cp infra/compose/.env.example infra/compose/.env
```

The required variable names are defined in [`infra/compose/.env.example`](infra/compose/.env.example). They cover PostgreSQL, JWT signing, Stripe, and the OpenAI-compatible Groq integration.

### Model assets

Model weights are distributed as release assets and must be placed in the expected paths before building the containers:

- [OCR YOLO model](https://github.com/kwatera-project/KWATERA/releases/tag/v1.0-ocr-yolo-model) → `services/ocr-service/models/digits.pt`
- [CatBoost pricing model](https://github.com/kwatera-project/KWATERA/releases/tag/v1.0-catboost-prediction-model) → `services/ai-pricing-service/src/main/resources/catboost_model_v1.cbm`

The AI pricing image copies the CatBoost file to `/app/catboost_model_v1.cbm`, which is the runtime path used by the service.

### Start the stack

Run from the repository root:

```bash
docker compose --env-file infra/compose/.env -f infra/compose/docker-compose.yml up --build
```

Then open [http://localhost:5173](http://localhost:5173).

## Demo accounts

All seeded users use the password `pass`.

| Role | Username | Email | Password |
| --- | --- | --- | --- |
| ADMIN | `admin` | `admin@example.com` | `pass` |
| OWNER | `owner1` | `owner1@example.com` | `pass` |
| OWNER | `owner2` | `owner2@example.com` | `pass` |
| OWNER | `owner3` | `owner3@example.com` | `pass` |
| OWNER | `owner4` | `owner4@example.com` | `pass` |
| OWNER | `owner5` | `owner5@example.com` | `pass` |
| OWNER | `owner6` | `owner6@example.com` | `pass` |
| OWNER | `owner7` | `owner7@example.com` | `pass` |
| OWNER | `owner8` | `owner8@example.com` | `pass` |
| OWNER | `owner9` | `owner9@example.com` | `pass` |
| OWNER | `owner10` | `owner10@example.com` | `pass` |
| GUEST | `guest1` | `guest1@example.com` | `pass` |
| GUEST | `guest2` | `guest2@example.com` | `pass` |
| GUEST | `guest3` | `guest3@example.com` | `pass` |
| GUEST | `guest4` | `guest4@example.com` | `pass` |
| GUEST | `guest5` | `guest5@example.com` | `pass` |
| GUEST | `guest6` | `guest6@example.com` | `pass` |
| GUEST | `guest7` | `guest7@example.com` | `pass` |
| GUEST | `guest8` | `guest8@example.com` | `pass` |
| GUEST | `guest9` | `guest9@example.com` | `pass` |
| GUEST | `guest10` | `guest10@example.com` | `pass` |

## Useful URLs

| Component | URL |
| --- | --- |
| Frontend | [http://localhost:5173](http://localhost:5173) |
| API Gateway health | [http://localhost:8090/actuator/health](http://localhost:8090/actuator/health) |
| Eureka dashboard | [http://localhost:8761](http://localhost:8761) |
| Config Server health | [http://localhost:8888/actuator/health](http://localhost:8888/actuator/health) |
| Mailpit UI | [http://localhost:8025](http://localhost:8025) |
| Auth Swagger UI | [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html) |
| Property Swagger UI | [http://localhost:8083/swagger-ui.html](http://localhost:8083/swagger-ui.html) |
| Reservation Swagger UI | [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) |
| Billing Swagger UI | [http://localhost:8086/swagger-ui.html](http://localhost:8086/swagger-ui.html) |
| AI Pricing Swagger UI | [http://localhost:8087/swagger-ui.html](http://localhost:8087/swagger-ui.html) |
| OCR FastAPI docs | [http://localhost:8085/docs](http://localhost:8085/docs) |
| Auth health | [http://localhost:8081/actuator/health](http://localhost:8081/actuator/health) |
| Property health | [http://localhost:8083/actuator/health](http://localhost:8083/actuator/health) |
| Reservation health | [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health) |
| Billing health | [http://localhost:8086/actuator/health](http://localhost:8086/actuator/health) |
| AI Pricing health | [http://localhost:8087/actuator/health](http://localhost:8087/actuator/health) |
| OCR health | [http://localhost:8085/health](http://localhost:8085/health) |

## Service documentation

- [Frontend](frontend/README.md)
- [API Gateway](services/api-gateway/README.md)
- [Auth Service](services/auth-service/README.md)
- [Config Server](services/config-server/README.md)
- [Service Registry](services/service-registry/README.md)
- [Property Service](services/property-service/README.md)
- [Reservation Service](services/reservation-service/README.md)
- [Billing Service](services/billing-service/README.md)
- [AI Pricing Service](services/ai-pricing-service/README.md)
- [OCR Service](services/ocr-service/README.md)
- [Database Migrations](services/db-migrations/README.md)

## Local quality checks

On Windows PowerShell, run the repository's pre-PR quality gate with:

```powershell
.\scripts\quality\pre-PR-check.ps1
```

The script derives all nine Java modules from the root Maven reactor and runs Spotless, Maven verification, and SpotBugs for each one. It also runs the OCR dependency, Ruff, pytest coverage, and Docker image checks, followed by the frontend Bun install, lint, and build. SonarQube Cloud analysis remains in GitHub Actions because it requires repository credentials and cloud project context.

Use the commands below when checking selected parts manually:

```bash
mvn clean verify
```

```bash
cd frontend
bun install --frozen-lockfile
bun run lint
bun run build
```

```bash
cd services/ocr-service
python -m pip install -r requirements-dev.txt
ruff check .
ruff format --check .
pytest --cov=app --cov-report=xml:coverage.xml --cov-report=term-missing -q
```

## Authors / Project Team

- **Zuzanna Adamczyk — AI/OCR & Catalog Engineer.** Developed the OCR module for automated utility readings and the property catalog. [GitHub](https://github.com/ZuzannaAdamczyk) · [LinkedIn](https://www.linkedin.com/in/zuzanna-adamczyk-26a56928a/)
- **Łukasz Jęcek — Tech Lead & System Architect.** Coordinated microservices architecture, CI/CD, system integration, and technical standards. [GitHub](https://github.com/lukaszjecek) · [LinkedIn](https://www.linkedin.com/in/lukasz-jecek)
- **Nadzeya Silchankava — Backend Domain & Payments Engineer.** Designed backend domain flows, billing, settlements, and Stripe payment integration. [GitHub](https://github.com/sinadzeya) · [LinkedIn](https://www.linkedin.com/in/nadzeya-silchankava/)
- **Alicja Świercz — Reservation Flow & UX Engineer.** Led reservation flow, frontend UX, dashboards, and checkout-related user flows. [GitHub](https://github.com/alicjaswiers) · [LinkedIn](https://www.linkedin.com/in/alicjaswiers/)

## Additional documentation

- [Contributing guide](CONTRIBUTING.md)
- [API and local demo notes](docs/stage-2-api-and-demo.md)
- [Demo flow](docs/stage-3-demo-flow.md)
- [CI quality pipeline](docs/ci/CI_QUALITY_PIPELINE.md)
