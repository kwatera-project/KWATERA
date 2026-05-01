"""Application entrypoint for the KWATERA OCR service."""

from fastapi import FastAPI

from app.api.health import router as health_router


def create_app() -> FastAPI:
    """Create and configure the FastAPI application."""
    application = FastAPI(
        title="KWATERA OCR Service",
        version="0.1.0",
        description="OCR microservice skeleton for KWATERA.",
    )
    application.include_router(health_router)
    return application


app = create_app()
