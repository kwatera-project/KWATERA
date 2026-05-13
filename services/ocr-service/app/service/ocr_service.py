import cv2
import numpy as np

from app.schemas.ocr import OcrResponse
from app.service.yolo_detector import YOLODetector
from app.service.paddle_reader import PaddleReader

yolo_detector = YOLODetector()
paddle_reader = PaddleReader()


def process_meter_image(image_bytes: bytes) -> OcrResponse:
    if not image_bytes:
        return OcrResponse(
            readingValue=None,
            confidence=0.0,
            rawText="",
        )

    image_array = np.frombuffer(image_bytes, dtype=np.uint8)
    image = cv2.imdecode(image_array, cv2.IMREAD_COLOR)

    if image is None:
        return OcrResponse(
            readingValue=None,
            confidence=0.0,
            rawText="",
        )

    cropped_meter = yolo_detector.find_meter(image)

    if cropped_meter is None:
        return OcrResponse(
            readingValue=None,
            confidence=0.0,
            rawText="",
        )

    digits, confidence = paddle_reader.read_digits(cropped_meter)

    if not digits:
        return OcrResponse(
            readingValue=None,
            confidence=0.0,
            rawText="",
        )

    return OcrResponse(
        readingValue=digits,
        confidence=confidence,
        rawText=digits,

    )