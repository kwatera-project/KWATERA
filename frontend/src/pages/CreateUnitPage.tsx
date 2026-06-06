import {useNavigate, useParams} from "react-router-dom";
import UnitForm, {type UnitFormData} from "../contexts/UnitForm.tsx";
import {createUnit} from "../api/ownerUnitApi.ts";

export default function CreateUnitPage() {
    const navigate = useNavigate();
    const { propertyId } = useParams<{ propertyId: string }>();

    const handleCreate = async (data: UnitFormData) => {
        if (!propertyId) {
            alert("Missing property ID");
            return;
        }
        try {
            await createUnit(propertyId, data);
            navigate(`/owner/properties/${propertyId}/units`);
        } catch (error) {
            console.error(error);
            alert("Failed to create unit");
        }
    };

    return (
        <div className="max-w-4xl mx-auto p-8">
            <h1 className="text-3xl font-black mb-6">
                Create Unit
            </h1>

            <UnitForm
                submitLabel="Create Unit"
                onSubmit={handleCreate}
            />
        </div>
    );
}