import { Minus, Plus, SlidersHorizontal, X } from "lucide-react";
import {
    COMMON_AMENITIES,
    EMPTY_FILTERS,
    PROPERTY_TYPES,
    type FilterState,
} from "../types/filters";

interface FilterSidebarProps {
    filters: FilterState;
    onFiltersChange: (filters: FilterState) => void;
    isOpen: boolean;
    onClose: () => void;
    currency: string;
}

/* ─── Building blocks ──────────────────────────────────────────────────── */

function Section({ title, children }: { title: string; children: React.ReactNode }) {
    return (
        <div className="py-5 border-b border-gray-100 last:border-0 last:pb-2">
            <p className="text-[10px] font-bold text-[#7A7A7A] uppercase tracking-[0.12em] mb-3.5">
                {title}
            </p>
            {children}
        </div>
    );
}

function CheckRow({
    label,
    checked,
    onChange,
}: {
    label: string;
    checked: boolean;
    onChange: () => void;
}) {
    return (
        <label className="flex items-center gap-3 cursor-pointer group select-none py-0.5">
            <input type="checkbox" checked={checked} onChange={onChange} className="sr-only" />
            <span
                className={`w-[18px] h-[18px] rounded-[5px] flex-shrink-0 flex items-center justify-center border-2 transition-all duration-150 ${
                    checked
                        ? "bg-[#42211D] border-[#42211D]"
                        : "border-gray-300 bg-white group-hover:border-[#42211D]/50"
                }`}
            >
                {checked && (
                    <svg className="w-2.5 h-2.5 text-white" viewBox="0 0 10 10" fill="none">
                        <path
                            d="M1.5 5l2.5 2.5 4.5-4.5"
                            stroke="currentColor"
                            strokeWidth="1.8"
                            strokeLinecap="round"
                            strokeLinejoin="round"
                        />
                    </svg>
                )}
            </span>
            <span
                className={`text-sm leading-snug transition-colors duration-150 ${
                    checked ? "text-[#42211D] font-semibold" : "text-[#3A3A3A] group-hover:text-[#42211D]"
                }`}
            >
                {label}
            </span>
        </label>
    );
}

function Stepper({
    label,
    value,
    min = 0,
    onChange,
}: {
    label: string;
    value: number;
    min?: number;
    onChange: (val: number) => void;
}) {
    return (
        <div className="flex items-center justify-between py-1">
            <span className="text-sm text-[#3A3A3A] font-medium">{label}</span>
            <div className="flex items-center gap-2.5">
                <button
                    type="button"
                    onClick={() => onChange(Math.max(min, value - 1))}
                    disabled={value <= min}
                    aria-label={`Decrease ${label}`}
                    className="w-8 h-8 rounded-full border-2 border-gray-200 flex items-center justify-center text-[#1A1A1A] hover:border-[#42211D] hover:text-[#42211D] disabled:opacity-30 disabled:cursor-not-allowed transition-all cursor-pointer"
                >
                    <Minus className="w-3.5 h-3.5" strokeWidth={2.5} />
                </button>
                <span className="w-6 text-center text-sm font-bold text-[#1A1A1A] tabular-nums select-none">
                    {value}
                </span>
                <button
                    type="button"
                    onClick={() => onChange(value + 1)}
                    aria-label={`Increase ${label}`}
                    className="w-8 h-8 rounded-full border-2 border-gray-200 flex items-center justify-center text-[#1A1A1A] hover:border-[#42211D] hover:text-[#42211D] transition-all cursor-pointer"
                >
                    <Plus className="w-3.5 h-3.5" strokeWidth={2.5} />
                </button>
            </div>
        </div>
    );
}

/* ─── Content (shared between desktop + mobile drawer) ─────────────────── */

