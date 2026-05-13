from paddleocr import PaddleOCR
import numpy as np


class PaddleReader:
    def __init__(self):

        self.ocr = PaddleOCR(use_angle_cls=True, lang='en', show_log=False)

    def read_digits(self, image):
        result = self.ocr.ocr(image, cls=True)

        if not result or not result[0]:
            return None, 0.0

        texts = []
        confidences = []

        for line in result[0]:
            text_from_line = line[1][0]
            score_from_line = line[1][1]

            texts.append(text_from_line)
            confidences.append(score_from_line)

        all_text = "".join(texts)

        final_digits = ""
        for character in all_text:
            if character.isdigit():
                final_digits += character

        if len(confidences) > 0:
            average_confidence = float(np.mean(confidences))
        else:
            average_confidence = 0.0

        return final_digits, average_confidence