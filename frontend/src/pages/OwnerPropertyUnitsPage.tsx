import { useParams, Link } from "react-router-dom";
import { useEffect, useState } from "react";
import { getProperty } from "../api/propertyApi.ts";
import type { Property, Unit } from "../types/property";
import {deleteUnit, getPropertyUnits, updateUnit} from "../api/ownerUnitApi.ts";
import { getPredictedPrice } from "../api/predictionApi.ts";
import { Wand2 } from "lucide-react";
import {useTranslation} from "react-i18next"

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
                alert(t('ownerUnits.deleteHasReservations'));
                return;
            }

            alert(t('ownerUnits.deleteFailed'));
        }
    };

    return (
        <div className="p-8 max-w-7xl mx-auto min-h-screen text-[#1A1A1A] space-y-6">
            <div>
                <Link to="/owner/properties" className="inline-flex items-center text-sm font-bold text-[#7A7A7A] hover:text-[#1A1A1A] transition-colors mb-4">
                    ← {t('createProperty.back')}
                </Link>
            </div>

            <div className="flex justify-between items-center border-b border-[#DACDCA] pb-4 mb-6">
                <div>
                    <h1 className="text-3xl font-black text-[#1A1A1A] tracking-tight">{property?.title || t('ownerUnits.loading')}</h1>
                    <p className="text-sm font-semibold text-[#7A7A7A] uppercase tracking-wider mt-1.5">{t('ownerUnits.subtitle')}</p>
                </div>

                <Link
                    to={`/owner/properties/${propertyId}/units/new`}
                    className="px-5 py-2.5 bg-[#42211D] text-white font-bold hover:bg-[#5C2E29] text-sm rounded-lg transition-colors border border-[#DACDCA] shadow-sm"
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
            alert(t('ownerUnits.priceUpdated'));
        } catch (error) {
            console.error("Failed to update price:", error);
            alert(t('ownerUnits.priceUpdateFailed'));
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

                <div className="pt-4 flex flex-wrap gap-x-10 gap-y-4 items-center border-t border-gray-100 mt-4">
                    <div>
                        <span className="block text-xxs uppercase tracking-wider text-[#7A7A7A] mb-1">{t('ownerUnits.currentPrice')}</span>
                        <div className="text-sm font-bold text-[#7A7A7A]">
                            <span className="text-xl font-black text-[#1A1A1A]">{currentPrice.toFixed(2)} PLN</span> {t('ownerUnits.perNight')}
                        </div>
                    </div>

                    <div>
                        <span className="block text-xxs uppercase tracking-wider text-[#7A7A7A] mb-1"> {t('ownerUnits.suggestedPrice')}</span>
                        <div className="text-sm font-bold text-[#7A7A7A] flex items-center gap-1.5">
                            {loadingPrediction ? (
                                <span className="text-gray-400 text-sm animate-pulse font-semibold">{t('ownerUnits.calculating')}</span>
                            ) : predictedPrice !== null ? (
                                    <div className="flex items-center gap-3">
                                        <span className="text-xl font-black text-indigo-600">{predictedPrice.toFixed(2)} PLN</span>

                                        {Math.abs(currentPrice - predictedPrice) > 0.01 && (
                                            <button
                                                onClick={handleApplySuggestedPrice}
                                                disabled={isUpdatingPrice}
                                                className="inline-flex items-center gap-1.5 px-2.5 py-1 bg-indigo-50 text-indigo-700 text-xs font-bold rounded-md border border-indigo-200 hover:bg-indigo-100 transition-colors disabled:opacity-50"
                                            >
                                                {isUpdatingPrice ? (
                                                    t('ownerUnits.applying')
                                                ) : (
                                                    <>
                                                        <Wand2 className="w-3.5 h-3.5" />
                                                        {t('ownerUnits.applyAiPrice')}
                                                    </>
                                                )}
                                            </button>
                                        )}
                                    </div>
                            ) : (
                                <span className="text-amber-600 text-sm font-semibold">{t('ownerUnits.unavailable')}</span>
                            )}
                        </div>
                    </div>
                </div>
            </div>

            <div className="flex gap-3 w-full lg:w-auto justify-end border-t border-gray-100 lg:border-none pt-4 lg:pt-0 shrink-0">
                <Link
                    to={`/owner/properties/${propertyId}/units/${unit.id}/images`}
                    className="px-4 py-2 border border-gray-300 bg-white text-gray-700 font-bold hover:bg-gray-50 text-sm rounded-lg shadow-sm transition-all inline-flex items-center"
                >
                    {t('ownerProperties.manageImages')}
                </Link>
                <Link
                    to={`/owner/properties/${propertyId}/units/${unit.id}/edit`}
                    className="px-4 py-2 border border-gray-300 bg-white text-gray-700 font-bold hover:bg-gray-50 text-sm rounded-lg shadow-sm transition-all"
                >
                    {t('ownerProperties.edit')}
                </Link>
                <button
                    onClick={() => onDelete(propertyId, unit.id)}
                    className="px-4 py-2 border border-red-200 bg-red-50 text-red-700 font-bold hover:bg-red-100 text-sm rounded-lg shadow-sm transition-all"
                >
                    {t('editPropertyImages.delete')}
                </button>
            </div>
        </div>
    );
}