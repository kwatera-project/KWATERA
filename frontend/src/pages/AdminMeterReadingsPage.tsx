import { useCallback, useEffect, useState } from "react";
import { useParams, useSearchParams, Link } from "react-router-dom";
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
        const timeoutId = window.setTimeout(() => {
            void loadReadings();
        }, 0);

        return () => {
            window.clearTimeout(timeoutId);
        };
    }, [loadReadings]);

    if (!settlementId || !unitId) {
        return (
            <div className="max-w-3xl mx-auto p-8 min-h-screen text-[#1A1A1A] space-y-6">
                <Link
                    to="/admin/reservations"
                    className="px-4 py-2 text-xs font-bold text-[#42211D] bg-[#F7F7F7] border border-[#DACDCA] hover:bg-gray-100 rounded-lg transition-colors shadow-sm inline-flex items-center gap-1.5 w-fit"
                >
                    &larr; Back to Reservations Overview
                </Link>
                <div className="bg-white border border-[#DACDCA] rounded-xl shadow-sm p-6">
                    <p className="text-red-600 font-semibold">Missing settlement or unit data.</p>
                </div>
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

    const renderStatusBadge = (status: string) => {
        const isApproved = status.includes("APPROVED");
        const isReview = status.includes("REVIEW") || status.includes("MANUAL");
        const isReupload = status.includes("REUPLOAD") || status.includes("FAIL");
        
        return (
            <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-bold border tracking-wider uppercase ${
                isApproved ? 'bg-emerald-50 border-emerald-200 text-emerald-800' :
                isReview ? 'bg-amber-50 border-amber-200 text-amber-800' :
                isReupload ? 'bg-red-50 border-red-200 text-red-800' :
                'bg-gray-50 border-gray-200 text-gray-800'
            }`}>
                {status}
            </span>
        );
    };

    return (
        <div className="max-w-3xl mx-auto p-8 min-h-screen text-[#1A1A1A] space-y-6 flex flex-col">
            <Link
                to="/admin/reservations"
                className="px-4 py-2 text-xs font-bold text-[#42211D] bg-[#F7F7F7] border border-[#DACDCA] hover:bg-gray-100 rounded-lg transition-colors shadow-sm inline-flex items-center gap-1.5 w-fit"
            >
                &larr; Back to Reservations Overview
            </Link>

            <div className="border-b border-[#DACDCA] pb-4">
                <h1 className="text-3xl font-bold text-[#1A1A1A] tracking-tight">
                    Admin Water Meter Readings
                </h1>
                <p className="text-sm text-[#7A7A7A] mt-1">
                    Review OCR water readings and approve corrected values if needed.
                </p>
            </div>

            {error && (
                <div className="flex items-center gap-3 p-4 bg-red-50 border-l-4 border-red-500 rounded-r-xl text-red-700 animate-fade-in shadow-sm">
                    <svg className="w-5 h-5 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
                        <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clipRule="evenodd"/>
                    </svg>
                    <span className="text-sm font-semibold">{error}</span>
                </div>
            )}

            {loading ? (
                <p className="text-[#7A7A7A] font-medium animate-pulse">Loading readings...</p>
            ) : !waterReading ? (
                <div className="bg-white border border-[#DACDCA] rounded-xl shadow-sm p-6">
                    <p className="text-sm text-[#7A7A7A] font-medium py-2">
                        No water reading has been uploaded yet.
                    </p>
                </div>
            ) : (
                <div className="bg-white border border-[#DACDCA] rounded-xl shadow-sm p-6 hover:shadow-md transition-all duration-300 space-y-6">
                    <div>
                        <h2 className="text-lg font-bold text-[#1A1A1A] tracking-tight border-b border-[#DACDCA] pb-3 mb-4">
                            Current Water Reading Status
                        </h2>

                        <div className="grid grid-cols-1 md:grid-cols-2 gap-6 text-sm">
                            <div className="bg-[#F7F7F7] border border-[#DACDCA] rounded-xl p-4 space-y-3">
                                <p className="text-xs font-bold text-[#42211D] uppercase tracking-wider mb-1">Check-in</p>
                                <div className="space-y-0.5">
                                    <p className="text-xs text-[#7A7A7A] font-semibold">Status</p>
                                    <div>{renderStatusBadge(waterReading.initialReadingStatus)}</div>
                                </div>
                                <div className="space-y-0.5">
                                    <p className="text-xs text-[#7A7A7A] font-semibold">Reading</p>
                                    <p className="font-bold text-base text-[#1A1A1A]">{waterReading.initialReading ?? "-"}</p>
                                </div>
                                <div className="space-y-0.5">
                                    <p className="text-xs text-[#7A7A7A] font-semibold">Confidence</p>
                                    <p className="font-semibold text-gray-800">{formatConfidence(waterReading.initialConfidenceScore)}</p>
                                </div>
                                <div className="space-y-0.5">
                                    <p className="text-xs text-[#7A7A7A] font-semibold">Source</p>
                                    <p className="font-semibold text-gray-800">{waterReading.initialReadingSource ?? "-"}</p>
                                </div>
                            </div>

                            <div className="bg-[#F7F7F7] border border-[#DACDCA] rounded-xl p-4 space-y-3">
                                <p className="text-xs font-bold text-[#42211D] uppercase tracking-wider mb-1">Check-out</p>
                                <div className="space-y-0.5">
                                    <p className="text-xs text-[#7A7A7A] font-semibold">Status</p>
                                    <div>{renderStatusBadge(waterReading.finalReadingStatus)}</div>
                                </div>
                                <div className="space-y-0.5">
                                    <p className="text-xs text-[#7A7A7A] font-semibold">Reading</p>
                                    <p className="font-bold text-base text-[#1A1A1A]">{waterReading.finalReading ?? "-"}</p>
                                </div>
                                <div className="space-y-0.5">
                                    <p className="text-xs text-[#7A7A7A] font-semibold">Confidence</p>
                                    <p className="font-semibold text-gray-800">{formatConfidence(waterReading.finalConfidenceScore)}</p>
                                </div>
                                <div className="space-y-0.5">
                                    <p className="text-xs text-[#7A7A7A] font-semibold">Source</p>
                                    <p className="font-semibold text-gray-800">{waterReading.finalReadingSource ?? "-"}</p>
                                </div>
                            </div>
                        </div>
                    </div>

                    <div>
                        <h3 className="text-lg font-bold text-[#1A1A1A] tracking-tight border-b border-[#DACDCA] pb-3 mb-4">
                            Uploaded Meter Photos
                        </h3>

                        {attempts.length === 0 ? (
                            <p className="text-sm text-[#7A7A7A] font-medium py-2">
                                No upload attempts yet.
                            </p>
                        ) : (
                            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                                {attempts.map((attempt) => (
                                    <div
                                        key={attempt.id}
                                        className="bg-[#F7F7F7] border border-[#DACDCA] rounded-xl p-4 text-sm space-y-3"
                                    >
                                        <div className="flex justify-between items-center border-b border-[#DACDCA] pb-2">
                                            <p className="font-bold text-[#42211D]">
                                                {attempt.readingType === "INITIAL"
                                                    ? "Check-in"
                                                    : "Check-out"}
                                            </p>
                                            <p className="text-xs font-semibold text-[#7A7A7A]">
                                                {new Date(attempt.attemptedAt).toLocaleString()}
                                            </p>
                                        </div>

                                        {attempt.imageBase64 ? (
                                            <img
                                                src={`data:image/jpeg;base64,${attempt.imageBase64}`}
                                                alt="Meter upload attempt"
                                                className="w-full max-h-64 object-contain border border-[#DACDCA] rounded-lg bg-gray-50 shadow-sm"
                                            />
                                        ) : (
                                            <p className="text-xs text-[#7A7A7A] italic">
                                                No image available.
                                            </p>
                                        )}

                                        <div className="space-y-1 pt-1">
                                            <p className="text-xs font-bold text-[#7A7A7A] uppercase">Status: <span className="normal-case font-semibold text-[#1A1A1A] ml-1">{attempt.status}</span></p>
                                            <p className="text-xs font-bold text-[#7A7A7A] uppercase">OCR Value: <span className="font-mono font-bold text-[#1A1A1A] ml-1">{attempt.ocrValue ?? "-"}</span></p>
                                            <p className="text-xs font-bold text-[#7A7A7A] uppercase">Confidence: <span className="font-semibold text-[#1A1A1A] ml-1">{formatConfidence(attempt.confidenceScore)}</span></p>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>

                    {waterReading.initialReadingStatus === "REQUEST_MANUAL_REVIEW" && (
                        <div className="pt-4 border-t border-[#DACDCA]">
                            <MeterReadingApproval
                                settlementId={settlementId}
                                unitId={unitId}
                                utilityType="WATER"
                                readingType="INITIAL"
                                onSuccess={loadReadings}
                            />
                        </div>
                    )}

                    {waterReading.finalReadingStatus === "REQUEST_MANUAL_REVIEW" && (
                        <div className="pt-4 border-t border-[#DACDCA]">
                            <MeterReadingApproval
                                settlementId={settlementId}
                                unitId={unitId}
                                utilityType="WATER"
                                readingType="FINAL"
                                onSuccess={loadReadings}
                            />
                        </div>
                    )}
                </div>
            )}
        </div>
    );
}