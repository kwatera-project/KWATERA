import { useState } from "react";
import heic2any from "heic2any";
import { uploadFinalMeterReading, uploadInitialMeterReading } from "../api/ocrApi";
import type { MeterReadingResponse, UtilityType } from "../api/ocrApi";

type Props = {
    settlementId: string;
    unitId: string;
    utilityType: UtilityType;
    unitPrice?: number;
    readingType: "INITIAL" | "FINAL";
    onSuccess?: () => void;
};

export default function MeterReadingUpload({
   settlementId,
   unitId,
   utilityType,
   unitPrice,
   readingType,
   onSuccess,
}: Props) {
    const [file, setFile] = useState<File | null>(null);
    const [loading, setLoading] = useState(false);
    const [result, setResult] = useState<MeterReadingResponse | null>(null);
    const [error, setError] = useState("");

    const convertHeicToJpegIfNeeded = async (selectedFile: File): Promise<File> => {
        const fileName = selectedFile.name.toLowerCase();

        const isHeicOrHeif =
            selectedFile.type === "image/heic" ||
            selectedFile.type === "image/heif" ||
            fileName.endsWith(".heic") ||
            fileName.endsWith(".heif");

        if (!isHeicOrHeif) {
            return selectedFile;
        }

        const convertedBlob = await heic2any({
            blob: selectedFile,
            toType: "image/jpeg",
            quality: 0.9,
        });

        const jpegBlob = Array.isArray(convertedBlob)
            ? convertedBlob[0]
            : convertedBlob;

        return new File(
            [jpegBlob],
            selectedFile.name.replace(/\.(heic|heif)$/i, ".jpg"),
            { type: "image/jpeg" }
        );
    };

    const handleFileChange = async (selectedFile: File | undefined) => {
        if (!selectedFile) {
            setFile(null);
            return;
        }

        setError("");
        setResult(null);

        try {
            const preparedFile = await convertHeicToJpegIfNeeded(selectedFile);
            setFile(preparedFile);
        } catch {
            setFile(null);
            setError("Could not convert HEIF/HEIC image. Please upload JPG, PNG or WebP.");
        }
    };

    const handleUpload = async () => {
        if (!file) return;

        setLoading(true);
        setError("");
        setResult(null);

        try {
            let response: MeterReadingResponse;

            if (readingType === "INITIAL") {
                if (unitPrice === undefined || unitPrice < 0) {
                    throw new Error("Valid unit price is required for initial reading");
                }

                response = await uploadInitialMeterReading(
                    settlementId,
                    unitId,
                    utilityType,
                    unitPrice,
                    file
                );
            } else {
                response = await uploadFinalMeterReading(
                    settlementId,
                    unitId,
                    utilityType,
                    file
                );
            }

            setResult(response);

            if (onSuccess) {
                onSuccess();
            }
        } catch (err: unknown) {
            setError(err instanceof Error ? err.message : "Upload failed");
            if (onSuccess) onSuccess();
        } finally {
            setLoading(false);
        }
    };

    const statusColor = () => {
        if (!result) return "";

        if (result.status === "AUTO_APPROVED") {
            return "text-green-600 bg-green-50 border-green-200";
        }

        if (result.status === "REQUEST_REUPLOAD") {
            return "text-yellow-600 bg-yellow-50 border-yellow-200";
        }

        if (result.status === "REQUEST_MANUAL_REVIEW") {
            return "text-blue-600 bg-blue-50 border-blue-200";
        }

        return "text-gray-600";
    };

    return (
        <div className="border rounded-xl p-4 bg-white shadow-sm">
            <h3 className="font-semibold text-gray-700 mb-3">
                {readingType === "INITIAL" ? "Check-in" : "Check-out"} Meter Reading ({utilityType})
            </h3>

            <input
                type="file"
                accept="image/jpeg,image/png,image/webp,image/heif,image/heic,.heif,.heic"
                onChange={(e) => handleFileChange(e.target.files?.[0])}
                className="block w-full text-sm text-gray-500 mb-3 file:mr-3 file:py-1 file:px-3 file:rounded file:border-0 file:text-sm file:font-medium file:bg-blue-50 file:text-blue-700"
            />

            <p className="text-xs text-gray-500 mb-3">
                Supported formats: JPG, PNG, WebP, HEIF, HEIC.
            </p>

            <button
                onClick={handleUpload}
                disabled={!file || loading}
                className="px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded hover:bg-blue-700 disabled:bg-gray-300 disabled:cursor-not-allowed transition"
            >
                {loading ? "Uploading..." : "Upload Photo"}
            </button>

            {error && <p className="mt-3 text-sm text-red-600">{error}</p>}

            {result && (
                <div className={`mt-3 p-3 rounded border text-sm font-medium ${statusColor()}`}>
                    {result.message}
                    {result.status === "REQUEST_REUPLOAD" && (
                        <p className="mt-1 text-xs font-normal">
                            Please take a clearer photo and try again.
                        </p>
                    )}
                </div>
            )}
        </div>
    );
}