[Back to the project README](../../README.md)

# CI Quality Pipeline

KWATERA uses two GitHub Actions workflows:

- `.github/workflows/ci.yml` runs code-quality checks on pushes and pull requests to `main`.
- `.github/workflows/frontend-pages-demo.yml` builds and deploys the static demo from `main` or a manual dispatch.

## Quality coverage

| Component | Technology | Maven reactor | `ci.yml` | Local pre-PR script | Docker Compose |
| --- | --- | --- | --- | --- | --- |
| `api-gateway` | Java | Yes | Yes | Yes | Yes |
| `auth-service` | Java | Yes | Yes | Yes | Yes |
| `config-server` | Java | Yes | Yes | Yes | Yes |
| `property-service` | Java | Yes | Yes | Yes | Yes |
| `reservation-service` | Java | Yes | Yes | Yes | Yes |
| `service-registry` | Java | Yes | Yes | Yes | Yes |
| `db-migrations` | Java | Yes | Yes | Yes | Yes |
| `billing-service` | Java | Yes | Yes | Yes | Yes |
| `ai-pricing-service` | Java | Yes | Yes | Yes | Yes |
| `ocr-service` | Python | No | Yes | Yes | Yes |
| `frontend` | React/TypeScript | No | Yes | Yes | Yes |

PostgreSQL, Kafka, and Mailpit are runtime infrastructure started by Docker Compose; they do not have separate code-quality jobs.

## Main CI workflow

The frontend job uses Bun and runs:

```bash
bun install --frozen-lockfile
bun run lint
bun run build
```

The OCR job uses Python 3.12 and runs dependency installation, Ruff lint and formatting checks, pytest with coverage, an OCR Docker image build, and SonarQube Cloud analysis.

Each of the nine Java modules is checked independently with Java 25:

```bash
./mvnw -B -ntp spotless:check
./mvnw -B -ntp clean verify
./mvnw -B -ntp spotbugs:check
```

The Java matrix then runs SonarQube Cloud analysis and uploads available JaCoCo, Surefire, Failsafe, and SpotBugs reports. The matrix uses `fail-fast: false`, so one module failure does not stop the remaining module jobs.

## GitHub Pages demo

The Pages workflow uses Node.js 22 and npm:

```bash
npm ci
npm run build
```

It sets `VITE_DEMO_MODE=true` and `VITE_BASE_PATH=/KWATERA/`, creates the single-page application fallback, and deploys `frontend/dist`.

## Local pre-PR script

On Windows PowerShell:

```powershell
.\scripts\quality\pre-PR-check.ps1
```

The script reads the root `pom.xml` and uses the locally installed Maven 3.9+ executable to run Spotless, Maven verification, and SpotBugs for every declared Maven module. This keeps its Java coverage aligned when reactor modules change.

For OCR it installs `requirements-dev.txt`, runs Ruff lint and formatting checks, runs pytest with the same coverage arguments as CI, and builds the OCR Docker image. For the frontend it runs `bun install --frozen-lockfile`, `bun run lint`, and `bun run build`.

The local script mirrors the executable Java, OCR, and frontend quality checks from `ci.yml`. SonarQube Cloud analysis and workflow artifact upload remain CI-only because they depend on GitHub repository credentials and workflow infrastructure.
