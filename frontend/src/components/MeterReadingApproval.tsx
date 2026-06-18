import { useState } from "react";
import { approveMediaReading } from "../api/ocrApi";
import type { ReadingType, UtilityType } from "../api/ocrApi";
import {useTranslation} from "react-i18next"

type Props = {
    settlementId: string;
    unitId: string;
    utilityType: UtilityType;
    readingType: ReadingType;
    onSuccess?: () => void;
};

export default function MeterReadingApproval({
    settlementId,
    unitId,
    utilityType,
    readingType,
    onSuccess,
}: Props) {
    const [correctedReading, setCorrectedReading] = useState("");
    const [loading, setLoading] = useState(false);
    const [success, setSuccess] = useState(false);
    const [error, setError] = useState("");
    const {t} = useTranslation();

    const handleApprove = async () => {
        const value = parseFloat(correctedReading);
        if (isNaN(value) || value < 0) {
            setError(t('meterApproval.invalidValue'));
            return;
        }

        setLoading(true);
        setError("");

        try {
            await approveMediaReading(settlementId, unitId, utilityType, value, readingType);
            setSuccess(true);
            if (onSuccess) onSuccess();
        } catch (err: unknown) {
            setError(err instanceof Error ? err.message : t('meterApproval.failed'));
        } finally {
            setLoading(false);
        }
    };

    if (success) {
        return (
            <div className="border rounded-xl p-4 bg-green-50 border-green-200">
                <p className="text-green-700 font-medium text-sm">
                    ✓ {t('meterApproval.success', {
                    readingType: readingType === "INITIAL" ? t('meterApproval.initial') : t('meterApproval.final'),
                    utilityType: t(`utilityTypes.${utilityType}`, { defaultValue: utilityType })
                })}
                </p>
            </div>
        );
    }

    return (
        <div className="border rounded-xl p-4 bg-white shadow-sm">
            <h3 className="font-semibold text-gray-700 mb-1">
                {t('meterApproval.title')} {t(`utilityTypes.${utilityType}`, { defaultValue: utilityType })} ({readingType === "INITIAL" ? t('manualReservation.checkIn') : t('manualReservation.checkOut')})
            </h3>
            <p className="text-xs text-gray-500 mb-3">
                {t('meterApproval.description')}
            </p>

            <div className="flex gap-2 items-center">
                <input
                    type="number"
                    min="0"
                    step="0.001"
                    value={correctedReading}
                    onChange={(e) => setCorrectedReading(e.target.value)}
                    placeholder={t('meterApproval.placeholder')}
                    className="border rounded px-3 py-2 text-sm w-48 focus:outline-none focus:ring-2 focus:ring-blue-400"
                />
                <button
                    onClick={handleApprove}
                    disabled={!correctedReading || loading}
                    className="px-4 py-2 bg-green-600 text-white text-sm font-medium rounded hover:bg-green-700 disabled:bg-gray-300 disabled:cursor-not-allowed transition"
                >
                    {loading ? t('meterApproval.approving') : t('meterApproval.approve')}
                </button>
            </div>

            {error && (
                <p className="mt-2 text-sm text-red-600">{error}</p>
            )}
        </div>
    );
}
