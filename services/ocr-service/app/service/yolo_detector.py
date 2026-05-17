"""Module for water meter digit detection using YOLO models."""

import logging

import cv2
import numpy as np
from ultralytics import YOLO

logger = logging.getLogger(__name__)

CONF_THRESHOLD = 0.10
IOU_THRESHOLD = 0.4
RED_RATIO_THRESH = 0.04


def _has_red(frame: np.ndarray, box_x: float, box_y: float, box_w: float, box_h: float) -> bool:
    """Check if the bounding box area contains a significant amount of red color."""
    fh, fw = frame.shape[:2]
    x1 = max(0, int(box_x - box_w / 2))
    y1 = max(0, int(box_y - box_h / 2))
    x2 = min(fw, int(box_x + box_w / 2))
    y2 = min(fh, int(box_y + box_h / 2))
    patch = frame[y1:y2, x1:x2]
    if patch.size == 0:
        return False
    hsv = cv2.cvtColor(patch, cv2.COLOR_BGR2HSV)
    mask1 = cv2.inRange(hsv, np.array([0, 50, 50]), np.array([10, 255, 255]))
    mask2 = cv2.inRange(hsv, np.array([160, 50, 50]), np.array([180, 255, 255]))
    total = patch.shape[0] * patch.shape[1]
    return (np.sum(mask1 > 0) + np.sum(mask2 > 0)) / total > RED_RATIO_THRESH


class YOLODetector:
    """Detector class responsible for running inference on water meter digits."""

    def __init__(self, model_path: str = "models/digits.pt")-> None:
        """Initialize the YOLO model with the given weights path."""
        self.model = YOLO(model_path)

    def read_digits_direct(self, frame: np.ndarray) -> tuple[str | None, float]:
        """Perform direct digit recognition and filter out red components."""
        results = self.model(
            frame, conf=CONF_THRESHOLD, iou=IOU_THRESHOLD, agnostic_nms=True, verbose=False
        )
        boxes = results[0].boxes
        if boxes is None or len(boxes) == 0:
            return None, 0.0

        fh, fw = frame.shape[:2]
        dets = []
        for b in boxes:
            x, y = float(b.xywh[0][0]), float(b.xywh[0][1])
            bw, bh = float(b.xywh[0][2]), float(b.xywh[0][3])
            if not _has_red(frame, x, y, bw, bh):
                dets.append({"x": x, "y": y, "digit": int(b.cls[0]), "conf": float(b.conf[0])})

        if not dets:
            return None, 0.0

        xs = []
        ys = []
        for d in dets:
            xs.append(d["x"])
            ys.append(d["y"])

        is_horizontal = (max(xs) - min(xs)) > (max(ys) - min(ys))
        dets.sort(key=lambda d: d["x"] if is_horizontal else -d["y"])

        result = ""
        confs = []
        for d in dets:
            result += str(d["digit"])
            confs.append(d["conf"])

        confidence = float(np.mean(confs))
        return result, confidence
