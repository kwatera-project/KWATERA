import type { MediaReadingStatus, MediaReadingUploadAttempt, MeterReadingResponse } from "../api/ocrApi";

const reading = (
    id: string,
    settlementId: string,
    finalReading: number,
    finalConfidenceScore: number
): MediaReadingStatus => ({
    id,
    settlementId,
    utilityType: "WATER",
    initialReading: 89.983,
    finalReading,
    initialConfidenceScore: 0.784642,
    finalConfidenceScore,
    initialReadingStatus: "AUTO_APPROVED",
    finalReadingStatus: "AUTO_APPROVED",
    initialReadingSource: "OCR",
    finalReadingSource: "OCR",
});

export const demoMediaReadingsBySettlementId: Record<string, MediaReadingStatus[]> = {
    "demo-settlement-0001": [
        reading("demo-reading-water-0001", "demo-settlement-0001", 92.383, 0.778662),
    ],
    "demo-settlement-0002": [
        reading("demo-reading-water-0002", "demo-settlement-0002", 95.583, 0.798311),
    ],
    "demo-settlement-0003": [
        reading("demo-reading-water-0003", "demo-settlement-0003", 92.383, 0.78607),
    ],
    "demo-settlement-0004": [
        reading("demo-reading-water-0004", "demo-settlement-0004", 90.383, 0.77504),
    ],
    "demo-settlement-0005": [
        reading("demo-reading-water-0005", "demo-settlement-0005", 92.983, 0.751107),
    ],
    "demo-settlement-0006": [
        reading("demo-reading-water-0006", "demo-settlement-0006", 91.583, 0.761524),
    ],
    "demo-settlement-0007": [
        reading("demo-reading-water-0007", "demo-settlement-0007", 91.183, 0.777656),
    ],
    "demo-settlement-0008": [
        reading("demo-reading-water-0008", "demo-settlement-0008", 90.583, 0.742221),
    ],
};

const attempt = (
    id: string,
    mediaReadingId: string,
    readingType: "INITIAL" | "FINAL",
    ocrValue: string,
    confidenceScore: number,
    attemptedAt: string
): MediaReadingUploadAttempt => ({
    id,
    mediaReadingId,
    imageBase64: null,
    ocrValue,
    confidenceScore,
    status: "AUTO_APPROVED",
    attemptedAt,
    readingType,
});

export const demoMediaReadingAttemptsBySettlementId: Record<string, MediaReadingUploadAttempt[]> = {
    "demo-settlement-0001": [
        attempt("demo-attempt-0001-water-initial", "demo-reading-water-0001", "INITIAL", "89.983000", 0.784642, "2026-01-18T12:00:00Z"),
        attempt("demo-attempt-0001-water-final", "demo-reading-water-0001", "FINAL", "92.383000", 0.778662, "2026-01-22T11:00:00Z"),
    ],
    "demo-settlement-0002": [
        attempt("demo-attempt-0002-water-initial", "demo-reading-water-0002", "INITIAL", "89.983000", 0.784642, "2026-02-17T12:00:00Z"),
        attempt("demo-attempt-0002-water-final", "demo-reading-water-0002", "FINAL", "95.583000", 0.798311, "2026-02-24T11:00:00Z"),
    ],
    "demo-settlement-0003": [
        attempt("demo-attempt-0003-water-initial", "demo-reading-water-0003", "INITIAL", "89.983000", 0.784642, "2026-03-19T12:00:00Z"),
        attempt("demo-attempt-0003-water-final", "demo-reading-water-0003", "FINAL", "92.383000", 0.78607, "2026-03-22T11:00:00Z"),
    ],
    "demo-settlement-0004": [
        attempt("demo-attempt-0004-water-initial", "demo-reading-water-0004", "INITIAL", "89.983000", 0.784642, "2026-04-18T12:00:00Z"),
        attempt("demo-attempt-0004-water-final", "demo-reading-water-0004", "FINAL", "90.383000", 0.77504, "2026-04-20T11:00:00Z"),
    ],
    "demo-settlement-0005": [
        attempt("demo-attempt-0005-water-initial", "demo-reading-water-0005", "INITIAL", "89.983000", 0.784642, "2026-05-03T12:00:00Z"),
        attempt("demo-attempt-0005-water-final", "demo-reading-water-0005", "FINAL", "92.983000", 0.751107, "2026-05-08T11:00:00Z"),
    ],
    "demo-settlement-0006": [
        attempt("demo-attempt-0006-water-initial", "demo-reading-water-0006", "INITIAL", "89.983000", 0.784642, "2026-05-18T12:00:00Z"),
        attempt("demo-attempt-0006-water-final", "demo-reading-water-0006", "FINAL", "91.583000", 0.761524, "2026-05-22T11:00:00Z"),
    ],
    "demo-settlement-0007": [
        attempt("demo-attempt-0007-water-initial", "demo-reading-water-0007", "INITIAL", "89.983000", 0.784642, "2026-05-28T12:00:00Z"),
        attempt("demo-attempt-0007-water-final", "demo-reading-water-0007", "FINAL", "91.183000", 0.777656, "2026-05-31T11:00:00Z"),
    ],
    "demo-settlement-0008": [
        attempt("demo-attempt-0008-water-initial", "demo-reading-water-0008", "INITIAL", "89.983000", 0.784642, "2026-06-07T12:00:00Z"),
        attempt("demo-attempt-0008-water-final", "demo-reading-water-0008", "FINAL", "90.583000", 0.742221, "2026-06-09T11:00:00Z"),
    ],
};

export const demoOcrSuccess: MeterReadingResponse = {
    status: "AUTO_APPROVED",
    message: "Demo OCR recognized a water meter reading with confidence aligned to the rich presentation seed.",
};
