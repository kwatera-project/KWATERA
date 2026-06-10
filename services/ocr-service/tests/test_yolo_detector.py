from unittest.mock import MagicMock, patch

import numpy as np

from app.service.yolo_detector import YOLODetector, _has_red


def make_frame(color):
    return np.full((100, 100, 3), color, dtype=np.uint8)


def test_has_red_gray_image():
    frame = make_frame([128, 128, 128])
    assert not _has_red(frame, 50, 50, 40, 40)


def test_has_red_red_image():
    frame = make_frame([0, 0, 200])
    assert _has_red(frame, 50, 50, 40, 40)


def test_has_red_empty_patch():
    frame = make_frame([128, 128, 128])
    assert not _has_red(frame, 50, 50, 0, 0)


def test_no_boxes_returns_none():
    with patch("app.service.yolo_detector.YOLO") as MockYOLO:
        mock_model = MagicMock()
        mock_result = MagicMock()
        mock_result.boxes = None
        mock_model.return_value = [mock_result]
        MockYOLO.return_value = mock_model

        detector = YOLODetector(model_path="models/digits.pt")
        result, conf = detector.read_digits_direct(make_frame([128, 128, 128]))

    assert result is None
    assert conf == 0.0


def test_empty_boxes_returns_none():
    with patch("app.service.yolo_detector.YOLO") as MockYOLO:
        mock_model = MagicMock()
        mock_result = MagicMock()
        mock_result.boxes = []
        mock_model.return_value = [mock_result]
        MockYOLO.return_value = mock_model

        detector = YOLODetector(model_path="models/digits.pt")
        result, conf = detector.read_digits_direct(make_frame([128, 128, 128]))

    assert result is None
    assert conf == 0.0


def test_all_red_boxes_return_decimal_value():
    with patch("app.service.yolo_detector._has_red", return_value=True):
        with patch("app.service.yolo_detector.YOLO") as MockYOLO:
            mock_model = MagicMock()
            mock_result = MagicMock()

            box = MagicMock()
            box.xywh = [[50, 50, 20, 20]]
            box.cls = [0]
            box.conf = [0.9]

            mock_result.boxes = [box]
            mock_model.return_value = [mock_result]
            MockYOLO.return_value = mock_model

            detector = YOLODetector(model_path="models/digits.pt")
            result, conf = detector.read_digits_direct(make_frame([128, 128, 128]))

    assert result == ".0"
    assert conf == 0.9


def test_horizontal_digits_sorted_left_to_right():
    with patch("app.service.yolo_detector._has_red", return_value=False):
        with patch("app.service.yolo_detector.YOLO") as MockYOLO:
            mock_model = MagicMock()
            mock_result = MagicMock()

            box1 = MagicMock()
            box1.xywh = [[20, 50, 10, 10]]
            box1.cls = [0]
            box1.conf = [0.9]

            box2 = MagicMock()
            box2.xywh = [[50, 50, 10, 10]]
            box2.cls = [8]
            box2.conf = [0.8]

            box3 = MagicMock()
            box3.xywh = [[80, 50, 10, 10]]
            box3.cls = [7]
            box3.conf = [0.85]

            mock_result.boxes = [box2, box3, box1]
            mock_model.return_value = [mock_result]
            MockYOLO.return_value = mock_model

            detector = YOLODetector(model_path="models/digits.pt")
            result, conf = detector.read_digits_direct(make_frame([128, 128, 128]))

    assert result == "087"