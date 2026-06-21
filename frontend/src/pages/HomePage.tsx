import { useEffect, useState } from "react";
import { getProperties } from "../api/propertyApi";
import HeroSection from '../components/landing/HeroSection';
import FeaturesSection from '../components/landing/FeaturesSection';
import TopPropertiesSection from '../components/landing/TopPropertiesSection';
import ExploreSection from '../components/landing/ExploreSection';
import BlogSection from '../components/landing/BlogSection';
import NewsletterSection from '../components/landing/NewsletterSection';
import TrustBadges from '../components/landing/TrustBadges';

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
            <FeaturesSection />
            <TopPropertiesSection properties={properties} />
            <ExploreSection />
            <BlogSection />
            <NewsletterSection />
            <TrustBadges />
        </div>
    );
}