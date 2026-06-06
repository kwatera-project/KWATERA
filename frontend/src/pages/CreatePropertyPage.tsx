import { useNavigate } from "react-router-dom";
import { createProperty } from "../api/ownerPropertyApi";
import PropertyForm, {type PropertyFormData} from "../contexts/PropertyForm.tsx";

export default function CreatePropertyPage() {
    const navigate = useNavigate();

    const handleCreate = async (data: PropertyFormData) => {
        try {
            await createProperty(data);
            navigate("/owner/properties");
        } catch (error) {
            console.error(error);
            alert("Failed to create property");
        }
    };

    return (
        <div className="max-w-4xl mx-auto p-8">
            <h1 className="text-3xl font-black mb-6">
                Create Property
            </h1>

            <PropertyForm
                submitLabel="Create Property"
                onSubmit={handleCreate}
            />
        </div>
    );
}