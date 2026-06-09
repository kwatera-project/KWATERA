import type { MediaReadingStatus, MediaReadingUploadAttempt, MeterReadingResponse } from "../api/ocrApi";

export const demoMediaReadingsBySettlementId: Record<string, MediaReadingStatus[]> = {
    "demo-settlement-5001": [
        {
            id: "demo-reading-water-1",
            settlementId: "demo-settlement-5001",
            utilityType: "WATER",
            initialReading: 128.4,
            finalReading: 141.2,
            initialConfidenceScore: 0.982,
            finalConfidenceScore: 0.947,
            initialReadingStatus: "AUTO_APPROVED",
            finalReadingStatus: "REQUEST_MANUAL_REVIEW",
            initialReadingSource: "OCR",
            finalReadingSource: "OCR",
        },
    ],
    "demo-settlement-5003": [
        {
            id: "demo-reading-water-3",
            settlementId: "demo-settlement-5003",
            utilityType: "WATER",
            initialReading: 54.1,
            finalReading: 75.1,
            initialConfidenceScore: 0.961,
            finalConfidenceScore: 0.973,
            initialReadingStatus: "AUTO_APPROVED",
            finalReadingStatus: "AUTO_APPROVED",
            initialReadingSource: "OCR",
            finalReadingSource: "OCR",
        },
    ],
};

export const demoMediaReadingAttemptsBySettlementId: Record<string, MediaReadingUploadAttempt[]> = {
    "demo-settlement-5001": [
        {
            id: "demo-attempt-initial-1",
            mediaReadingId: "demo-reading-water-1",
            imageBase64: null,
            ocrValue: "128.4",
            confidenceScore: 0.982,
            status: "AUTO_APPROVED",
            attemptedAt: "2026-06-18T13:10:00Z",
            readingType: "INITIAL",
        },
        {
            id: "demo-attempt-final-1",
            mediaReadingId: "demo-reading-water-1",
            imageBase64: null,
            ocrValue: "141.2",
            confidenceScore: 0.947,
            status: "REQUEST_MANUAL_REVIEW",
            attemptedAt: "2026-06-22T10:40:00Z",
            readingType: "FINAL",
        },
    ],
};

export const demoOcrSuccess: MeterReadingResponse = {
    status: "AUTO_APPROVED",
    message: "Demo OCR rozpoznał wskazanie licznika: 141.2 m3 (pewność 94.7%).",
};
