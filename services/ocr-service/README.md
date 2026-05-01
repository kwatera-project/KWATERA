# KWATERA OCR Service

Python microservice skeleton for the KWATERA OCR module.

This service currently provides only the technical baseline:

- FastAPI application skeleton,
- health endpoint,
- Ruff linting and formatting configuration,
- pytest setup,
- Docker image build support.

Real OCR recognition logic is intentionally out of scope for now.

## Local setup

```bash
python -m venv .venv
source .venv/bin/activate
python -m pip install --upgrade pip
pip install -r requirements-dev.txt
```

On Windows PowerShell:

```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install --upgrade pip
pip install -r requirements-dev.txt
```

## Quality checks

Run from `services/ocr-service`:

```bash
ruff check .
ruff format --check .
pytest -q
```

If formatting check fails, run:

```bash
ruff format .
```

## Run locally

```bash
uvicorn app.main:app --host 0.0.0.0 --port 8085
```

Health endpoint:

```txt
GET /health
```

Expected response:

```json
{
  "status": "UP",
  "service": "ocr-service"
}
```

## Docker

From repository root:

```bash
docker build -f services/ocr-service/Dockerfile -t kwatera-ocr-service:local .
docker run --rm -p 8085:8085 kwatera-ocr-service:local
```

## Docker Compose

From repository root:

```bash
docker compose -f infra/compose/docker-compose.yml up --build ocr-service
```

Then open:

```txt
http://localhost:8085/health
```
