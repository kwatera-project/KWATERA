"""Health endpoints for the OCR service."""

from fastapi import APIRouter

router = APIRouter(tags=["health"])


@router.get("/health")
def health() -> dict[str, str]:
    """Return the OCR service health status."""
    return {
        "status": "UP",
        "service": "ocr-service",
    }
