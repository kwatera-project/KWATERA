import { GATEWAY_BASE_URL } from "./apiConfig";

const BILLING_URL = `${GATEWAY_BASE_URL}/api/billing/media-readings`;

export type ReadingStatus =
    | "PENDING"
    | "AUTO_APPROVED"
    | "REQUEST_REUPLOAD"
    | "REQUEST_MANUAL_REVIEW"
    | "MANUALLY_APPROVED";

export type ReadingType = "INITIAL" | "FINAL";

export type UtilityType = "WATER";

export type MeterReadingResponse = {
    status: ReadingStatus;
    message: string;
};

export type ReadingSource = "OCR" | "MANUAL";

export type MediaReadingStatus = {
    id: string;
    settlementId: string;
    utilityType: UtilityType;
    initialReading: number | null;
    finalReading: number | null;
    initialConfidenceScore: number | null;
    finalConfidenceScore: number | null;
    initialReadingStatus: ReadingStatus;
    finalReadingStatus: ReadingStatus;
    initialReadingSource: ReadingSource | null;
    finalReadingSource: ReadingSource | null;
};

export type MediaReadingUploadAttempt = {
    id: string;
    mediaReadingId: string;
    imageBase64: string | null;
    ocrValue: string | null;
    confidenceScore: number | null;
    status: ReadingStatus;
    attemptedAt: string;
    readingType: ReadingType;
};

function getAuthHeaders(): HeadersInit {
    const token = localStorage.getItem("token");

    if (!token) {
        return {};
    }

    return {
        Authorization: `Bearer ${token}`,
    };
}

async function throwApiError(response: Response, fallbackMessage: string): Promise<never> {
    const text = await response.text();

    if (!text) {
        throw new Error(fallbackMessage);
    }

    try {
        const json = JSON.parse(text);
        throw new Error(json.message || json.error || fallbackMessage);
    } catch {
        throw new Error(text || fallbackMessage);
    }
}

export async function uploadInitialMeterReading(
    settlementId: string,
    unitId: string,
    utilityType: UtilityType,
    file: File
): Promise<MeterReadingResponse> {
    const formData = new FormData();

    formData.append("file", file);
    formData.append("unitId", unitId);
    formData.append("utilityType", utilityType);

    const response = await fetch(`${BILLING_URL}/${settlementId}/upload-initial`, {
        method: "POST",
        headers: getAuthHeaders(),
        body: formData,
    });

    if (!response.ok) {
        await throwApiError(response, "Failed to upload initial meter image");
    }

    return response.json();
}

export async function uploadFinalMeterReading(
    settlementId: string,
    unitId: string,
    utilityType: UtilityType,
    file: File
): Promise<MeterReadingResponse> {
    const formData = new FormData();

    formData.append("file", file);
    formData.append("unitId", unitId);
    formData.append("utilityType", utilityType);

    const response = await fetch(`${BILLING_URL}/${settlementId}/upload-final`, {
        method: "POST",
        headers: getAuthHeaders(),
        body: formData,
    });

    if (!response.ok) {
        await throwApiError(response, "Failed to upload final meter image");
    }

    return response.json();
}

export async function approveMediaReading(
    settlementId: string,
    unitId: string,
    utilityType: UtilityType,
    correctedReading: number,
    readingType: ReadingType
): Promise<void> {
    const params = new URLSearchParams({
        unitId,
        utilityType,
        correctedReading: correctedReading.toString(),
        readingType,
    });

    const response = await fetch(`${BILLING_URL}/${settlementId}/approve?${params}`, {
        method: "POST",
        headers: getAuthHeaders(),
    });

    if (!response.ok) {
        await throwApiError(response, "Failed to approve meter reading");
    }
}

export async function getMediaReadings(
    settlementId: string
): Promise<MediaReadingStatus[]> {
    const response = await fetch(`${BILLING_URL}/${settlementId}`, {
        method: "GET",
        headers: getAuthHeaders(),
    });

    if (!response.ok) {
        await throwApiError(response, "Failed to load media readings");
    }

    return response.json();
}

export async function getMediaReadingAttempts(
    settlementId: string,
    utilityType: UtilityType
): Promise<MediaReadingUploadAttempt[]> {
    const params = new URLSearchParams({
        utilityType,
    });

    const response = await fetch(`${BILLING_URL}/${settlementId}/attempts?${params}`, {
        method: "GET",
        headers: getAuthHeaders(),
    });

    if (!response.ok) {
        await throwApiError(response, "Failed to load meter reading upload attempts");
    }

    return response.json();
}
