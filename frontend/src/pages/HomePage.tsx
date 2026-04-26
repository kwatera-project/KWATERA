import { useEffect, useState } from "react";
import { getProperties } from "../api/propertyApi";
import Slider from "../components/Slider";

export default function HomePage() {
    const [slides, setSlides] = useState<{ id: string; image: string }[]>([]);

    useEffect(() => {
        async function load() {
            try {
                const response = await getProperties();

                const props = Array.isArray(response)
                    ? response
                    : response.content || response.data || [];

                const allSlides = props
                    .filter((p: any) => p.imageUrl)
                    .map((p: any) => ({
                        id: p.id,
                        image: p.imageUrl
                    }));

                setSlides(allSlides);

            } catch (err) {
                console.log("ERROR:", err);
            }
        }

        load();
    }, []);

    return (
        <div className="bg-card">
            <Slider slides={slides} />
        </div>
    );
}