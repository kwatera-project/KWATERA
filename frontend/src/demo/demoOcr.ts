import type { MediaReadingStatus, MediaReadingUploadAttempt, MeterReadingResponse } from "../api/ocrApi";

const reading = (
    id: string,
    settlementId: string,
    utilityType: "WATER" | "ELECTRICITY",
    initialReading: number,
    finalReading: number | null,
    initialConfidenceScore: number,
    finalConfidenceScore: number | null,
    finalStatus: MediaReadingStatus["finalReadingStatus"] = "AUTO_APPROVED"
): MediaReadingStatus => ({
    id,
    settlementId,
    utilityType,
    initialReading,
    finalReading,
    initialConfidenceScore,
    finalConfidenceScore,
    initialReadingStatus: "AUTO_APPROVED",
    finalReadingStatus: finalStatus,
    initialReadingSource: "OCR",
    finalReadingSource: finalReading === null ? null : "OCR",
});

export const demoMediaReadingsBySettlementId: Record<string, MediaReadingStatus[]> = {
    "demo-settlement-5001": [
        reading("demo-reading-water-5001", "demo-settlement-5001", "WATER", 118.4, 126.8, 0.982, 0.961),
        reading("demo-reading-electricity-5001", "demo-settlement-5001", "ELECTRICITY", 2201.0, 2243.0, 0.974, 0.952),
    ],
    "demo-settlement-5002": [
        reading("demo-reading-water-5002", "demo-settlement-5002", "WATER", 54.1, 70.3, 0.961, 0.973),
        reading("demo-reading-electricity-5002", "demo-settlement-5002", "ELECTRICITY", 811.5, 878.5, 0.943, 0.936),
    ],
    "demo-settlement-5005": [
        reading("demo-reading-water-5005", "demo-settlement-5005", "WATER", 302.8, 307.0, 0.956, 0.941),
    ],
    "demo-settlement-5006": [
        reading("demo-reading-water-5006", "demo-settlement-5006", "WATER", 128.4, 141.2, 0.982, 0.947, "REQUEST_MANUAL_REVIEW"),
        reading("demo-reading-electricity-5006", "demo-settlement-5006", "ELECTRICITY", 3410.0, 3456.0, 0.966, 0.958),
    ],
    "demo-settlement-5008": [
        reading("demo-reading-water-5008", "demo-settlement-5008", "WATER", 76.2, 90.8, 0.934, 0.925),
        reading("demo-reading-electricity-5008", "demo-settlement-5008", "ELECTRICITY", 1433.0, 1491.0, 0.971, 0.964),
    ],
    "demo-settlement-5011": [
        reading("demo-reading-water-5011", "demo-settlement-5011", "WATER", 91.7, null, 0.964, null, "PENDING"),
        reading("demo-reading-electricity-5011", "demo-settlement-5011", "ELECTRICITY", 1764.0, null, 0.951, null, "PENDING"),
    ],
    "demo-settlement-5014": [
        reading("demo-reading-water-5014", "demo-settlement-5014", "WATER", 142.0, 151.7, 0.972, 0.938),
        reading("demo-reading-electricity-5014", "demo-settlement-5014", "ELECTRICITY", 3650.0, 3687.0, 0.953, 0.944),
    ],
};

const attempt = (
    id: string,
    mediaReadingId: string,
    readingType: "INITIAL" | "FINAL",
    ocrValue: string,
    confidenceScore: number,
    status: MediaReadingUploadAttempt["status"],
    attemptedAt: string
): MediaReadingUploadAttempt => ({
    id,
    mediaReadingId,
    imageBase64: null,
    ocrValue,
    confidenceScore,
    status,
    attemptedAt,
    readingType,
});

export const demoMediaReadingAttemptsBySettlementId: Record<string, MediaReadingUploadAttempt[]> = {
    "demo-settlement-5001": [
        attempt("demo-attempt-5001-water-initial", "demo-reading-water-5001", "INITIAL", "118.4", 0.982, "AUTO_APPROVED", "2026-05-05T15:10:00Z"),
        attempt("demo-attempt-5001-water-final", "demo-reading-water-5001", "FINAL", "126.8", 0.961, "AUTO_APPROVED", "2026-05-09T10:30:00Z"),
    ],
    "demo-settlement-5006": [
        attempt("demo-attempt-5006-water-initial", "demo-reading-water-5006", "INITIAL", "128.4", 0.982, "AUTO_APPROVED", "2026-06-18T13:10:00Z"),
        attempt("demo-attempt-5006-water-final", "demo-reading-water-5006", "FINAL", "141.2", 0.947, "REQUEST_MANUAL_REVIEW", "2026-06-22T10:40:00Z"),
    ],
    "demo-settlement-5011": [
        attempt("demo-attempt-5011-water-initial", "demo-reading-water-5011", "INITIAL", "91.7", 0.964, "AUTO_APPROVED", "2026-07-15T14:05:00Z"),
    ],
    "demo-settlement-5014": [
        attempt("demo-attempt-5014-water-initial", "demo-reading-water-5014", "INITIAL", "142.0", 0.972, "AUTO_APPROVED", "2026-07-04T16:20:00Z"),
        attempt("demo-attempt-5014-water-final", "demo-reading-water-5014", "FINAL", "151.7", 0.938, "AUTO_APPROVED", "2026-07-08T11:10:00Z"),
    ],
};

export const demoOcrSuccess: MeterReadingResponse = {
    status: "AUTO_APPROVED",
    message: "Demo OCR recognized a meter reading of 141.2 with 94.7% confidence.",
};
