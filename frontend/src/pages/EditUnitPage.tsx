import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { updateUnit } from "../api/ownerUnitApi.ts";
import { getUnit } from "../api/propertyApi.ts";
import UnitForm, { type UnitFormData } from "../contexts/UnitForm.tsx";

export default function EditUnitPage() {
    const { propertyId, unitId } = useParams();
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
                    amenities: data.amenities || [],
                    bedrooms: data.bedrooms,
                    beds: data.beds,
                });
            })
            .finally(() => setLoading(false));
    }, [unitId]);

    const handleUpdate = async (data: UnitFormData) => {
        if (!propertyId || !unitId) return;

        try {
            await updateUnit(propertyId, unitId, data);
            navigate(`/owner/properties/${propertyId}/units`);
        } catch (error) {
            console.error(error);
            alert("Failed to update unit");
        }
    };

    if (loading) {
        return (
            <div className="p-8 max-w-7xl mx-auto min-h-screen text-[#7A7A7A] font-semibold text-sm">
                Loading...
            </div>
        );
    }

    if (!unit) {
        return (
            <div className="p-8 max-w-7xl mx-auto min-h-screen text-[#7A7A7A] font-semibold text-sm">
                Unit not found
            </div>
        );
    }

    return (
        <div className="p-4 sm:p-8 max-w-7xl mx-auto min-h-screen text-[#1A1A1A] space-y-6">
            <div>
                <Link
                    to={`/owner/properties/${propertyId}/units`}
                    className="inline-flex items-center text-sm font-semibold text-[#7A7A7A] hover:text-[#1A1A1A] transition-colors mb-2"
                >
                    ← Back to Units
                </Link>
            </div>

            <div className="border-b border-[#DACDCA] pb-4 mb-6">
                <h1 className="text-3xl font-black text-[#1A1A1A] tracking-tight">
                    Edit Unit
                </h1>
                <p className="text-sm font-semibold text-[#7A7A7A] mt-1">
                    Modify the general settings, pricing, and details of your unit
                </p>
            </div>

            <div className="max-w-4xl mx-auto bg-white border border-[#DACDCA] rounded-xl shadow-sm p-5 sm:p-8 mt-6">
                <UnitForm
                    initialValues={unit}
                    submitLabel="Save Changes"
                    onSubmit={handleUpdate}
                />
            </div>
        </div>
    );
}