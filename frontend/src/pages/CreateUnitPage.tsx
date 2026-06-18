import { Link, useNavigate, useParams } from "react-router-dom";
import UnitForm, { type UnitFormData } from "../contexts/UnitForm.tsx";
import { createUnit } from "../api/ownerUnitApi.ts";
import { useTranslation } from "react-i18next";

export default function CreateUnitPage() {
    const navigate = useNavigate();
    const { propertyId } = useParams<{ propertyId: string }>();
    const {t} = useTranslation();

    const handleCreate = async (data: UnitFormData) => {
        if (!propertyId) {
            alert(t("createUnit.missingPropertyId"));
            return;
        }
        try {
            await createUnit(propertyId, data);
            navigate(`/owner/properties/${propertyId}/units`);
        } catch (error) {
            console.error(error);
            alert(t("createUnit.failed"));
        }
    };

    return (
        <div className="p-4 sm:p-8 max-w-7xl mx-auto min-h-screen text-[#1A1A1A] space-y-6">
            <div>
                <Link
                    to={`/owner/properties/${propertyId}/units`}
                    className="inline-flex items-center text-sm font-semibold text-[#7A7A7A] hover:text-[#1A1A1A] transition-colors mb-2"
                >
                    ← {t("createUnit.back")}
                </Link>
            </div>

            <div className="border-b border-[#DACDCA] pb-4 mb-6">
                <h1 className="text-3xl font-black text-[#1A1A1A] tracking-tight">
                    {t("createUnit.title")}
                </h1>
                <p className="text-sm font-semibold text-[#7A7A7A] mt-1">
                    {t("createUnit.subtitle")}
                </p>
            </div>

            <div className="max-w-4xl mx-auto bg-white border border-[#DACDCA] rounded-xl shadow-sm p-5 sm:p-8 mt-6">
                <UnitForm
                    submitLabel={t("createUnit.submit")}
                    onSubmit={handleCreate}
                />
            </div>
        </div>
    );
}