from fastapi import APIRouter, File, UploadFile

from app.schemas.ocr import OcrResponse
from app.service.ocr_service import process_meter_image

router = APIRouter(prefix="/ocr", tags=["ocr"])

@router.post("/read-meter")
async def read_meter(file: UploadFile = File(...)) -> OcrResponse:
    image = await file.read()
    return process_meter_image(image)