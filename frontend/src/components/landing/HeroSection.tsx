import { useState } from 'react';
import { Search, MapPin, CalendarDays, Users } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import SharedDatePicker from "../SharedDatePicker";

export default function HeroSection() {
    const [location, setLocation] = useState('');
    const [checkIn, setCheckIn] = useState<Date | null>(null);
    const [checkOut, setCheckOut] = useState<Date | null>(null);
    const [guests, setGuests] = useState('2');
    const navigate = useNavigate();

    const handleSearch = () => {
        const params = new URLSearchParams();
        if (location) params.append('location', location);
        if (checkIn) params.append('checkIn', checkIn.toISOString().split('T')[0]);
        if (checkOut) params.append('checkOut', checkOut.toISOString().split('T')[0]);
        if (guests) params.append('guests', guests);

        navigate(`/catalog?${params.toString()}`);
    };

    return (
        <section className="relative z-40 w-full min-h-[85vh] flex flex-col justify-end bg-card">

            <div className="absolute inset-0 w-full h-full z-0 bg-black overflow-hidden">
                <div className="absolute inset-0 w-full h-full">
                    <img
                        src="https://images.pexels.com/photos/37100579/pexels-photo-37100579.jpeg"
                        alt="Hero Background"
                        className="w-full h-full object-cover opacity-90"
                    />
                </div>
                <div className="absolute inset-0 bg-gradient-to-b from-black/50 via-black/20 to-black/60" />
            </div>

            <div className="relative z-[9999] w-full bg-card rounded-t-[50px] pt-10 pb-16 px-4 md:px-8 lg:px-16 mt-auto">
                <div className="max-w-7xl mx-auto flex flex-col items-center text-center">
                    <h1 className="text-4xl md:text-5xl lg:text-7xl font-black text-title mb-6 drop-shadow-sm" style={{ color: 'rgb(var(--color-burgundy))' }}>Good Morning!</h1>
                    <p className="text-details text-lg md:text-2xl mb-8 max-w-2xl font-medium drop-shadow-sm">
                        Explore beautiful places in the world with Kwatera
                    </p>

                    <div className="relative z-[9999] w-full max-w-5xl bg-white rounded-3xl md:rounded-full shadow-xl border border-[#DACDCA] p-3 flex flex-col md:flex-row items-center divide-y md:divide-y-0 md:divide-x divide-gray-200 gap-y-2 md:gap-y-0">

                        <div className="flex-1 w-full px-6 py-3 flex flex-col items-start hover:bg-gray-50 rounded-full transition-colors relative group z-[100]">
                            <label htmlFor="location" className="text-xs font-bold text-title uppercase tracking-wider mb-1 cursor-pointer flex items-center gap-1.5">
                                <MapPin size={14} className="text-[rgb(var(--color-burgundy))]" /> Location
                            </label>
                            <input
                                type="text"
                                id="location"
                                value={location}
                                onChange={(e) => setLocation(e.target.value)}
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
                                onChange={(date: Date | null) => setCheckIn(date)}
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
                                <label htmlFor="guests" className="text-xs font-bold text-title uppercase tracking-wider mb-1 cursor-pointer flex items-center gap-1.5">
                                    <Users size={14} className="text-[rgb(var(--color-burgundy))]" /> Guests
                                </label>
                                <input
                                    id="guests"
                                    type="number"
                                    min="1"
                                    placeholder="Add guests"
                                    value={guests}
                                    onChange={(e) => setGuests(e.target.value)}
                                    className="w-full text-sm text-primary bg-transparent focus:outline-none placeholder-gray-400 font-medium"
                                />
                            </div>
                            <button
                                onClick={handleSearch}
                                className="bg-[rgb(var(--color-burgundy))] text-white p-5 rounded-full hover:bg-[rgb(var(--color-burgundy-hover))] transition-all shadow-lg flex-shrink-0 ml-4"
                            >
                                <Search size={24} />
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        </section>
    );
}