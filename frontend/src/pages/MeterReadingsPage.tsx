import { useCallback, useEffect, useState } from "react";
import { useParams, useSearchParams, Link } from "react-router-dom";
import MeterReadingUpload from "../components/MeterReadingUpload";
import { getMediaReadings } from "../api/ocrApi";
import type { MediaReadingStatus } from "../api/ocrApi";
import {useTranslation} from "react-i18next"

export default function MeterReadingsPage() {
    const { settlementId } = useParams();
    const [searchParams] = useSearchParams();

    const unitId = searchParams.get("unitId");
    const {t} = useTranslation();
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
            setError(err instanceof Error ? err.message : t('adminMeterReadings.loadError'));
        } finally {
            setLoading(false);
        }
    }, [settlementId, t]);

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
                    to="/my-reservations"
                    className="px-4 py-2 text-xs font-bold text-brand-primary bg-[#F7F7F7] border border-[#DACDCA] hover:bg-gray-100 rounded-lg transition-colors shadow-sm inline-flex items-center gap-1.5 w-fit self-start mb-6"
                >
                    &larr; {t('meterReadings.backToReservations')}
                </Link>
                <div className="bg-white border border-gray-200 rounded-xl shadow-sm p-6">
                    <p className="text-red-600 font-semibold">{t('adminMeterReadings.missingData')}</p>
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
                {t(`ocrStatuses.${status}`, { defaultValue: status })}
            </span>
        );
    };

    return (
        <div className="max-w-3xl mx-auto p-4 md:p-8 min-h-screen text-[#1A1A1A] space-y-6 flex flex-col">
            <Link
                to="/my-reservations"
                className="px-4 py-2 text-xs font-bold text-brand-primary bg-[#F7F7F7] border border-[#DACDCA] hover:bg-gray-100 rounded-lg transition-colors shadow-sm inline-flex items-center gap-1.5 w-fit self-start mb-6"
            >
                &larr; {t('meterReadings.backToReservations')}
            </Link>

            <div className="border-b border-[#DACDCA] pb-4">
                <h1 className="text-3xl font-bold text-[#1A1A1A] tracking-tight">
                    {t('meterReadings.title')}
                </h1>
                <p className="text-sm text-[#7A7A7A] mt-1">
                    {t('meterReadings.subtitle')}
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
                <p className="text-[#7A7A7A] font-medium animate-pulse">{t('adminMeterReadings.loading')}</p>
            ) : (
                <div className="bg-white border border-gray-200 rounded-xl shadow-sm p-6 hover:shadow-md transition-all duration-300 space-y-4">
                    <h2 className="text-lg font-bold text-[#1A1A1A] tracking-tight border-b border-gray-200 pb-3">
                        {t('adminMeterReadings.currentStatus')}
                    </h2>

                    {!waterReading ? (
                        <p className="text-sm text-[#7A7A7A] font-medium py-2">
                            {t('adminMeterReadings.noReading')}
                        </p>
                    ) : (
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-6 text-sm pt-2">
                            <div className="bg-[#F7F7F7] border border-[#DACDCA] rounded-xl p-4 space-y-3">
                                <p className="text-xs font-bold text-[#42211D] uppercase tracking-wider mb-1">{t('manualReservation.checkIn')}</p>
                                <div className="space-y-0.5">
                                    <p className="text-xs text-[#7A7A7A] font-semibold">{t('common.status')}</p>
                                    <div>{renderStatusBadge(waterReading.initialReadingStatus)}</div>
                                </div>
                                <div className="space-y-0.5">
                                    <p className="text-xs text-[#7A7A7A] font-semibold">{t('adminMeterReadings.reading')}</p>
                                    <p className="font-bold text-base text-[#1A1A1A]">{waterReading.initialReading ?? "-"}</p>
                                </div>
                                <div className="space-y-0.5">
                                    <p className="text-xs text-[#7A7A7A] font-semibold">{t('adminMeterReadings.confidence')}</p>
                                    <p className="font-semibold text-gray-800">{formatConfidence(waterReading.initialConfidenceScore)}</p>
                                </div>
                                <div className="space-y-0.5">
                                    <p className="text-xs text-[#7A7A7A] font-semibold">{t('adminMeterReadings.source')}</p>
                                    <p className="font-semibold text-gray-800">{waterReading.initialReadingSource ?? "-"}</p>
                                </div>
                            </div>

                            <div className="bg-[#F7F7F7] border border-[#DACDCA] rounded-xl p-4 space-y-3">
                                <p className="text-xs font-bold text-[#42211D] uppercase tracking-wider mb-1">{t('manualReservation.checkOut')}</p>
                                <div className="space-y-0.5">
                                    <p className="text-xs text-[#7A7A7A] font-semibold">{t('common.status')}</p>
                                    <div>{renderStatusBadge(waterReading.finalReadingStatus)}</div>
                                </div>
                                <div className="space-y-0.5">
                                    <p className="text-xs text-[#7A7A7A] font-semibold">{t('adminMeterReadings.reading')}</p>
                                    <p className="font-bold text-base text-[#1A1A1A]">{waterReading.finalReading ?? "-"}</p>
                                </div>
                                <div className="space-y-0.5">
                                    <p className="text-xs text-[#7A7A7A] font-semibold">{t('adminMeterReadings.confidence')}</p>
                                    <p className="font-semibold text-gray-800">{formatConfidence(waterReading.finalConfidenceScore)}</p>
                                </div>
                                <div className="space-y-0.5">
                                    <p className="text-xs text-[#7A7A7A] font-semibold">{t('adminMeterReadings.source')}</p>
                                    <p className="font-semibold text-gray-800">{waterReading.finalReadingSource ?? "-"}</p>
                                </div>
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
                <div className="flex items-center gap-3 p-4 bg-blue-50 border-l-4 border-blue-500 rounded-r-xl text-blue-700 animate-fade-in shadow-sm">
                    <svg className="w-5 h-5 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
                        <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clipRule="evenodd"/>
                    </svg>
                    <span className="text-xs font-semibold leading-relaxed">{t('meterReadings.checkInManualReview')}</span>
                </div>
            )}

            {waterReading?.finalReadingStatus === "REQUEST_MANUAL_REVIEW" && (
                <div className="flex items-center gap-3 p-4 bg-blue-50 border-l-4 border-blue-500 rounded-r-xl text-blue-700 animate-fade-in shadow-sm">
                    <svg className="w-5 h-5 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
                        <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clipRule="evenodd"/>
                    </svg>
                    <span className="text-xs font-semibold leading-relaxed">{t('meterReadings.checkOutManualReview')}</span>
                </div>
            )}
        </div>
    );
}
