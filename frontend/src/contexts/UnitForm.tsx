import { useState } from "react";
import { useTranslation } from "react-i18next"
import TagInput from "../components/TagInput";

export interface UnitFormData {
    name: string;
    description: string;
    pricePerNight: number;
    capacity: number;
    bedrooms: number;
    beds: number;
    unitType: string;
    unitNumber: string;
    floor: number;
    amenities?: string[];
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
    const [form, setForm] = useState<UnitFormData>(() => {
        if (initialValues) {
            return {
                ...initialValues,
                amenities: initialValues.amenities ?? [],
            };
        }
        return {
            name: "",
            description: "",
            pricePerNight: "" as unknown as number,
            capacity: "" as unknown as number,
            bedrooms: "" as unknown as number,
            beds: "" as unknown as number,
            unitType: UNIT_TYPES[0],
            unitNumber: "",
            floor: "" as unknown as number,
            amenities: [],
        };
    });
    const {t} = useTranslation();

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
            bedrooms: Number(form.bedrooms),
            beds: Number(form.beds),
            floor: Number(form.floor),
            amenities: form.amenities ?? []
        };
        await onSubmit(payload);
    };

    const inputClasses = "w-full border border-[#DACDCA] rounded-lg p-3 text-sm font-semibold text-[#1A1A1A] placeholder-[#7A7A7A]/70 focus:outline-none focus:border-[#42211D] focus:ring-1 focus:ring-[#42211D] transition-all bg-transparent";

    return (
        <form onSubmit={handleSubmit} className="space-y-5">
            <div className="space-y-1">
                <label className="text-xs font-bold text-[#7A7A7A] uppercase tracking-wider">{t('unitForm.name')}</label>
                <input
                    name="name"
                    placeholder={t('unitForm.namePlaceholder')}
                    value={form.name}
                    onChange={handleChange}
                    className={inputClasses}
                    required
                />
            </div>

            <div className="space-y-1">
                <label className="text-xs font-bold text-[#7A7A7A] uppercase tracking-wider">{t('propertyForm.description')}</label>
                <textarea
                    name="description"
                    placeholder={t('unitForm.descriptionPlaceholder')}
                    value={form.description}
                    onChange={handleChange}
                    className={inputClasses}
                    rows={4}
                />
            </div>

            <div className="border-t border-[#DACDCA]/50 my-6 pt-4 space-y-4">
                <span className="block text-xs font-bold text-[#7A7A7A] uppercase tracking-wider mb-2">{t('unitForm.pricingType')}</span>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div className="space-y-1">
                        <input
                            type="number"
                            name="pricePerNight"
                            placeholder={t('unitForm.pricePerNight')}
                            value={form.pricePerNight === 0 || isNaN(form.pricePerNight) ? "" : form.pricePerNight}
                            onChange={handleChange}
                            className={inputClasses}
                            min="0"
                            step="0.01"
                            required
                        />
                    </div>

                    <div className="space-y-1">
                        <select
                            name="unitType"
                            value={form.unitType}
                            onChange={handleChange}
                            className={`${inputClasses} bg-white appearance-none cursor-pointer`}
                            required
                        >
                            {UNIT_TYPES.map(type => (
                                <option key={type} value={type}>
                                    {type}
                                </option>
                            ))}
                        </select>
                    </div>
                </div>

                <span className="block text-xs font-bold text-[#7A7A7A] uppercase tracking-wider mb-2 pt-2">{t('unitForm.spaceLocation')}</span>

                <div className="grid grid-cols-2 md:grid-cols-5 gap-4">
                    <div className="space-y-1">
                        <label htmlFor="capacity" className="text-xs font-bold text-[#7A7A7A] uppercase tracking-wider">
                            {t('unitForm.maxGuests')}
                        </label>
                        <input
                            id="capacity"
                            type="number"
                            name="capacity"
                            placeholder={t('unitForm.maxGuests')}
                            value={form.capacity === 0 || isNaN(form.capacity) ? "" : form.capacity}
                            onChange={handleChange}
                            className={inputClasses}
                            min="1"
                            required
                        />
                    </div>

                    <div className="space-y-1">
                        <label htmlFor="bedrooms" className="text-xs font-bold text-[#7A7A7A] uppercase tracking-wider">
                            Bedrooms
                        </label>
                        <input
                            id="bedrooms"
                            type="number"
                            name="bedrooms"
                            placeholder="Bedrooms"
                            value={form.bedrooms === 0 || isNaN(form.bedrooms) ? "" : form.bedrooms}
                            onChange={handleChange}
                            className={inputClasses}
                            min="0"
                            required
                        />
                    </div>

                    <div className="space-y-1">
                        <label htmlFor="beds" className="text-xs font-bold text-[#7A7A7A] uppercase tracking-wider">
                            Beds
                        </label>
                        <input
                            id="beds"
                            type="number"
                            name="beds"
                            placeholder="Beds"
                            value={form.beds === 0 || isNaN(form.beds) ? "" : form.beds}
                            onChange={handleChange}
                            className={inputClasses}
                            min="0"
                            required
                        />
                    </div>

                    <div className="space-y-1">
                        <label htmlFor="unitNumber" className="text-xs font-bold text-[#7A7A7A] uppercase tracking-wider">
                            Unit Number
                        </label>
                        <input
                            id="unitNumber"
                            name="unitNumber"
                            placeholder={t('unitForm.unitNumber')}
                            value={form.unitNumber}
                            onChange={handleChange}
                            className={inputClasses}
                            required
                        />
                    </div>

                    <div className="space-y-1">
                        <label htmlFor="floor" className="text-xs font-bold text-[#7A7A7A] uppercase tracking-wider">
                            {t('unitForm.floor')}
                        </label>
                        <input
                            id="floor"
                            type="number"
                            name="floor"
                            placeholder={t('unitForm.floor')}
                            value={isNaN(form.floor) ? "" : form.floor}
                            onChange={handleChange}
                            className={inputClasses}
                            required
                        />
                    </div>
                </div>
            </div>

            <div className="border-t border-[#DACDCA]/50 my-6 pt-4">
                <TagInput
                    label="Amenities & Tags"
                    tags={form.amenities ?? []}
                    onChange={(tags) => setForm(prev => ({ ...prev, amenities: tags }))}
                />
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