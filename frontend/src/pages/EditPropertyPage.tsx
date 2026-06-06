import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";

import {updateProperty} from "../api/ownerPropertyApi";
import {getProperty} from "../api/propertyApi.ts";
import PropertyForm, {type PropertyFormData} from "../contexts/PropertyForm.tsx";

export default function EditPropertyPage() {
    const { propertyId } = useParams();
    const navigate = useNavigate();

    const [loading, setLoading] = useState(true);
    const [property, setProperty] =
        useState<PropertyFormData | null>(null);

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
        return <div className="p-8">Loading...</div>;
    }

    if (!property) {
        return <div className="p-8">Property not found</div>;
    }

    return (
        <div className="max-w-4xl mx-auto p-8">
            <h1 className="text-3xl font-black mb-6">
                Edit Property
            </h1>

            <PropertyForm
                initialValues={property}
                submitLabel="Save Changes"
                onSubmit={handleUpdate}
            />
        </div>
    );
}