import { useParams, Link } from "react-router-dom";
import { useEffect, useState } from "react";
import {getProperty} from "../api/propertyApi.ts";
import type {Property, Unit} from "../types/property";
import {getPropertyUnits} from "../api/ownerUnitApi.ts";


export default function OwnerPropertyUnitsPage() {
    const { propertyId } = useParams();

    const [property, setProperty] = useState<Property>();
    const [units, setUnits] = useState<Unit[]>([]);

    useEffect(() => {
        if (!propertyId) return;

        getProperty(propertyId).then(setProperty);
        getPropertyUnits(propertyId).then(setUnits);
    }, [propertyId]);

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
                        <h2 className="font-bold">
                            {unit.name}
                        </h2>

                        <p>{unit.description}</p>

                        <div>
                            Capacity: {unit.capacity}
                        </div>

                        <div>
                            {unit.pricePerNight} PLN/night
                        </div>

                        <div className="flex gap-3 mt-3">
                            <Link
                                to={`/owner/properties/${propertyId}/units/${unit.id}/edit`}
                            >
                                Edit
                            </Link>

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