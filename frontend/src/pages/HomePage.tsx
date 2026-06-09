import { useEffect, useState } from "react";
import { getProperties } from "../api/propertyApi";
import HeroSection from '../components/landing/HeroSection';
import FeaturesSection from '../components/landing/FeaturesSection';
import TopPropertiesSection from '../components/landing/TopPropertiesSection';
import ExploreSection from '../components/landing/ExploreSection';
import BlogSection from '../components/landing/BlogSection';
import NewsletterSection from '../components/landing/NewsletterSection';
import TrustBadges from '../components/landing/TrustBadges';
import Footer from '../components/landing/Footer';
import DemoRoleSelector from "../components/DemoRoleSelector";
import { IS_DEMO_MODE } from "../api/apiConfig";

interface PropertyData {
    id: string;
    imageUrl: string;
    [key: string]: unknown;
}

export default function HomePage() {
    const [properties, setProperties] = useState<PropertyData[]>([]);

    useEffect(() => {
        async function load() {
            try {
                const response = await getProperties();
                const props = Array.isArray(response)
                    ? response
                    : response.content || response.data || [];

                setProperties(props);


            } catch (err) {
                console.log("ERROR:", err);
            }
        }
        load();
    }, []);

    return (
        <div className="w-full bg-card font-sans text-title">
            <HeroSection />
            {IS_DEMO_MODE && (
                <div className="max-w-7xl mx-auto px-4 md:px-8 lg:px-16 -mt-24 relative z-20 pb-10">
                    <div className="w-full bg-[#42211D] border border-white/20 shadow-xl rounded-xl p-4 sm:p-5 flex flex-col lg:flex-row lg:items-center gap-4 justify-between">
                        <div>
                            <p className="text-white text-lg font-black tracking-tight">Poster demo</p>
                            <p className="text-white/80 text-sm font-medium">Wybierz rolę i przejdź przez główne ekrany bez hasła.</p>
                        </div>
                        <DemoRoleSelector compact />
                    </div>
                </div>
            )}
            <FeaturesSection />
            <TopPropertiesSection properties={properties} />
            <ExploreSection />
            <BlogSection />
            <NewsletterSection />
            <TrustBadges />
            <Footer />
        </div>
    );
}
