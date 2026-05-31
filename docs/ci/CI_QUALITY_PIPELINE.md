[BACK TO README.MD](../../README.md)

# CI Quality Pipeline

This document describes the current quality pipeline used in the KWATERA repository.

## Purpose

The CI pipeline verifies code quality and build stability across the repository.

Its goals are:

- keep formatting and static analysis consistent across modules,
- detect build or test regressions early,
- run SonarCloud analysis where the workflow enables it,
- make backend reports available as downloadable workflow artifacts where applicable.

## Workflow Scope

The main CI workflow is defined in:

- `.github/workflows/ci.yml`

It runs on:

- `push` to `main`
- `pull_request` targeting `main` for `opened`, `synchronize`, and `reopened`

The workflow currently contains three quality paths:

- a frontend job for the application under `frontend/`,
- a Python job for `services/ocr-service`,
- a Java backend matrix job for Maven modules under `services/<module-name>`.

## Components Covered By CI

### Frontend Application

The frontend quality job covers:

```text
frontend/
```

### Python OCR Service

The Python quality job covers:

```text
services/ocr-service/
```

`ocr-service` is not part of the Java backend matrix because it is a Python FastAPI service.

### Java Backend Matrix

The Java backend CI matrix covers:

- `api-gateway`
- `auth-service`
- `config-server`
- `property-service`
- `reservation-service`
- `service-registry`
- `db-migrations`
- `billing-service`
- `ai-pricing-service`

Each Java module is executed from:

```text
services/<module-name>
```

## What CI Runs For The Frontend

For the frontend job, CI executes:

1. Checkout repository.
2. Set up Bun with `oven-sh/setup-bun@v2`.
3. Install dependencies:

   ```bash
   bun install --frozen-lockfile
   ```

4. Run lint:

   ```bash
   bun run lint
   ```

5. Build frontend:

   ```bash
   bun run build
   ```

The frontend job does not upload dedicated workflow report artifacts.

## What CI Runs For OCR Service

For `services/ocr-service`, CI executes:

1. Checkout repository with `fetch-depth: 0`.
2. Set up Python 3.12 with pip caching.
3. Install development dependencies:

   ```bash
   python -m pip install --upgrade pip
   pip install -r requirements-dev.txt
   ```

4. Run Ruff lint:

   ```bash
   ruff check .
   ```

5. Check Ruff formatting:

   ```bash
   ruff format --check .
   ```

6. Run pytest with coverage:

   ```bash
   pytest --cov=app --cov-report=xml:coverage.xml --cov-report=term-missing -q
   ```

7. Build the OCR Docker image:

   ```bash
   docker build -f Dockerfile -t kwatera-ocr-service:ci ../..
   ```

8. Run SonarQube Cloud analysis with `projectBaseDir: services/ocr-service`.

## What CI Runs For Each Java Backend Module

For every Java module in the matrix, CI executes:

1. Checkout repository with `fetch-depth: 0`.
2. Set up Temurin Java 25 with Maven dependency caching.
3. Cache Sonar scanner packages under `~/.sonar/cache`.
4. Make Maven Wrapper executable:

   ```bash
   chmod +x mvnw
   ```

5. Check formatting:

   ```bash
   ./mvnw -B -ntp spotless:check
   ```

6. Build, test, and generate coverage:

   ```bash
   ./mvnw -B -ntp clean verify
   ```

7. Run SpotBugs:

   ```bash
   ./mvnw -B -ntp spotbugs:check
   ```

8. Run SonarQube Cloud analysis when `sonar_enabled: true`:

   ```bash
   ./mvnw -B -ntp org.sonarsource.scanner.maven:sonar-maven-plugin:5.5.0.6356:sonar \
     -Dsonar.organization=${SONAR_ORGANIZATION} \
     -Dsonar.projectKey=<module-project-key> \
     -Dsonar.host.url=https://sonarcloud.io \
     -Dsonar.qualitygate.wait=true
   ```

9. Upload reports with `actions/upload-artifact@v4`.

The report upload step uses these paths:

```text
services/<module>/target/site/jacoco/**
services/<module>/target/surefire-reports/**
services/<module>/target/failsafe-reports/**
services/<module>/target/spotbugsXml.xml
```

Not every module generates every report type.

## SonarCloud Configuration

The current Java matrix sets `sonar_enabled: true` for every Java module listed below:

| Module | SonarCloud project key |
| --- | --- |
| `api-gateway` | `kwatera-project_KWATERA_api-gateway` |
| `auth-service` | `kwatera-project_KWATERA_auth-service` |
| `config-server` | `kwatera-project_KWATERA_config-server` |
| `property-service` | `kwatera-project_KWATERA_property-service` |
| `reservation-service` | `kwatera-project_KWATERA_reservation-service` |
| `service-registry` | `kwatera-project_KWATERA_service-registry` |
| `db-migrations` | `kwatera-project_KWATERA_db-migrations` |
| `billing-service` | `kwatera-project_KWATERA_billing-service` |
| `ai-pricing-service` | `kwatera-project_KWATERA_ai-pricing-service` |

The OCR service also runs SonarQube Cloud analysis through the Python quality job using the `services/ocr-service` project base directory.

No Java matrix entry currently has `sonar_enabled: false`.

## Meaning Of Java Matrix Fields

Each Java backend matrix entry contains:

- `service`: module directory name under `services/`.
- `sonar_enabled`: controls whether the conditional SonarQube Cloud Maven step runs.
- `sonar_project_key`: SonarCloud project key used by the Maven Sonar scanner.

## Reports Produced By CI

For Java backend modules, the workflow attempts to upload:

- JaCoCo coverage reports,
- Surefire test reports,
- Failsafe integration test reports,
- SpotBugs XML reports.

For the OCR service, pytest generates `coverage.xml` inside `services/ocr-service`, but the current workflow does not upload it as an artifact.

## Notes

- `db-migrations` is included in the Java quality matrix as a technical Maven module.
- `billing-service` and `ai-pricing-service` are Stage 3 Java services and are covered by the current Java matrix.
- `ocr-service` has its own Python quality path instead of being listed as a Java backend module.
- The backend matrix uses `fail-fast: false`, so one module failure does not stop the other matrix entries from running.
