import { useEffect, useState } from "react";
import { getProperties } from "../api/propertyApi";
import type { Property } from "../types/property";
import { Link } from "react-router-dom";

export default function PropertiesPage() {
    const [properties, setProperties] = useState<Property[]>([]);

    useEffect(() => {
        getProperties().then(setProperties);
    }, []);

    return (
        <div className="p-6 grid grid-cols-1 md:grid-cols-2 gap-6">
            {properties.map(p => (
                <Link key={p.id} to={`/property/${p.id}`}>
                    <div className="bg-card rounded-xl shadow p-4">
                        <img src={p.imageUrl} className="w-full h-40 object-cover rounded" />
                        <h2 className="text-xl font-bold mt-2">{p.title}</h2>
                        <p className="text-details">{p.location}</p>
                    </div>
                </Link>
            ))}
        </div>
    );
}