import {useEffect, useState} from "react";
import {getMyProperties} from "../api/ownerPropertyApi.ts";
import type {Property} from "../types/property";
import {Link} from "react-router-dom";

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

                <button>
                    Add Property
                </button>
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

                                <p>{property.description}</p>

                                <div>
                                    Country: {property.country}
                                </div>

                                <div>
                                    City: {property.city}
                                </div>

                                <div>
                                    Postal Code: {property.postalCode}
                                </div>

                                <div>
                                    Street: {property.street}
                                </div>

                                <div>
                                    Street number: {property.streetNumber}
                                </div>
                            </div>

                            <div className="flex gap-3 mt-3">
                                <Link
                                    to={`/owner/properties/${property.id}/units`}
                                >
                                    Manage Units
                                </Link>
                                <button>
                                    Edit
                                </button>
                                <button>
                                    Delete
                                </button>
                            </div>
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
}