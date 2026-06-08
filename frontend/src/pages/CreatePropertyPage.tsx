import {Link, useNavigate} from "react-router-dom";
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
                    Create Property
                </h1>
                <p className="text-sm font-semibold text-[#7A7A7A] mt-1">
                    Add a new property to your portfolio and set up its location details
                </p>
            </div>

            <div className="max-w-4xl mx-auto bg-white border border-[#DACDCA] rounded-xl shadow-sm p-8 mt-6">
                <PropertyForm
                    submitLabel="Create Property"
                    onSubmit={handleCreate}
                />
            </div>
        </div>
    );
}