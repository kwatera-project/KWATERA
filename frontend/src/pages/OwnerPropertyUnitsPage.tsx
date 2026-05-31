import { useParams } from "react-router-dom";
import { useEffect, useState } from "react";
import { getProperty } from "../api/propertyApi.ts";
import type { Property, Unit } from "../types/property";
import { getPropertyUnits } from "../api/ownerUnitApi.ts";
import { getPredictedPrice } from "../api/predictionApi.ts";
import { useCurrency } from "../contexts/CurrencyContext";

export default function OwnerPropertyUnitsPage() {
    const { propertyId } = useParams();

    const [property, setProperty] = useState<Property>();
    const [units, setUnits] = useState<Unit[]>([]);

    useEffect(() => {
        if (!propertyId) return;

        getProperty(propertyId).then(setProperty).catch(console.error);
        getPropertyUnits(propertyId).then(setUnits).catch(console.error);
    }, [propertyId]);

    return (
        <div className="p-6">
            <div className="flex justify-between mb-6 items-center">
                <div>
                    <h1 className="text-3xl font-bold">{property?.title || "Loading..."}</h1>
                    <p className="text-gray-500 text-sm">Manage Units</p>
                </div>

                <button className="px-4 py-2 bg-primary text-white rounded-lg text-sm font-medium">
                    Add Unit
                </button>
            </div>

            <div className="space-y-4">
                {units.map(unit => (
                    <UnitCard
                        key={unit.id}
                        unit={unit}
                        propertyId={propertyId!}
                    />
                ))}
                {units.length === 0 && (
                    <div className="text-gray-500 italic py-4">No units found for this property.</div>
                )}
            </div>
        </div>
    );
}

function UnitCard({ unit, propertyId }: { unit: Unit; propertyId: string }) {
    const { currency } = useCurrency();
    const [predictedPrice, setPredictedPrice] = useState<number | null>(null);
    const [loadingPrediction, setLoadingPrediction] = useState(true);

    useEffect(() => {
        setLoadingPrediction(true);
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

    return (
        <div className="border rounded-xl p-4 bg-card shadow-sm flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
            <div className="space-y-1">
                <h2 className="font-bold text-xl">{unit.name}</h2>
                <p className="text-gray-600 text-sm">{unit.description}</p>

                <div className="grid grid-cols-2 md:grid-cols-4 gap-x-4 gap-y-1 pt-2 text-sm text-gray-500">
                    <div><span className="font-medium text-gray-700">Type:</span> {unit.unitType}</div>
                    <div><span className="font-medium text-gray-700">Unit #:</span> {unit.unitNumber}</div>
                    <div><span className="font-medium text-gray-700">Floor:</span> {unit.floor}</div>
                    <div><span className="font-medium text-gray-700">Capacity:</span> {unit.capacity} guests</div>
                </div>

                <div className="pt-2 flex flex-wrap gap-x-6 items-center">
                    <div className="text-sm font-medium">
                        Current Price: <span className="text-base font-semibold">{unit.pricePerNight} {currency}</span> / night
                    </div>

                    <div className="text-sm font-medium text-blue-600 flex items-center gap-1">
                        Suggested price:{" "}
                        {loadingPrediction ? (
                            <span className="text-gray-400 text-xs animate-pulse">Calculating...</span>
                        ) : predictedPrice !== null ? (
                            <span className="text-base font-bold">{predictedPrice} {currency}</span>
                        ) : (
                            <span className="text-amber-600 text-xs">Unavailable</span>
                        )}
                    </div>
                </div>
            </div>

            <div className="flex gap-2 self-end md:self-center">
                <button className="px-3 py-1.5 border rounded-lg text-sm hover:bg-gray-50">
                    Edit
                </button>
                <button className="px-3 py-1.5 border rounded-lg text-sm bg-destructive text-destructive-foreground hover:bg-destructive/90">
                    Delete
                </button>
            </div>
        </div>
    );
}