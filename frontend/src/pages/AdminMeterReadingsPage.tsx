import { useCallback, useEffect, useState } from "react";
import { useParams, useSearchParams } from "react-router-dom";
import MeterReadingApproval from "../components/MeterReadingApproval";
import { getMediaReadings, getMediaReadingAttempts } from "../api/ocrApi";
import type { MediaReadingStatus, MediaReadingUploadAttempt } from "../api/ocrApi";

export default function AdminMeterReadingsPage() {
    const { settlementId } = useParams();
    const [searchParams] = useSearchParams();

    const unitId = searchParams.get("unitId");

    const [readings, setReadings] = useState<MediaReadingStatus[]>([]);
    const [attempts, setAttempts] = useState<MediaReadingUploadAttempt[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    const loadReadings = useCallback(async () => {
        if (!settlementId) return;

        setLoading(true);
        setError("");

        try {
            const data = await getMediaReadings(settlementId);
            setReadings(data);

            const attemptsData = await getMediaReadingAttempts(settlementId, "WATER");
            setAttempts(attemptsData);
        } catch (err: unknown) {
            setError(err instanceof Error ? err.message : "Failed to load readings");
        } finally {
            setLoading(false);
        }
    }, [settlementId]);

    useEffect(() => {
        loadReadings();
    }, [loadReadings]);

    if (!settlementId || !unitId) {
        return (
            <div className="p-6">
                <p className="text-red-600">Missing settlement or unit data.</p>
            </div>
        );
    }

    const waterReading = readings.find((reading) => reading.utilityType === "WATER");

    const formatConfidence = (confidence: number | null) => {
        if (confidence === null || confidence === undefined) {
            return "-";
        }

        return `${(confidence * 100).toFixed(2)}%`;
    };

    return (
        <div className="max-w-4xl mx-auto p-6 space-y-6">
            <div>
                <h1 className="text-2xl font-bold text-gray-800">
                    Admin water meter readings
                </h1>
                <p className="text-sm text-gray-500 mt-1">
                    Review OCR water readings and approve corrected values if needed.
                </p>
            </div>

            {error && (
                <div className="border border-red-200 bg-red-50 text-red-700 p-3 rounded">
                    {error}
                </div>
            )}

            {loading ? (
                <p className="text-gray-500">Loading readings...</p>
            ) : !waterReading ? (
                <div className="border rounded-xl p-4 bg-gray-50">
                    <p className="text-sm text-gray-500">
                        No water reading has been uploaded yet.
                    </p>
                </div>
            ) : (
                <div className="border rounded-xl p-4 bg-gray-50 space-y-4">
                    <h2 className="font-semibold text-gray-700">
                        Current water reading status
                    </h2>

                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
                        <div className="bg-white border rounded p-3">
                            <p className="font-medium text-gray-700">Check-in</p>
                            <p>Status: {waterReading.initialReadingStatus}</p>
                            <p>Reading: {waterReading.initialReading ?? "-"}</p>
                            <p>Confidence: {formatConfidence(waterReading.initialConfidenceScore)}</p>
                            <p>Source: {waterReading.initialReadingSource ?? "-"}</p>
                        </div>

                        <div className="bg-white border rounded p-3">
                            <p className="font-medium text-gray-700">Check-out</p>
                            <p>Status: {waterReading.finalReadingStatus}</p>
                            <p>Reading: {waterReading.finalReading ?? "-"}</p>
                            <p>Confidence: {formatConfidence(waterReading.finalConfidenceScore)}</p>
                            <p>Source: {waterReading.finalReadingSource ?? "-"}</p>
                        </div>
                    </div>

                    <div className="mt-6">
                        <h3 className="font-semibold text-gray-700 mb-3">
                            Uploaded meter photos
                        </h3>

                        {attempts.length === 0 ? (
                            <p className="text-sm text-gray-500">
                                No upload attempts yet.
                            </p>
                        ) : (
                            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                                {attempts.map((attempt) => (
                                    <div
                                        key={attempt.id}
                                        className="bg-white border rounded p-3 text-sm space-y-2"
                                    >
                                        <div className="flex justify-between gap-2">
                                            <p className="font-medium text-gray-700">
                                                {attempt.readingType === "INITIAL"
                                                    ? "Check-in"
                                                    : "Check-out"}
                                            </p>
                                            <p className="text-xs text-gray-500">
                                                {new Date(attempt.attemptedAt).toLocaleString()}
                                            </p>
                                        </div>

                                        {attempt.imageBase64 ? (
                                            <img
                                                src={`data:image/jpeg;base64,${attempt.imageBase64}`}
                                                alt="Meter upload attempt"
                                                className="w-full max-h-64 object-contain border rounded bg-gray-50"
                                            />
                                        ) : (
                                            <p className="text-xs text-gray-500">
                                                No image available.
                                            </p>
                                        )}

                                        <p>Status: {attempt.status}</p>
                                        <p>OCR value: {attempt.ocrValue ?? "-"}</p>
                                        <p>Confidence: {formatConfidence(attempt.confidenceScore)}</p>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>

                    {waterReading.initialReadingStatus === "REQUEST_MANUAL_REVIEW" && (
                        <MeterReadingApproval
                            settlementId={settlementId}
                            unitId={unitId}
                            utilityType="WATER"
                            readingType="INITIAL"
                            onSuccess={loadReadings}
                        />
                    )}

                    {waterReading.finalReadingStatus === "REQUEST_MANUAL_REVIEW" && (
                        <MeterReadingApproval
                            settlementId={settlementId}
                            unitId={unitId}
                            utilityType="WATER"
                            readingType="FINAL"
                            onSuccess={loadReadings}
                        />
                    )}
                </div>
            )}
        </div>
    );
}