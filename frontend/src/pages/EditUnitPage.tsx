import {useNavigate, useParams} from "react-router-dom";
import {useEffect, useState} from "react";
import {updateUnit} from "../api/ownerUnitApi.ts";
import UnitForm, {type UnitFormData} from "../contexts/UnitForm.tsx";
import {getUnit} from "../api/propertyApi.ts";

export default function EditUnitPage() {
    const { propertyId, unitId } = useParams<{ propertyId: string; unitId: string }>();
    const navigate = useNavigate();

    const [loading, setLoading] = useState(true);
    const [unit, setUnit] = useState<UnitFormData | null>(null);

    useEffect(() => {
        if (!unitId) return;

        getUnit(unitId)
            .then(data => {
                setUnit({
                    name: data.name,
                    description: data.description,
                    pricePerNight: data.pricePerNight,
                    capacity: data.capacity,
                    unitType: data.unitType,
                    unitNumber: data.unitNumber,
                    floor: data.floor,
                });
            })
            .finally(() => setLoading(false));
    }, [propertyId, unitId]);

    const handleUpdate = async (data: UnitFormData) => {
        if (!propertyId || !unitId) {
            alert("Missing required identifiers");
            return;
        }

        try {
            await updateUnit(propertyId, unitId, data);
            navigate(`/owner/properties/${propertyId}/units`);
        } catch (error) {
            console.error(error);
            alert("Failed to update unit");
        }
    };

    if (loading) {
        return <div className="p-8">Loading...</div>;
    }

    if (!unit) {
        return <div className="p-8">Unit not found</div>;
    }

    return (
        <div className="max-w-4xl mx-auto p-8">
            <h1 className="text-3xl font-black mb-6">
                Edit Unit
            </h1>

            <UnitForm
                initialValues={unit}
                submitLabel="Save Changes"
                onSubmit={handleUpdate}
            />
        </div>
    );
}