function SidebarContent({
    filters,
    onFiltersChange,
    onClose,
    currency,
    isMobile,
}: FilterSidebarProps & { isMobile: boolean }) {
    const activeCount =
        filters.selectedAmenities.length +
        filters.propertyTypes.length +
        (filters.minPrice !== "" ? 1 : 0) +
        (filters.maxPrice !== "" ? 1 : 0) +
        (filters.guests > 1 ? 1 : 0) +
        (filters.bedrooms > 0 ? 1 : 0) +
        (filters.beds > 0 ? 1 : 0);

    const toggle = (key: "selectedAmenities" | "propertyTypes", value: string) => {
        const arr = filters[key] as string[];
        const updated = arr.includes(value) ? arr.filter((v) => v !== value) : [...arr, value];
        onFiltersChange({ ...filters, [key]: updated });
    };

    return (
        <div className="flex flex-col h-full">
            {/* Header */}
            <div className="flex items-center justify-between pb-4 border-b border-gray-100 flex-shrink-0">
                <div className="flex items-center gap-2.5">
                    <SlidersHorizontal className="w-4 h-4 text-[#42211D]" strokeWidth={2.5} />
                    <span className="text-sm font-bold text-[#1A1A1A]">Filters</span>
                    {activeCount > 0 && (
                        <span className="bg-[#42211D] text-white text-[10px] font-bold w-5 h-5 rounded-full flex items-center justify-center leading-none">
                            {activeCount}
                        </span>
                    )}
                </div>
                <div className="flex items-center gap-3">
                    {activeCount > 0 && (
                        <button
                            onClick={() => onFiltersChange(EMPTY_FILTERS)}
                            className="text-xs font-semibold text-[#42211D] hover:underline cursor-pointer"
                        >
                            Clear all
                        </button>
                    )}
                    {isMobile && (
                        <button
                            onClick={onClose}
                            className="p-1.5 rounded-lg hover:bg-gray-100 transition-colors cursor-pointer"
                            aria-label="Close filters"
                        >
                            <X className="w-4 h-4 text-[#7A7A7A]" />
                        </button>
                    )}
                </div>
            </div>

            {/* Scrollable body */}
            <div className="flex-1 overflow-y-auto">
                {/* Property Type */}
                <Section title="Property Type">
                    <div className="space-y-2">
                        {PROPERTY_TYPES.map((type) => (
                            <CheckRow
                                key={type}
                                label={type}
                                checked={filters.propertyTypes.includes(type)}
                                onChange={() => toggle("propertyTypes", type)}
                            />
                        ))}
                    </div>
                </Section>

                {/* Capacity */}
                <Section title="Capacity">
                    <div className="space-y-1 divide-y divide-gray-50">
                        <Stepper
                            label="Guests"
                            value={filters.guests}
                            min={1}
                            onChange={(v) => onFiltersChange({ ...filters, guests: v })}
                        />
                        <Stepper
                            label="Bedrooms"
                            value={filters.bedrooms}
                            min={0}
                            onChange={(v) => onFiltersChange({ ...filters, bedrooms: v })}
                        />
                        <Stepper
                            label="Beds"
                            value={filters.beds}
                            min={0}
                            onChange={(v) => onFiltersChange({ ...filters, beds: v })}
                        />
                    </div>
                </Section>

                {/* Price Range */}
                <Section title="Price per Night">
                    <div className="flex items-end gap-2.5">
                        <div className="flex-1">
                            <p className="text-[10px] font-semibold text-[#7A7A7A] uppercase tracking-wider mb-1.5">Min</p>
                            <div className="relative">
                                <span className="absolute left-2.5 top-1/2 -translate-y-1/2 text-xs font-semibold text-[#7A7A7A] pointer-events-none">
                                    {currency}
                                </span>
                                <input
                                    type="number"
                                    min="0"
                                    placeholder="0"
                                    value={filters.minPrice}
                                    onChange={(e) => onFiltersChange({ ...filters, minPrice: e.target.value })}
                                    className="w-full pl-9 pr-2 py-2.5 text-sm border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-[#42211D]/20 focus:border-[#42211D]/60 transition-all bg-white text-[#1A1A1A] appearance-none"
                                />
                            </div>
                        </div>
                        <span className="text-[#7A7A7A] text-sm pb-2.5 flex-shrink-0">—</span>
                        <div className="flex-1">
                            <p className="text-[10px] font-semibold text-[#7A7A7A] uppercase tracking-wider mb-1.5">Max</p>
                            <div className="relative">
                                <span className="absolute left-2.5 top-1/2 -translate-y-1/2 text-xs font-semibold text-[#7A7A7A] pointer-events-none">
                                    {currency}
                                </span>
                                <input
                                    type="number"
                                    min="0"
                                    placeholder="Any"
                                    value={filters.maxPrice}
                                    onChange={(e) => onFiltersChange({ ...filters, maxPrice: e.target.value })}
                                    className="w-full pl-9 pr-2 py-2.5 text-sm border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-[#42211D]/20 focus:border-[#42211D]/60 transition-all bg-white text-[#1A1A1A] appearance-none"
                                />
                            </div>
                        </div>
                    </div>
                </Section>

                {/* Amenities */}
                <Section title="Amenities">
                    <div className="space-y-2">
                        {COMMON_AMENITIES.map((amenity) => (
                            <CheckRow
                                key={amenity}
                                label={amenity}
                                checked={filters.selectedAmenities.includes(amenity)}
                                onChange={() => toggle("selectedAmenities", amenity)}
                            />
                        ))}
                    </div>
                </Section>
            </div>
        </div>
    );
}

/* ─── Public component ──────────────────────────────────────────────────── */

export default function FilterSidebar(props: FilterSidebarProps) {
    const { isOpen, onClose } = props;

    return (
        <>
            {/* Desktop sticky sidebar */}
            <aside className="hidden md:block w-[268px] flex-shrink-0 self-start sticky top-24">
                <div className="bg-white border border-gray-100 rounded-2xl shadow-sm p-5 flex flex-col overflow-hidden" style={{ height: 'calc(100vh - 7rem)' }}>
                    <SidebarContent {...props} isMobile={false} />
                </div>
            </aside>

            {/* Mobile slide-over drawer */}
            {isOpen && (
                <div className="md:hidden fixed inset-0 z-50 flex">
                    <div
                        className="absolute inset-0 bg-black/40 backdrop-blur-[2px]"
                        onClick={onClose}
                    />
                    <div className="relative z-10 w-[85vw] max-w-xs bg-white h-full shadow-2xl flex flex-col p-5 overflow-hidden">
                        <SidebarContent {...props} isMobile={true} />
                    </div>
                </div>
            )}
        </>
    );
}
