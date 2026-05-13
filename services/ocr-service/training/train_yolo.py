from ultralytics import YOLO

model = YOLO("yolov8s.pt")

model.train(
    data="datasets/water-meter/data.yaml",
    epochs=30,
    imgsz=640,
    batch=8,
    device="mps"
)