import { useState } from "react";
import {useTranslation} from "react-i18next"

export interface PropertyFormData {
    title: string;
    description: string;
    country: string;
    city: string;
    postalCode: string;
    street: string;
    streetNumber: string;
}

interface PropertyFormProps {
    initialValues?: PropertyFormData;
    onSubmit: (data: PropertyFormData) => Promise<void>;
    submitLabel: string;
}

export default function PropertyForm({
                                         initialValues,
                                         onSubmit,
                                         submitLabel,
                                     }: PropertyFormProps) {
    const [form, setForm] = useState<PropertyFormData>(
        initialValues ?? {
            title: "",
            description: "",
            country: "",
            city: "",
            postalCode: "",
            street: "",
            streetNumber: "",
        }
    );

    const handleChange = (
        e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>
    ) => {
        setForm(prev => ({
            ...prev,
            [e.target.name]: e.target.value,
        }));
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        await onSubmit(form);
    };


    const inputClasses = "w-full border border-[#DACDCA] rounded-lg p-3 text-sm font-semibold text-[#1A1A1A] placeholder-[#7A7A7A]/70 focus:outline-none focus:border-[#42211D] focus:ring-1 focus:ring-[#42211D] transition-all bg-transparent";
    const {t} = useTranslation();

    return (
        <form onSubmit={handleSubmit} className="space-y-5">
            <div className="space-y-1">
                <label className="text-xs font-bold text-[#7A7A7A] uppercase tracking-wider">{t('propertyForm.title')}</label>
                <input
                    name="title"
                    placeholder={t('propertyForm.titlePlaceholder')}
                    value={form.title}
                    onChange={handleChange}
                    className={inputClasses}
                    required
                />
            </div>

            <div className="space-y-1">
                <label className="text-xs font-bold text-[#7A7A7A] uppercase tracking-wider">{t('propertyForm.description')}</label>
                <textarea
                    name="description"
                    placeholder={t('propertyForm.descriptionPlaceholder')}
                    value={form.description}
                    onChange={handleChange}
                    className={inputClasses}
                    rows={4}
                />
            </div>

            <div className="border-t border-[#DACDCA]/50 my-6 pt-4 space-y-4">
                <span className="block text-xs font-bold text-[#7A7A7A] uppercase tracking-wider mb-2">{t('propertyForm.locationDetails')}</span>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div className="space-y-1">
                        <input
                            name="country"
                            placeholder={t('propertyForm.country')}
                            value={form.country}
                            onChange={handleChange}
                            className={inputClasses}
                            required
                        />
                    </div>

                    <div className="space-y-1">
                        <input
                            name="city"
                            placeholder={t('search.city')}
                            value={form.city}
                            onChange={handleChange}
                            className={inputClasses}
                            required
                        />
                    </div>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                    <input
                        name="postalCode"
                        placeholder={t('propertyForm.postalCode')}
                        value={form.postalCode}
                        onChange={handleChange}
                        className={inputClasses}
                        required
                    />

                    <input
                        name="street"
                        placeholder={t('propertyForm.street')}
                        value={form.street}
                        onChange={handleChange}
                        className={inputClasses}
                        required
                    />

                    <input
                        name="streetNumber"
                        placeholder={t('propertyForm.streetNumber')}
                        value={form.streetNumber}
                        onChange={handleChange}
                        className={inputClasses}
                        required
                    />
                </div>
            </div>

            <div className="pt-4">
                <button
                    type="submit"
                    className="w-full md:w-auto px-6 py-3 bg-[#42211D] text-white font-bold hover:bg-[#5C2E29] text-sm rounded-lg transition-colors border border-[#DACDCA] shadow-sm tracking-tight"
                >
                    {submitLabel}
                </button>
            </div>
        </form>
    );
}