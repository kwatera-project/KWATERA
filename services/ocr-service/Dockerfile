# syntax=docker/dockerfile:1.7
FROM python:3.12-slim

WORKDIR /app

COPY services/ocr-service/requirements.txt .

RUN apt-get update && apt-get install -y \
    libgl1 \
    libglib2.0-0 \
    libxcb1 \
    && rm -rf /var/lib/apt/lists/*

RUN --mount=type=cache,target=/root/.cache/pip,sharing=locked python -m pip install --upgrade pip \
    && pip install -r requirements.txt

COPY services/ocr-service/app ./app
COPY services/ocr-service/models ./models

EXPOSE 8085

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8085"]
