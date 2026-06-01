import { useState } from "react";
import heic2any from "heic2any";
import { uploadFinalMeterReading, uploadInitialMeterReading } from "../api/ocrApi";
import type { MeterReadingResponse, UtilityType } from "../api/ocrApi";

type Props = {
    settlementId: string;
    unitId: string;
    utilityType: UtilityType;
    readingType: "INITIAL" | "FINAL";
    onSuccess?: () => void;
};

export default function MeterReadingUpload({
   settlementId,
   unitId,
   utilityType,
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
                response = await uploadInitialMeterReading(
                    settlementId,
                    unitId,
                    utilityType,
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

    const fileInputId = `file-input-${readingType.toLowerCase()}-${utilityType.toLowerCase()}`;
    const utilityName = utilityType.charAt(0) + utilityType.slice(1).toLowerCase();

    return (
        <div className="bg-white border border-gray-200 rounded-xl shadow-sm p-6">
            <h3 className="font-bold text-lg text-brand-main tracking-tight mb-3">
                {readingType === "INITIAL" ? "Check-in" : "Check-out"} Meter Reading ({utilityName})
            </h3>

            <div className="flex items-center gap-3 mb-3">
                <label
                    htmlFor={fileInputId}
                    className="border border-gray-300 bg-white text-brand-main px-4 py-2 rounded-md hover:bg-gray-50 cursor-pointer inline-flex items-center gap-2 font-medium text-sm transition shadow-sm"
                >
                    <svg className="w-4.5 h-4.5 text-brand-muted" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12" />
                    </svg>
                    Choose file
                </label>
                <input
                    id={fileInputId}
                    type="file"
                    accept="image/jpeg,image/png,image/webp,image/heif,image/heic,.heif,.heic"
                    onChange={(e) => handleFileChange(e.target.files?.[0])}
                    className="hidden"
                />
                {file ? (
                    <span className="text-sm font-medium text-brand-main bg-brand-bg px-2.5 py-1.5 rounded-lg border border-brand-accent truncate max-w-xs" title={file.name}>
                        {file.name}
                    </span>
                ) : (
                    <span className="text-sm text-brand-muted">No file chosen</span>
                )}
            </div>

            <p className="text-xs text-brand-muted mb-4">
                Supported formats: JPG, PNG, WebP, HEIF, HEIC.
            </p>

            <button
                onClick={handleUpload}
                disabled={!file || loading}
                className={`text-sm font-semibold transition py-2 px-4 rounded-lg ${
                    !file || loading
                        ? "bg-brand-accent text-brand-main opacity-50 cursor-not-allowed"
                        : "bg-brand-primary text-white hover:opacity-90 cursor-pointer"
                }`}
            >
                {loading ? "Uploading..." : "Upload Photo"}
            </button>

            {error && <p className="mt-3 text-sm text-red-600 font-semibold">{error}</p>}

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
