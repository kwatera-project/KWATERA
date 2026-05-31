import {useEffect, useState} from "react";
import {getMyProperties} from "../api/ownerPropertyApi.ts";
import type {Property} from "../types/property";
import {Link} from "react-router-dom";
import {getPropertyImages} from "../api/propertyApi.ts";

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
                <h1 className="text-3xl font-bold">My Properties</h1>
                <button className="px-4 py-2 bg-primary text-white rounded-lg">
                    Add Property
                </button>
            </div>

            <div className="space-y-4">
                {properties.map(property => (
                    <PropertyCard key={property.id} property={property}/>
                ))}
            </div>
        </div>
    );
}

function PropertyCard({ property }: { property: Property }) {
    const [mainImage, setMainImage] = useState<string>("");

    useEffect(() => {
        getPropertyImages(property.id)
            .then((data) => {
                if (data && data.length > 0) {
                    setMainImage(data[0]);
                }
            })
            .catch(console.error);
    }, [property.id]);

    return (
        <div className="bg-card rounded-xl p-4 shadow flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
            <div className="flex gap-4 items-start">
                <div className="w-32 h-32 bg-gray-200 rounded-lg overflow-hidden flex-shrink-0 flex items-center justify-center">
                    {mainImage ? (
                        <img src={mainImage} alt={property.title} className="w-full h-full object-cover" />
                    ) : (
                        <span className="text-gray-400 text-sm">No Image</span>
                    )}
                </div>

                <div>
                    <h2 className="font-bold text-xl">{property.title}</h2>
                    <p className="text-gray-600 text-sm mb-2">{property.description}</p>
                    <div className="text-sm text-gray-500 space-y-0.5">
                        <div>Address: {property.street} {property.streetNumber}, {property.postalCode} {property.city}, {property.country}</div>
                    </div>
                </div>
            </div>

            <div className="flex gap-3 self-end md:self-center">
                <Link
                    to={`/owner/properties/${property.id}/units`}
                    className="px-3 py-1.5 border rounded-lg text-sm hover:bg-gray-50"
                >
                    Manage Units
                </Link>
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