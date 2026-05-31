import { useEffect, useState } from "react";
import {getMyProperties} from "../api/ownerPropertyApi.ts";
import type { Property } from "../types/property";
import { Link } from "react-router-dom";

export default function OwnerPropertiesPage() {
    const [properties, setProperties] = useState<Property[]>([]);

    useEffect(() => {
        getMyProperties()
            .then(data => {
                if (Array.isArray(data)) {
                    setProperties(data);
                }
            })
            .catch(console.error);
    }, []);

    return (
        <div className="p-6">
            <div className="flex justify-between mb-6">
                <h1 className="text-3xl font-bold">
                    My Properties
                </h1>

                <Link
                    to="/owner/properties/new"
                    className="btn-primary"
                >
                    Add Property
                </Link>
            </div>

            <div className="space-y-4">
                {properties.map(property => (
                    <div
                        key={property.id}
                        className="bg-card rounded-xl p-4 shadow"
                    >
                        <div className="flex justify-between items-center">
                            <div>
                                <h2 className="font-bold text-xl">
                                    {property.title}
                                </h2>

                                <p>{property.location}</p>
                            </div>

                            <div className="flex gap-2">
                                <Link
                                    to={`/owner/properties/${property.id}/edit`}
                                >
                                    Edit
                                </Link>

                                <button>
                                    Delete
                                </button>

                                <Link
                                    to={`/owner/properties/${property.id}/units`}
                                >
                                    Manage Units
                                </Link>
                            </div>
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
}