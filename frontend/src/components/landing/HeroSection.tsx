import { useNavigate } from 'react-router-dom';
import PropertySearchBar, { type PropertySearchValues } from "../PropertySearchBar";
import { formatSearchDate } from "../../utils/searchDates";

export default function HeroSection() {
    const navigate = useNavigate();

    const handleSearch = ({ location, checkIn, checkOut }: PropertySearchValues) => {
        const params = new URLSearchParams();
        if (location) params.append('location', location);
        if (checkIn) params.append('checkIn', formatSearchDate(checkIn));
        if (checkOut) params.append('checkOut', formatSearchDate(checkOut));

        navigate(`/properties${params.toString() ? `?${params.toString()}` : ''}`);
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

                    <PropertySearchBar onSearch={handleSearch} />
                </div>
            </div>
        </section>
    );
}
