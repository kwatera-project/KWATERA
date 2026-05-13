from pydantic import BaseModel

class OcrResponse(BaseModel):
    readingValue: str | None
    confidence: float
    rawText: str
