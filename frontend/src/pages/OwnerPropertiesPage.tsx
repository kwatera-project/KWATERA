import {useEffect, useState} from "react";
import {deleteProperty, getMyProperties} from "../api/ownerPropertyApi.ts";
import type {Property} from "../types/property";
import {Link} from "react-router-dom";
import {getPropertyImages} from "../api/propertyApi.ts";

type PropertyImage = {
    url: string;
    isMain?: boolean;
};

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

    const handleDelete = async (propertyId: string) => {
        const confirmed = window.confirm(
            "Are you sure you want to delete this property?"
        );

        if (!confirmed) return;

        try {
            await deleteProperty(propertyId);

            setProperties(prev =>
                prev.filter(p => p.id !== propertyId)
            );
        } catch (error: any) {
            console.error(error);

            if (error.response?.status === 409) {
                alert(error.response.data.message);
            }

            alert("Failed to delete property");
        }
    };

    return (
        <div className="p-8 max-w-7xl mx-auto min-h-screen text-[#1A1A1A] space-y-6">
            <div className="flex justify-between items-center border-b border-[#DACDCA] pb-4 mb-6">
                <div>
                    <h1 className="text-3xl font-black text-[#1A1A1A] tracking-tight">My Properties</h1>
                    <p className="text-sm font-semibold text-[#7A7A7A] mt-1">Manage and configure your rental accommodations</p>
                </div>
                <Link
                    to="/owner/properties/new"
                    className="px-5 py-2.5 bg-[#42211D] text-white font-bold hover:bg-[#5C2E29] text-sm rounded-lg transition-colors border border-[#DACDCA] shadow-sm"
                >
                    Add Property
                </Link>
            </div>

            <div className="space-y-6">
                {properties.map(property => (
                    <PropertyCard
                        key={property.id}
                        property={property}
                        onDelete={handleDelete}
                    />
                ))}
                {properties.length === 0 && (
                    <div className="text-gray-500 italic py-8 text-center bg-white border border-[#DACDCA] rounded-xl shadow-sm">
                        No properties found. Click "Add Property" to create one.
                    </div>
                )}
            </div>
        </div>
    );
}

function PropertyCard({ property, onDelete }: { property: Property, onDelete: (id: string) => void; }) {
    const [mainImage, setMainImage] = useState<string>("");

    useEffect(() => {
        getPropertyImages(property.id)
            .then((data: PropertyImage[]) => {
                if (data && data.length > 0) {
                    const mainImgObject =
                        data.find((img) => img.isMain) || data[0];

                    setMainImage(mainImgObject.url);
                }
            })
            .catch(console.error);
    }, [property.id]);

    return (
        <div className="bg-white border border-[#DACDCA] rounded-xl shadow-sm p-6 hover:shadow-md transition-all duration-300 flex flex-col md:flex-row justify-between items-start md:items-center gap-6">
            <div className="flex flex-col sm:flex-row gap-6 items-start flex-grow w-full">
                <div className="w-full sm:w-32 h-32 bg-gray-100 rounded-lg overflow-hidden flex-shrink-0 flex items-center justify-center border border-[#DACDCA]/50 shadow-sm">
                    {mainImage ? (
                        <img src={mainImage} alt={property.title} className="w-full h-full object-cover" />
                    ) : (
                        <span className="text-gray-400 text-xs font-semibold uppercase tracking-wider">No Image</span>
                    )}
                </div>

                <div className="space-y-2 flex-grow">
                    <h2 className="font-black text-xl text-[#1A1A1A] tracking-tight">{property.title}</h2>
                    <p className="text-[#7A7A7A] text-sm font-medium line-clamp-2">{property.description}</p>
                    
                    <div className="pt-2">
                        <span className="block text-xxs font-bold text-[#7A7A7A] uppercase tracking-wider mb-1">Address</span>
                        <p className="text-sm font-semibold text-[#1A1A1A]">
                            {property.street} {property.streetNumber}, {property.postalCode} {property.city}, {property.country}
                        </p>
                    </div>
                </div>
            </div>

            <div className="flex gap-3 w-full md:w-auto justify-end border-t border-gray-100 md:border-none pt-4 md:pt-0 shrink-0">
                <Link
                    to={`/owner/properties/${property.id}/units`}
                    className="px-4 py-2 border border-[#42211D] bg-[#42211D] text-white font-bold hover:bg-[#5C2E29] text-sm rounded-lg shadow-sm transition-all inline-flex items-center"
                >
                    Manage Units
                </Link>
                <Link
                    to={`/owner/properties/${property.id}/images`}
                    className="px-4 py-2 border border-gray-300 bg-white text-gray-700 font-bold hover:bg-gray-50 text-sm rounded-lg shadow-sm transition-all inline-flex items-center"
                >
                    Manage Images
                </Link>
                <Link
                    to={`/owner/properties/${property.id}/edit`}
                    className="px-4 py-2 border border-gray-300 bg-white text-gray-700 font-bold hover:bg-gray-50 text-sm rounded-lg shadow-sm transition-all"
                >
                    Edit
                </Link>
                <button
                    onClick={() => onDelete(property.id)}
                    className="px-4 py-2 border border-red-200 bg-red-50 text-red-700 font-bold hover:bg-red-100 text-sm rounded-lg shadow-sm transition-all"
                >
                    Delete
                </button>
            </div>
        </div>
    );
}