import { useEffect, useState } from "react";
import { getProperty, getUnits } from "../api/propertyApi";
import { useParams } from "react-router-dom";
import type { Unit } from "../types/property";

export default function PropertyDetailsPage() {
    const { id } = useParams();
    const [property, setProperty] = useState<any>(null);
    const [units, setUnits] = useState<Unit[]>([]);

    useEffect(() => {
        if (id) {
            getProperty(id).then(setProperty);
            getUnits(id).then(setUnits);
        }
    }, [id]);

    if (!property) return <div className="p-6">Loading...</div>;

    return (
        <div className="p-6">
            <img src={property.imageUrl} className="w-full h-60 object-cover rounded" />
            <h1 className="text-3xl font-bold mt-4">{property.title}</h1>
            <p className="text-details">{property.location}</p>

            <h2 className="mt-6 text-xl font-bold">Units</h2>

            {units.map(u => (
                <div key={u.id} className="border p-4 rounded mt-2">
                    <h3 className="font-bold">{u.name}</h3>
                    <p>{u.description}</p>
                    <p>{u.pricePerNight} zł</p>
                    <p>{u.capacity} osób</p>
                </div>
            ))}
        </div>
    );
}