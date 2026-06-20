import { useParams, Link } from "react-router-dom";
import { useEffect, useState } from "react";
import { getProperty } from "../api/propertyApi.ts";
import type { Property, Unit } from "../types/property";
import {deleteUnit, getPropertyUnits, updateUnit} from "../api/ownerUnitApi.ts";
import { getPredictedPrice } from "../api/predictionApi.ts";
import { Sparkles } from "lucide-react";
import {useTranslation} from "react-i18next";
import { toast } from "react-hot-toast";

export default function OwnerPropertyUnitsPage() {
    const { propertyId } = useParams();
    const {t} = useTranslation();
    const [property, setProperty] = useState<Property>();
    const [units, setUnits] = useState<Unit[]>([]);

    useEffect(() => {
        if (!propertyId) return;

        getProperty(propertyId).then(setProperty).catch(console.error);
        getPropertyUnits(propertyId).then(setUnits).catch(console.error);
    }, [propertyId]);

    const handleDelete = async (propetryId: string, unitId: string) => {
        const confirmed = window.confirm( t('ownerUnits.deleteConfirm'));
        if (!confirmed) return;

        try {
            await deleteUnit(propetryId, unitId);

            setUnits(prev =>
                prev.filter(p => p.id !== unitId)
            );
        } catch (error) {

            const err = error as { status?: number; message?: string };

            if (err.status === 409) {
                toast.error(t('ownerUnits.deleteHasReservations'));
                return;
            }

            toast.error(t('ownerUnits.deleteFailed'));
        }
    };

    return (
        <div className="p-4 sm:p-8 max-w-7xl mx-auto min-h-screen text-[#1A1A1A] space-y-6">
            <div>
                <Link to="/owner/properties" className="inline-flex items-center text-sm font-bold text-[#7A7A7A] hover:text-[#1A1A1A] transition-colors mb-4">
                    ← {t('createProperty.back')}
                </Link>
            </div>

            <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center border-b border-[#DACDCA] pb-4 mb-6 gap-4">
                <div>
                    <h1 className="text-3xl font-black text-[#1A1A1A] tracking-tight">{property?.title || t('ownerUnits.loading')}</h1>
                    <p className="text-sm font-semibold text-[#7A7A7A] uppercase tracking-wider mt-1.5">{t('ownerUnits.subtitle')}</p>
                </div>

                <Link
                    to={`/owner/properties/${propertyId}/units/new`}
                    className="px-5 py-2.5 bg-[#42211D] text-white font-bold hover:bg-[#5C2E29] text-sm rounded-lg transition-colors border border-[#DACDCA] shadow-sm shrink-0"
                >
                    {t('ownerUnits.addUnit')}
                </Link>
            </div>

            <div className="space-y-6">
                {units.map(unit => (
                    <UnitCard
                        key={unit.id}
                        unit={unit}
                        propertyId={propertyId!}
                        onDelete={handleDelete}
                    />
                ))}
                {units.length === 0 && (
                    <div className="text-gray-500 italic py-8 text-center bg-white border border-[#DACDCA] rounded-xl shadow-sm">
                        {t('ownerUnits.noUnits')}
                    </div>
                )}
            </div>
        </div>
    );
}

function UnitCard({ unit, propertyId, onDelete }: { unit: Unit, propertyId: string, onDelete: (propertyId: string, unitId: string) => void }) {
    const [predictedPrice, setPredictedPrice] = useState<number | null>(null);
    const [loadingPrediction, setLoadingPrediction] = useState(true);
    const [currentPrice, setCurrentPrice] = useState<number>(unit.pricePerNight);
    const [isUpdatingPrice, setIsUpdatingPrice] = useState(false);
    const [modalConfig, setModalConfig] = useState<{ isOpen: boolean; type: "success" | "error"; title: string; description: string } | null>(null);
    const {t} = useTranslation();
    useEffect(() => {
        getPredictedPrice(propertyId, unit.id, undefined)
            .then((price) => {
                setPredictedPrice(price);
            })
            .catch((e) => {
                console.error(`Prediction error for unit ${unit.id}:`, e);
            })
            .finally(() => {
                setLoadingPrediction(false);
            });
    }, [propertyId, unit.id]);

    const handleApplySuggestedPrice = async () => {
        if (predictedPrice === null || isUpdatingPrice) return;

        setIsUpdatingPrice(true);
        try {
            await updateUnit(propertyId, unit.id, { pricePerNight: predictedPrice });

            setCurrentPrice(predictedPrice);
            setModalConfig({
                isOpen: true,
                type: "success",
                title: t('ownerUnits.priceUpdated'),
                description: t('ownerUnits.priceUpdatedDesc')
            });
        } catch (error) {
            console.error("Failed to update price:", error);
            setModalConfig({
                isOpen: true,
                type: "error",
                title: t('ownerUnits.priceUpdateFailed'),
                description: t('ownerUnits.priceUpdateFailedDesc')
            });
        } finally {
            setIsUpdatingPrice(false);
        }
    };

    return (
        <div className="bg-white border border-[#DACDCA] rounded-xl shadow-sm p-6 hover:shadow-md transition-all duration-300 flex flex-col lg:flex-row justify-between items-start lg:items-center gap-6">
            <div className="space-y-4 flex-grow w-full">
                <div>
                    <h2 className="font-black text-xl text-[#1A1A1A] tracking-tight">{unit.name}</h2>
                    <p className="text-[#7A7A7A] text-sm font-medium mt-1">{unit.description}</p>
                </div>

                <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 pt-2 text-xs font-bold text-gray-500 border-t border-gray-50/50">
                    <div>
                        <span className="block text-xxs uppercase tracking-wider text-[#7A7A7A] mb-0.5">{t('ownerUnits.unitType')}</span>
                        <span className="text-sm font-bold text-[#1A1A1A]">{unit.unitType}</span>
                    </div>
                    <div>
                        <span className="block text-xxs uppercase tracking-wider text-[#7A7A7A] mb-0.5">{t('ownerUnits.unitNumberShort')}</span>
                        <span className="text-sm font-bold text-[#1A1A1A]">{unit.unitNumber}</span>
                    </div>
                    <div>
                        <span className="block text-xxs uppercase tracking-wider text-[#7A7A7A] mb-0.5">{t('ownerUnits.floorLevel')}</span>
                        <span className="text-sm font-bold text-[#1A1A1A]">{unit.floor}</span>
                    </div>
                    <div>
                        <span className="block text-xxs uppercase tracking-wider text-[#7A7A7A] mb-0.5">{t('ownerUnits.guestCapacity')}</span>
                        <span className="text-sm font-bold text-[#1A1A1A]">{unit.capacity} {t('ownerUnits.guests')}</span>
                    </div>
                </div>

                <div className="pt-4 border-t border-gray-100 mt-4 flex flex-wrap items-end gap-6">
                    <div>
                        <span className="block text-xs font-semibold text-gray-500 uppercase tracking-wider mb-1">{t('ownerUnits.currentPrice')}</span>
                        <div className="text-sm font-bold text-gray-500">
                            <span className="text-xl font-black text-stone-900">{currentPrice.toFixed(2)} PLN</span> {t('ownerUnits.perNight')}
                        </div>
                    </div>

                    {loadingPrediction ? (
                        <div>
                            <span className="block text-xs font-semibold text-gray-500 uppercase tracking-wider mb-1">{t('ownerUnits.suggestedPrice')}</span>
                            <span className="text-gray-400 text-sm animate-pulse font-semibold block py-1">{t('ownerUnits.calculating')}</span>
                        </div>
                    ) : predictedPrice !== null ? (
                        <>
                            <div>
                                <span className="block text-xs font-semibold text-gray-500 uppercase tracking-wider mb-1">{t('ownerUnits.suggestedPrice')}</span>
                                <div className="text-sm font-bold text-gray-500">
                                    <span className="text-xl font-black text-stone-900">{predictedPrice.toFixed(2)} PLN</span>
                                </div>
                            </div>

                            {Math.abs(currentPrice - predictedPrice) > 0.01 && (
                                <button
                                    onClick={handleApplySuggestedPrice}
                                    disabled={isUpdatingPrice}
                                    className="flex items-center justify-center gap-2 px-4 py-2 border border-stone-300 bg-white text-stone-700 hover:bg-stone-50 text-sm font-bold rounded-md shadow-sm transition-colors disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer"
                                >
                                    {isUpdatingPrice ? (
                                        <>
                                            <span className="w-4 h-4 rounded-full border-2 border-stone-300 border-t-stone-700 animate-spin inline-block shrink-0" />
                                            <span>{t('ownerUnits.applying')}</span>
                                        </>
                                    ) : (
                                        <>
                                            <Sparkles className="w-[18px] h-[18px] text-stone-500" />
                                            <span>{t('ownerUnits.applyAiPrice')}</span>
                                        </>
                                    )}
                                </button>
                            )}
                        </>
                    ) : (
                        <div>
                            <span className="block text-xs font-semibold text-gray-500 uppercase tracking-wider mb-1">{t('ownerUnits.suggestedPrice')}</span>
                            <span className="text-amber-600 text-sm font-semibold block py-1">{t('ownerUnits.unavailable')}</span>
                        </div>
                    )}
                </div>
            </div>

            <div className="flex flex-wrap gap-2.5 w-full lg:w-auto justify-start sm:justify-end border-t border-gray-100 lg:border-none pt-4 lg:pt-0 shrink-0">
                <Link
                    to={`/owner/properties/${propertyId}/units/${unit.id}/images`}
                    className="px-4 py-2 border border-gray-300 bg-white text-gray-700 font-bold hover:bg-gray-50 text-sm rounded-lg shadow-sm transition-all inline-flex items-center justify-center flex-grow sm:flex-grow-0 text-center"
                >
                    {t('ownerProperties.manageImages')}
                </Link>
                <Link
                    to={`/owner/properties/${propertyId}/units/${unit.id}/edit`}
                    className="px-4 py-2 border border-gray-300 bg-white text-gray-700 font-bold hover:bg-gray-50 text-sm rounded-lg shadow-sm transition-all text-center flex-grow sm:flex-grow-0"
                >
                    {t('ownerProperties.edit')}
                </Link>
                <button
                    onClick={() => onDelete(propertyId, unit.id)}
                    className="px-4 py-2 border border-red-200 bg-red-50 text-red-700 font-bold hover:bg-red-100 text-sm rounded-lg shadow-sm transition-all text-center flex-grow sm:flex-grow-0"
                >
                    {t('editPropertyImages.delete')}
                </button>
            </div>

            {modalConfig?.isOpen && (
                <div className="fixed inset-0 z-[9998] flex items-center justify-center p-4">
                    <div className="fixed inset-0 bg-black/50 backdrop-blur-sm transition-opacity" onClick={() => setModalConfig(null)} />
                    <div className="relative bg-white rounded-2xl shadow-2xl border border-brand-accent w-full max-w-sm overflow-hidden flex flex-col z-[9999] animate-in fade-in zoom-in-95 duration-200 p-6 text-center">
                        {modalConfig.type === "success" ? (
                            <div className="mx-auto flex items-center justify-center h-12 w-12 rounded-full bg-green-50 border border-green-200 mb-4">
                                <svg className="h-6 w-6 text-green-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2.5">
                                    <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
                                </svg>
                            </div>
                        ) : (
                            <div className="mx-auto flex items-center justify-center h-12 w-12 rounded-full bg-red-50 border border-red-200 mb-4">
                                <svg className="h-6 w-6 text-red-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2.5">
                                    <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
                                </svg>
                            </div>
                        )}
                        <h3 className="text-lg font-black text-[#1A1A1A] tracking-tight">{modalConfig.title}</h3>
                        <p className="text-sm text-gray-500 mt-2">
                            {modalConfig.description}
                        </p>
                        <div className="mt-6">
                            <button
                                onClick={() => setModalConfig(null)}
                                className="w-full px-4 py-2 bg-[#42211D] hover:bg-[#5c2e29] text-white font-bold text-sm rounded-lg shadow-sm transition-colors cursor-pointer"
                            >
                                {t('ownerUnits.ok')}
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}