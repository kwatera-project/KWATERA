import {useRef, useState} from "react";
import {useTranslation} from "react-i18next"

export interface ImageUploadFormData {
    file: File | null;
    isMain: boolean;
}

interface ImageUploadFormProps {
    onSubmit: (data: { file: File; isMain: boolean }) => Promise<void>;
    submitLabel?: string;
}

export default function ImageUploadForm({
                                            onSubmit,
                                            submitLabel,
                                        }: ImageUploadFormProps) {
    const {t} = useTranslation();
    const buttonLabel = submitLabel ?? t("imageUpload.upload");
    const [file, setFile] = useState<File | null>(null);
    const [isMain, setIsMain] = useState<boolean>(false);
    const [loading, setLoading] = useState<boolean>(false);
    const [error, setError] = useState<string | null>(null);
    const fileInputRef = useRef<HTMLInputElement>(null);

    const handleCustomButtonClick = () => {
        fileInputRef.current?.click();
    };

    const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        if (e.target.files && e.target.files.length > 0) {
            const selectedFile = e.target.files[0];

            const allowedExtensions = ["image/jpeg", "image/jpg", "image/png"];
            if (!allowedExtensions.includes(selectedFile.type)) {
                setError(t('imageUpload.invalidFormat'));
                setFile(null);
                return;
            }

            setError(null);
            setFile(selectedFile);
        }
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!file) {
            setError(t('imageUpload.noFile'));
            return;
        }

        try {
            setLoading(true);
            setError(null);

            await onSubmit({ file, isMain });

            setFile(null);
            setIsMain(false);
            if (e.target instanceof HTMLFormElement) {
                e.target.reset();
            }
        } catch {
            setError(t('imageUpload.error'));
        } finally {
            setLoading(false);
        }
    };

    return (
        <form
            onSubmit={handleSubmit}
            className="space-y-5"
        >
            <h3 className="text-xl font-black text-[#1A1A1A] tracking-tight border-b border-[#DACDCA]/50 pb-2 mb-4">
                {t('imageUpload.title')}
            </h3>

            {error && (
                <div className="text-red-700 bg-red-50 p-3 rounded-lg text-sm font-semibold border border-red-200">
                    {error}
                </div>
            )}

            <div className="space-y-1">
                <label className="block text-xs font-bold text-[#7A7A7A] uppercase tracking-wider mb-1">
                    {t('imageUpload.label')}
                </label>

                <input
                    type="file"
                    ref={fileInputRef}
                    accept=".jpg,.jpeg,.png"
                    onChange={handleFileChange}
                    className="hidden"
                    required
                />

                <div className="flex items-center space-x-3 w-full border border-[#DACDCA] rounded-lg p-2 bg-white">
                    <button
                        type="button"
                        onClick={handleCustomButtonClick}
                        className="py-1.5 px-4 rounded-md text-xs font-bold uppercase tracking-wider bg-[#42211D] text-white hover:bg-[#5C2E29] transition-colors"
                    >
                        {t('imageUpload.chooseFile')}
                    </button>
                    <span className="text-sm font-semibold text-[#1A1A1A] truncate">
            {file ? file.name : t('imageUpload.noFile') }
        </span>
                </div>
            </div>

            <div className="flex items-center space-x-3 py-2">
                <input
                    type="checkbox"
                    id="isMain"
                    name="isMain"
                    checked={isMain}
                    onChange={(e) => setIsMain(e.target.checked)}
                    className="h-5 w-5 rounded border-[#DACDCA] text-[#42211D] focus:ring-[#42211D] cursor-pointer"
                />
                <label
                    htmlFor="isMain"
                    className="text-sm font-semibold text-[#1A1A1A] select-none cursor-pointer"
                >
                    {t('imageUpload.setMain')}
                </label>
            </div>

            <div className="pt-2">
                <button
                    type="submit"
                    disabled={loading || !file}
                    className="w-full md:w-auto px-6 py-3 bg-[#42211D] text-white font-bold hover:bg-[#5C2E29] text-sm rounded-lg transition-all border border-[#DACDCA] shadow-sm tracking-tight disabled:bg-gray-100 disabled:text-gray-400 disabled:border-gray-200 disabled:cursor-not-allowed disabled:shadow-none"
                >
                    {loading ? t('imageUpload.uploading') : buttonLabel}
                </button>
            </div>
        </form>
    );
}