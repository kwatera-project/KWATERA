import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { updateProperty } from "../api/ownerPropertyApi";
import { getProperty } from "../api/propertyApi.ts";
import PropertyForm, {type PropertyFormData} from "../contexts/PropertyForm.tsx";


export default function EditPropertyPage() {
    const { propertyId } = useParams();
    const navigate = useNavigate();

    const [loading, setLoading] = useState(true);
    const [property, setProperty] = useState<PropertyFormData | null>(null);

    useEffect(() => {
        if (!propertyId) return;

        getProperty(propertyId)
            .then(data => {
                setProperty({
                    title: data.title,
                    description: data.description,
                    country: data.country,
                    city: data.city,
                    postalCode: data.postalCode,
                    street: data.street,
                    streetNumber: data.streetNumber,
                    amenities: data.amenities || [],
                    propertyType: data.propertyType ?? "",
                });
            })
            .finally(() => setLoading(false));
    }, [propertyId]);

    const handleUpdate = async (data: PropertyFormData) => {
        if (!propertyId) return;

        try {
            await updateProperty(propertyId, data);
            navigate("/owner/properties");
        } catch (error) {
            console.error(error);
            alert("Failed to update property");
        }
    };

    if (loading) {
        return (
            <div className="p-8 max-w-7xl mx-auto min-h-screen text-[#7A7A7A] font-semibold text-sm">
                Loading...
            </div>
        );
    }

    if (!property) {
        return (
            <div className="p-8 max-w-7xl mx-auto min-h-screen text-[#7A7A7A] font-semibold text-sm">
                Property not found
            </div>
        );
    }

    return (
        <div className="p-8 max-w-7xl mx-auto min-h-screen text-[#1A1A1A] space-y-6">
            <div>
                <Link
                    to="/owner/properties"
                    className="inline-flex items-center text-sm font-semibold text-[#7A7A7A] hover:text-[#1A1A1A] transition-colors mb-2"
                >
                    ← Back to Properties
                </Link>
            </div>

            <div className="border-b border-[#DACDCA] pb-4 mb-6">
                <h1 className="text-3xl font-black text-[#1A1A1A] tracking-tight">
                    Edit Property
                </h1>
                <p className="text-sm font-semibold text-[#7A7A7A] mt-1">
                    Modify the general settings, details, and location of your property
                </p>
            </div>

            <div className="max-w-4xl mx-auto bg-white border border-[#DACDCA] rounded-xl shadow-sm p-8 mt-6">
                <PropertyForm
                    initialValues={property}
                    submitLabel="Save Changes"
                    onSubmit={handleUpdate}
                />
            </div>
        </div>
    );
}