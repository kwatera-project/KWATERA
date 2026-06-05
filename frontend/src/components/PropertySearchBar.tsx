import { useRef, useState } from "react";
import type { FormEvent } from "react";
import DatePicker from "react-datepicker";
import { CalendarDays, MapPin, Search, Users } from "lucide-react";
import SharedDatePicker from "./SharedDatePicker";

export interface PropertySearchValues {
    location: string;
    checkIn: Date | null;
    checkOut: Date | null;
    guests: string;
}

interface PropertySearchBarProps {
    initialValues?: Partial<PropertySearchValues>;
    onSearch: (values: PropertySearchValues) => void;
    className?: string;
}

export default function PropertySearchBar({
    initialValues,
    onSearch,
    className = "",
}: PropertySearchBarProps) {
    const [location, setLocation] = useState(initialValues?.location ?? "");
    const [checkIn, setCheckIn] = useState<Date | null>(initialValues?.checkIn ?? null);
    const [checkOut, setCheckOut] = useState<Date | null>(initialValues?.checkOut ?? null);
    const [guests, setGuests] = useState(initialValues?.guests ?? "");
    const checkOutRef = useRef<DatePicker | null>(null);

    const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();
        onSearch({
            location: location.trim(),
            checkIn,
            checkOut,
            guests: guests.trim(),
        });
    };

    return (
        <form
            onSubmit={handleSubmit}
            className={`relative z-[9999] w-full max-w-5xl bg-white rounded-3xl md:rounded-full shadow-xl border border-[#DACDCA] p-3 flex flex-col md:flex-row items-center divide-y md:divide-y-0 md:divide-x divide-gray-200 gap-y-2 md:gap-y-0 ${className}`}
        >
            <div className="flex-1 w-full px-6 py-3 flex flex-col items-start hover:bg-gray-50 rounded-full transition-colors relative group z-[100]">
                <label htmlFor="property-search-location" className="text-xs font-bold text-title uppercase tracking-wider mb-1 cursor-pointer flex items-center gap-1.5">
                    <MapPin size={14} className="text-[rgb(var(--color-burgundy))]" /> Location
                </label>
                <input
                    type="text"
                    id="property-search-location"
                    value={location}
                    onChange={(event) => setLocation(event.target.value)}
                    className="w-full bg-transparent text-title placeholder-gray-400 focus:outline-none font-medium text-lg"
                    placeholder="Warszawa, Zakopane..."
                    autoComplete="off"
                />
            </div>

            <div className="flex-1 w-full px-6 py-3 flex flex-col items-start hover:bg-gray-50 rounded-full transition-colors relative z-50">
                <label className="text-xs font-bold text-title uppercase tracking-wider mb-1 cursor-pointer flex items-center gap-1.5">
                    <CalendarDays size={14} className="text-[rgb(var(--color-burgundy))]" /> Check in
                </label>
                <SharedDatePicker
                    selected={checkIn}
                    onChange={(date: Date | null) => {
                        setCheckIn(date);
                        if (date && checkOut && date >= checkOut) {
                            setCheckOut(null);
                        }
                        if (date) {
                            setTimeout(() => {
                                checkOutRef.current?.setOpen(true);
                            }, 100);
                        }
                    }}
                    selectsStart
                    startDate={checkIn}
                    endDate={checkOut}
                    placeholderText="Add dates"
                    className="w-full text-sm text-primary bg-transparent focus:outline-none font-medium cursor-pointer"
                    dateFormat="dd/MM/yyyy"
                    minDate={new Date()}
                />
            </div>

            <div className="flex-1 w-full px-6 py-3 flex flex-col items-start hover:bg-gray-50 rounded-full transition-colors relative z-50">
                <label className="text-xs font-bold text-title uppercase tracking-wider mb-1 cursor-pointer flex items-center gap-1.5">
                    <CalendarDays size={14} className="text-[rgb(var(--color-burgundy))]" /> Check out
                </label>
                <SharedDatePicker
                    datepickerRef={checkOutRef}
                    selected={checkOut}
                    onChange={(date: Date | null) => setCheckOut(date)}
                    selectsEnd
                    startDate={checkIn}
                    endDate={checkOut}
                    minDate={checkIn || new Date()}
                    placeholderText="Add dates"
                    className="w-full text-sm text-primary bg-transparent focus:outline-none font-medium cursor-pointer"
                    dateFormat="dd/MM/yyyy"
                />
            </div>

            <div className="flex-1 w-full pl-6 pr-3 py-3 flex items-center justify-between hover:bg-gray-50 rounded-full transition-colors">
                <div className="flex flex-col items-start">
                    <label htmlFor="property-search-guests" className="text-xs font-bold text-title uppercase tracking-wider mb-1 cursor-pointer flex items-center gap-1.5">
                        <Users size={14} className="text-[rgb(var(--color-burgundy))]" /> Guests
                    </label>
                    <input
                        id="property-search-guests"
                        type="number"
                        min="1"
                        placeholder="Add guests"
                        value={guests}
                        onChange={(event) => setGuests(event.target.value)}
                        className="w-full text-sm text-primary bg-transparent focus:outline-none placeholder-gray-400 font-medium"
                    />
                </div>
                <button
                    type="submit"
                    aria-label="Search properties"
                    className="bg-[rgb(var(--color-burgundy))] text-white p-5 rounded-full hover:bg-[rgb(var(--color-burgundy-hover))] transition-all shadow-lg flex-shrink-0 ml-4"
                >
                    <Search size={24} />
                </button>
            </div>
        </form>
    );
}
