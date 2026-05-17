import io
from unittest.mock import patch

from PIL import Image

from app.service.ocr_service import process_meter_image


def make_img():
    img = Image.new("RGB", (100, 100), color=(128, 128, 128))
    buf = io.BytesIO()
    img.save(buf, format="JPEG")
    return buf.getvalue()


def make_big_img():
    img = Image.new("RGB", (2000, 2000), color=(128, 128, 128))
    buf = io.BytesIO()
    img.save(buf, format="JPEG")
    return buf.getvalue()


def test_digits_found():
    with patch("app.service.ocr_service._detector") as mock:
        mock.read_digits_direct.return_value = ("00087", 0.85)
        result = process_meter_image(make_img())
    assert result.readingValue == "00087"
    assert result.confidence == 0.85


def test_no_digits():
    with patch("app.service.ocr_service._detector") as mock:
        mock.read_digits_direct.return_value = (None, 0.0)
        result = process_meter_image(make_img())
    assert result.readingValue is None
    assert result.confidence == 0.0


def test_too_few_digits():
    with patch("app.service.ocr_service._detector") as mock:
        mock.read_digits_direct.return_value = ("12", 0.9)
        result = process_meter_image(make_img())
    assert result.readingValue is None


def test_invalid_file():
    result = process_meter_image(b"not_an_image")
    assert result.readingValue is None
    assert result.confidence == 0.0


def test_large_image():
    with patch("app.service.ocr_service._detector") as mock:
        mock.read_digits_direct.return_value = ("0836", 0.80)
        result = process_meter_image(make_big_img())
    assert result.readingValue == "0836"
