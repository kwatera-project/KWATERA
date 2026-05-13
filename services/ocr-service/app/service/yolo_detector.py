from ultralytics import YOLO
import numpy as np

class YOLODetector:
    def __init__(self, model_path: str = "models/best.pt"):
        self.model = YOLO(model_path)

    def find_meter(self, frame):
        results = self.model(frame, verbose=False)
        boxes = results[0].boxes

        if boxes is None or len(boxes) == 0:
            return None

        coords = boxes.xyxy.cpu().numpy()

        x_min = int(np.min(coords[:, 0])) - 15
        y_min = int(np.min(coords[:, 1])) - 15

        x_max = int(np.max(coords[:, 2])) + 15
        y_max = int(np.max(coords[:, 3])) + 15

        x_min, y_min = max(0, x_min), max(0, y_min)
        x_max, y_max = min(frame.shape[1], x_max), min(frame.shape[0], y_max)

        cropped_area = frame[y_min:y_max, x_min:x_max]
        return cropped_area