import { useState } from "react";

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

    return (
        <form
            onSubmit={handleSubmit}
            className="bg-white border border-[#DACDCA] rounded-xl p-6 shadow-sm space-y-4"
        >
            <input
                name="title"
                placeholder="Title"
                value={form.title}
                onChange={handleChange}
                className="w-full border rounded-lg p-3"
                required
            />

            <textarea
                name="description"
                placeholder="Description"
                value={form.description}
                onChange={handleChange}
                className="w-full border rounded-lg p-3"
                rows={4}
            />

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <input
                    name="country"
                    placeholder="Country"
                    value={form.country}
                    onChange={handleChange}
                    className="border rounded-lg p-3"
                    required
                />

                <input
                    name="city"
                    placeholder="City"
                    value={form.city}
                    onChange={handleChange}
                    className="border rounded-lg p-3"
                    required
                />
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <input
                    name="postalCode"
                    placeholder="Postal Code"
                    value={form.postalCode}
                    onChange={handleChange}
                    className="border rounded-lg p-3"
                    required
                />

                <input
                    name="street"
                    placeholder="Street"
                    value={form.street}
                    onChange={handleChange}
                    className="border rounded-lg p-3"
                    required
                />

                <input
                    name="streetNumber"
                    placeholder="Street Number"
                    value={form.streetNumber}
                    onChange={handleChange}
                    className="border rounded-lg p-3"
                    required
                />
            </div>

            <button
                type="submit"
                className="bg-[#42211D] text-white px-5 py-3 rounded-lg font-bold hover:bg-[#5C2E29]"
            >
                {submitLabel}
            </button>
        </form>
    );
}