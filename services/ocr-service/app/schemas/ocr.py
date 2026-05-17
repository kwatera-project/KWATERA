"""Pydantic schemas for the OCR service responses."""

from pydantic import BaseModel


class OcrResponse(BaseModel):
    """Schema definition for water meter OCR recognition response."""

    readingValue: str | None
    confidence: float
