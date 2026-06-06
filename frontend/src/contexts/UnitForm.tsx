import { useState } from "react";

export interface UnitFormData {
    name: string;
    description: string;
    pricePerNight: number;
    capacity: number;
    unitType: string;
    unitNumber: string;
    floor: number;
}

interface UnitFormProps {
    initialValues?: UnitFormData;
    onSubmit: (data: UnitFormData) => Promise<void>;
    submitLabel: string;
}

const UNIT_TYPES = [
    "Entire rental unit", "Private room in rental unit", "Private room in home",
    "Entire townhouse", "Entire home", "Entire condo", "Entire guest suite",
    "Private room in guest suite", "Shared room in home", "Shared room in hostel",
    "Entire serviced apartment", "Entire villa", "Private room in townhouse",
    "Private room in hostel", "Room in aparthotel", "Entire loft", "Private room",
    "Room in serviced apartment", "Private room in condo", "Shared room in rental unit",
    "Entire place", "Room in hotel", "Entire apartment", "Entire guesthouse",
    "Room in boutique hotel", "Private room in villa", "Entire chalet", "Room in hostel",
    "Private room in bed and breakfast", "Entire cottage", "Tiny home",
    "Private room in serviced apartment", "Entire bungalow", "Entire home/apt"
];

export default function UnitForm({
                                         initialValues,
                                         onSubmit,
                                         submitLabel,
                                     }: UnitFormProps) {
    const [form, setForm] = useState<UnitFormData>(
        initialValues ?? {
            name: "",
            description: "",
            pricePerNight: "" as unknown as number,
            capacity: "" as unknown as number,
            unitType: UNIT_TYPES[0],
            unitNumber: "",
            floor: "" as unknown as number,
        }
    );

    const handleChange = (
        e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>
    ) => {
        const { name, value, type } = e.target;

        setForm(prev => ({
            ...prev,
            [name]: type === "number" ? (value === "" ? "" : Number(value)) : value,
        }));
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        const payload = {
            ...form,
            pricePerNight: Number(form.pricePerNight),
            capacity: Number(form.capacity),
            floor: Number(form.floor)
        };
        await onSubmit(payload);
    };

    return (
        <form
            onSubmit={handleSubmit}
            className="bg-white border border-[#DACDCA] rounded-xl p-6 shadow-sm space-y-4"
        >
            <input
                name="name"
                placeholder="Name"
                value={form.name}
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
                    type="number"
                    name="pricePerNight"
                    placeholder="Price"
                    value={form.pricePerNight === 0 ? "" : form.pricePerNight}
                    onChange={handleChange}
                    className="w-full border rounded-lg p-3"
                    min="0"
                    step="0.01"
                    required
                />

                <select
                    name="unitType"
                    value={form.unitType}
                    onChange={handleChange}
                    className="w-full border rounded-lg p-3 bg-white h-[50px]"
                    required>
                    {UNIT_TYPES.map(type => (
                        <option key={type} value={type}>
                            {type}
                        </option>
                    ))}
                </select>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <input
                    type="number"
                    name="capacity"
                    placeholder="Guests"
                    value={form.capacity === 0 ? "" : form.capacity}
                    onChange={handleChange}
                    className="w-full border rounded-lg p-3"
                    min="1"
                    required
                />

                <input
                    name="unitNumber"
                    placeholder="e.g. 104A"
                    value={form.unitNumber}
                    onChange={handleChange}
                    className="w-full border rounded-lg p-3"
                    required
                />

                <input
                    type="number"
                    name="floor"
                    placeholder="Floor"
                    onChange={handleChange}
                    className="w-full border rounded-lg p-3"
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