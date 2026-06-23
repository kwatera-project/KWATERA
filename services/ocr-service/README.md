[← Back to the project README](../../README.md)

# OCR Service

The OCR Service is a Python FastAPI application that reads digits from uploaded water meter images. It validates and preprocesses JPG, PNG, and HEIC/HEIF files before running a YOLO-based recognition pipeline.

## Main responsibilities

- Accept water meter image uploads.
- Validate and preprocess supported image formats.
- Detect meter digits and return a reading with confidence.
- Provide OCR results to the Billing Service.

## Default port

`8085`

## Useful local URLs

- Health: [http://localhost:8085/health](http://localhost:8085/health)
- FastAPI docs: [http://localhost:8085/docs](http://localhost:8085/docs)

## Configuration notes

Download the [OCR YOLO model release](https://github.com/kwatera-project/KWATERA/releases/tag/v1.0-ocr-yolo-model) and place the weights at:

```text
services/ocr-service/models/digits.pt
```

The Docker image copies the directory to `/app/models`, and the service loads `models/digits.pt`.

## Local verification

From this directory, after installing `requirements-dev.txt`:

```bash
pytest -q
```
