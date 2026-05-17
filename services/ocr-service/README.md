# KWATERA OCR Service

Python microservice skeleton for the KWATERA OCR module.

The service provides water meter digit recognition using a YOLO-based OCR pipeline exposed through a FastAPI HTTP API.

Current features:

- FastAPI OCR API,
- water meter digit detection using YOLO,
- image preprocessing and validation,
- support for JPG, PNG and HEIC/HEIF images,
- Docker and Docker Compose support.



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

Read Meter endpoint:

```txt
POST /ocr/read-meter
```

Example request: 

curl -X POST "http://127.0.0.1:8085/ocr/read-meter" -F "file=@tests/test1.jpg"

Expected response:

```json
{

  "readingValue": "0836",
  "confidence": 0.8400988936

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
