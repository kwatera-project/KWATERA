import { Link, useNavigate, useParams } from "react-router-dom";
import {useEffect, useState} from "react";
import { getPropertyImages } from "../api/propertyApi.ts";
import { deletePropertyImage, setPropertyImageAsMain, uploadPropertyImage } from "../api/ownerPropertyApi.ts";
import ImageUploadForm from "../contexts/ImageUploadForm.tsx";
import { useTranslation } from "react-i18next";

interface PropertyImage {
    id: string;
    url: string;
    isMain: boolean;
}

export default function EditPropertyImages() {
    const { propertyId } = useParams();
    const navigate = useNavigate();
    const {t} = useTranslation();
    const [loading, setLoading] = useState(true);
    const [images, setImages] = useState<PropertyImage[]>([]);
    const [refreshKey, setRefreshKey] = useState(0);

    useEffect(() => {
        if (!propertyId) return;

        getPropertyImages(propertyId)
            .then(data => {
                if (Array.isArray(data)) {
                    setImages(data);
                } else {
                    setImages([]);
                }
            })
            .catch(error => {
                console.error("Failed to fetch property images", error);
            })
            .finally(() => setLoading(false));
    }, [propertyId, refreshKey]);

    const triggerRefresh = () => setRefreshKey(prev => prev + 1);

    const handleImageSubmit = async (data: { file: File; isMain: boolean }) => {
        if (!propertyId) return;

        try {
            await uploadPropertyImage(propertyId, data.file, data.isMain);
            alert(t('editPropertyImages.uploadSuccess'));
            navigate("/owner/properties");
        } catch (error) {
            console.error(error);
            alert(t('editPropertyImages.uploadFailed'));
        }
    };

    const handleDeleteImage = async (imageId: string) => {
        if (!propertyId) return;

        if (!confirm(t('editPropertyImages.deleteConfirm'))) {
            return;
        }

        try {
            await deletePropertyImage(propertyId, imageId);
            alert(t('editPropertyImages.deleteSuccess'));
            triggerRefresh();
        } catch (error) {
            console.error(error);
            alert(t('editPropertyImages.deleteFailed'));
        }
    };

    const handleSetMainImage = async (imageId: string) => {
        if (!propertyId) return;

        try {
            await setPropertyImageAsMain(propertyId, imageId, true);
            alert(t('editPropertyImages.mainSuccess'));
            triggerRefresh();
        } catch (error) {
            console.error(error);
            alert(t('editPropertyImages.mainFailed'));
        }
    };

    if (loading && images.length === 0) {
        return (
            <div className="p-8 max-w-7xl mx-auto min-h-screen text-[#7A7A7A] font-semibold text-sm">
                {t('editPropertyImages.loading')}...
            </div>
        );
    }

    return (
        <div className="p-4 sm:p-8 max-w-7xl mx-auto min-h-screen text-[#1A1A1A] space-y-6">
            <div>
                <Link
                    to="/owner/properties"
                    className="inline-flex items-center text-sm font-semibold text-[#7A7A7A] hover:text-[#1A1A1A] transition-colors mb-2"
                >
                    ← {t('createProperty.back')}
                </Link>
            </div>

            <div className="border-b border-[#DACDCA] pb-4 mb-6">
                <h1 className="text-3xl font-black text-[#1A1A1A] tracking-tight">
                    {t('editPropertyImages.title')}
                </h1>
                <p className="text-sm font-semibold text-[#7A7A7A] mt-1">
                    {t('editPropertyImages.subtitle')}
                </p>
            </div>

            <div className="max-w-4xl mx-auto space-y-10">

                <div className="bg-white border border-[#DACDCA] rounded-xl shadow-sm p-5 sm:p-8 mt-6">
                    <ImageUploadForm
                        onSubmit={handleImageSubmit}
                        submitLabel={t('editPropertyImages.uploadLabel')}
                    />
                </div>

                <div className="space-y-4">
                    <h2 className="text-xl font-black text-[#1A1A1A] tracking-tight uppercase tracking-wider text-xs font-bold text-[#7A7A7A]">
                        {t('editPropertyImages.currentGallery')}
                    </h2>

                    {images.length === 0 ? (
                        <div className="text-gray-500 italic py-8 text-center bg-white border border-[#DACDCA] rounded-xl shadow-sm">
                            {t('editPropertyImages.noImages')}
                        </div>
                    ) : (
                        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-6">
                            {images.map((img, index) => (
                                <div
                                    key={img?.id || index}
                                    className="relative bg-white border border-[#DACDCA] rounded-xl overflow-hidden shadow-sm flex flex-col justify-between hover:shadow-md transition-all duration-300"
                                >
                                    <div>
                                        <div className="relative border-b border-[#DACDCA]/30">
                                            <img
                                                src={img.url}
                                                alt="Property"
                                                className="w-full h-48 object-cover"
                                            />

                                            {img.isMain && (
                                                <span className="absolute top-3 left-3 bg-[#42211D] border border-[#DACDCA]/40 text-white text-xxs font-bold uppercase tracking-wider px-2.5 py-1 rounded-md shadow-sm">
                                                    {t('editPropertyImages.mainImage')}
                                                </span>
                                            )}
                                        </div>

                                        <div className="p-3 bg-white">
                                            <button
                                                disabled={img.isMain}
                                                onClick={() => img?.id && handleSetMainImage(img.id)}
                                                className={`w-full text-center py-2 rounded-lg text-xs font-bold uppercase tracking-wider transition-all border shadow-sm ${
                                                    img.isMain
                                                        ? "bg-gray-50 text-gray-400 border-gray-200 cursor-not-allowed shadow-none"
                                                        : "bg-white text-[#42211D] border-[#42211D] hover:bg-gray-50"
                                                }`}
                                            >
                                                {img.isMain ? t('editPropertyImages.activeMain') : t('editPropertyImages.setMain')}
                                            </button>
                                        </div>
                                    </div>

                                    <div className="p-3 bg-gray-50 border-t border-[#DACDCA]/30 flex justify-between items-center">
                                        <span className="text-xxs font-bold text-[#7A7A7A] uppercase tracking-wider truncate max-w-[140px]">
                                            ID: {img?.id ? `${img.id.substring(0, 8)}` : "N/A"}
                                        </span>
                                        <button
                                            onClick={() => img?.id && handleDeleteImage(img.id)}
                                            className="px-3 py-1 border border-red-200 bg-red-50 text-red-700 font-bold hover:bg-red-100 text-xs rounded-lg shadow-sm transition-all"
                                        >
                                            {t('editPropertyImages.delete')}
                                        </button>
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}