import { useCallback, useEffect, useState } from "react";
import { useParams, useSearchParams } from "react-router-dom";
import MeterReadingUpload from "../components/MeterReadingUpload";
import { getMediaReadings } from "../api/ocrApi";
import type { MediaReadingStatus } from "../api/ocrApi";

export default function MeterReadingsPage() {
    const { settlementId } = useParams();
    const [searchParams] = useSearchParams();

    const unitId = searchParams.get("unitId");
    const unitPriceParam = searchParams.get("unitPrice");

    const unitPrice = unitPriceParam ? Number(unitPriceParam) : 0;

    const [readings, setReadings] = useState<MediaReadingStatus[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    const loadReadings = useCallback(async () => {
        if (!settlementId) return;

        setLoading(true);
        setError("");

        try {
            const data = await getMediaReadings(settlementId);
            setReadings(data);
        } catch (err: unknown) {
            setError(err instanceof Error ? err.message : "Failed to load readings");
        } finally {
            setLoading(false);
        }
    }, [settlementId]);

    useEffect(() => {
        const timeoutId = window.setTimeout(() => {
            void loadReadings();
        }, 0);

        return () => {
            window.clearTimeout(timeoutId);
        };
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
                    Water meter readings
                </h1>
                <p className="text-sm text-gray-500 mt-1">
                    Upload water meter photos for check-in and check-out.
                </p>
            </div>

            {error && (
                <div className="border border-red-200 bg-red-50 text-red-700 p-3 rounded">
                    {error}
                </div>
            )}

            {loading ? (
                <p className="text-gray-500">Loading readings...</p>
            ) : (
                <div className="border rounded-xl p-4 bg-gray-50">
                    <h2 className="font-semibold text-gray-700 mb-3">
                        Current water reading status
                    </h2>

                    {!waterReading ? (
                        <p className="text-sm text-gray-500">
                            No water reading has been uploaded yet.
                        </p>
                    ) : (
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
                    )}
                </div>
            )}

            {(!waterReading ||
                waterReading.initialReadingStatus === "PENDING" ||
                waterReading.initialReadingStatus === "REQUEST_REUPLOAD") && (
                <MeterReadingUpload
                    settlementId={settlementId}
                    unitId={unitId}
                    utilityType="WATER"
                    unitPrice={unitPrice}
                    readingType="INITIAL"
                    onSuccess={loadReadings}
                />
            )}

            {waterReading &&
                (waterReading.initialReadingStatus === "AUTO_APPROVED" ||
                    waterReading.initialReadingStatus === "MANUALLY_APPROVED") &&
                (waterReading.finalReadingStatus === "PENDING" ||
                    waterReading.finalReadingStatus === "REQUEST_REUPLOAD") && (
                    <MeterReadingUpload
                        settlementId={settlementId}
                        unitId={unitId}
                        utilityType="WATER"
                        readingType="FINAL"
                        onSuccess={loadReadings}
                    />
                )}
            {waterReading?.initialReadingStatus === "REQUEST_MANUAL_REVIEW" && (
                <p className="mt-3 text-sm text-blue-700 bg-blue-50 border border-blue-200 rounded p-3">
                    Check-in reading was sent for manual review by the owner.
                </p>
            )}

            {waterReading?.finalReadingStatus === "REQUEST_MANUAL_REVIEW" && (
                <p className="mt-3 text-sm text-blue-700 bg-blue-50 border border-blue-200 rounded p-3">
                    Check-out reading was sent for manual review by the owner.
                </p>
            )}
        </div>
    );
}