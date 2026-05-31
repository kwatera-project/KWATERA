import { useParams, Link } from "react-router-dom";
import { useEffect, useState } from "react";
import {getProperty} from "../api/propertyApi.ts";
import type {Property, Unit} from "../types/property";
import {getPropertyUnits} from "../api/ownerUnitApi.ts";
import { getPredictedPrice } from "../api/predictionApi.ts";
import { useCurrency } from "../contexts/CurrencyContext";

export default function OwnerPropertyUnitsPage() {
    const { propertyId } = useParams();

    const [property, setProperty] = useState<Property>();
    const [units, setUnits] = useState<Unit[]>([]);
    const [predictions, setPredictions] = useState<Record<string, number>>({});

    const { currency } = useCurrency();

    useEffect(() => {
        if (!propertyId) return;

        getProperty(propertyId).then(setProperty);
        getPropertyUnits(propertyId).then(setUnits);
    }, [propertyId]);

    useEffect(() => {
        if (!propertyId || units.length === 0) return;

        const fetchPredictions = async () => {
            const results: Record<string, number> = {};

            await Promise.all(
                units.map(async (unit) => {
                    try {
                        const price = await getPredictedPrice(
                            propertyId,
                            unit.id,
                            undefined
                        );

                        results[unit.id] = price;
                    } catch (e) {
                        console.error("Prediction error for unit", unit.id, e);
                    }
                })
            );

            setPredictions(results);
        };

        fetchPredictions();

    }, [units, propertyId]);

    return (
        <div className="p-6">
            <div className="flex justify-between mb-6">
                <div>
                    <h1 className="text-3xl font-bold">
                        {property?.title}
                    </h1>

                    <p>Manage Units</p>
                </div>

                <Link
                    to={`/owner/properties/${propertyId}/units/new`}
                >
                    Add Unit
                </Link>
            </div>

            <div className="space-y-4">
                {units.map(unit => (
                    <div
                        key={unit.id}
                        className="border rounded-xl p-4"
                    >
                        <h2 className="font-bold text-xl">
                            {unit.name}
                        </h2>

                        <p>{unit.description}</p>

                        <div>
                            Type: {unit.unit_type}
                        </div>

                        <div>
                            Unit number: {unit.unit_number}
                        </div>

                        <div>
                            Floor: {unit.floor}
                        </div>

                        <div>
                            Capacity: {unit.capacity}
                        </div>

                        <div>
                            Price: {unit.pricePerNight} {currency} / night
                        </div>

                        <div className="mt-2 font-semibold text-blue-600">
                            Suggested price:{" "}
                            {predictions[unit.id]
                                ? `${predictions[unit.id]} ${currency} / night`
                                : "Loading..."}
                        </div>

                        <div className="flex gap-3 mt-3">
                            <button>
                                Edit
                            </button>

                            <button>
                                Delete
                            </button>
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
}