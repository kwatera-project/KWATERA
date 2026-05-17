import cv2
import numpy as np
import io
import logging
from PIL import Image, ImageOps
from pillow_heif import register_heif_opener

from app.service.yolo_detector import YOLODetector
from app.schemas.ocr import OcrResponse

logger = logging.getLogger(__name__)
register_heif_opener()


_detector = YOLODetector(model_path="models/digits.pt")


def process_meter_image(image_bytes: bytes) -> OcrResponse:
    try:
        pil_img = Image.open(io.BytesIO(image_bytes))
        pil_img = ImageOps.exif_transpose(pil_img)
        if pil_img.mode != "RGB":
            pil_img = pil_img.convert("RGB")
        image = cv2.cvtColor(np.array(pil_img), cv2.COLOR_RGB2BGR)
    except Exception as e:
        logger.error(f"Błąd dekodowania: {e}")
        return OcrResponse(readingValue=None, confidence=0.0)

    h, w = image.shape[:2]
    scale = min(1600 / w, 1600 / h)
    if scale < 1.0:
        image = cv2.resize(image, (int(w * scale), int(h * scale)),
                           interpolation=cv2.INTER_AREA)
    image = cv2.convertScaleAbs(image, alpha=1.1, beta=10)

    digits, confidence = _detector.read_digits_direct(image)

    if not digits or len(digits) < 3:
        return OcrResponse(readingValue=None, confidence=0.0)

    logger.info(f"Odczytano: '{digits}' (confidence={confidence:.2f})")
    return OcrResponse(readingValue=digits, confidence=confidence)
