import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

interface Slide {
    id: string;
    image: string;
}

interface SliderProps {
    slides: Slide[];
}

export default function Slider({ slides }: SliderProps) {
    const [index, setIndex] = useState(0);
    const navigate = useNavigate();

    useEffect(() => {
        if (!slides || slides.length === 0) return;
        const interval = setInterval(() => {
            setIndex(prev => (prev + 1) % slides.length);
        }, 5000);
        return () => clearInterval(interval);
    }, [slides]);

    function next() {
        if (slides.length === 0) return;
        setIndex(prev => (prev + 1) % slides.length);
    }

    function prev() {
        if (slides.length === 0) return;
        setIndex(prev => prev === 0 ? slides.length - 1 : prev - 1);
    }

    return (
        <div className="relative w-full h-[750px] md:h-[950px] overflow-hidden">
            <div
                className="absolute inset-0 cursor-pointer z-10"
                onClick={() => slides[index] && navigate(`/property/${slides[index].id}`)}
            >
                {slides.map((slide, i) => (
                    <img
                        key={slide.id + "_" + i}
                        src={slide.image}
                        className={`absolute w-full h-full object-cover transition-opacity duration-1000 ${
                            i === index ? "opacity-100" : "opacity-0"
                        }`}
                    />
                ))}
            </div>

            <div className="absolute bottom-0 left-0 w-full bg-card z-20 rounded-t-[50px] pt-24 pb-20 md:pt-12 md:pb-32 px-10 md:px-20 shadow-2xl">
                <div className="max-w-6xl mx-auto">
                    <h1 className="text-4xl md:text-5xl font-bold mb-3" style={{ color: 'rgb(var(--color-burgundy))' }}>
                        Good Morning!
                    </h1>
                    <p className="text-details text-lg md:text-xl font-medium">
                        Explore beautiful places in the world with Kwatera
                    </p>
                </div>
            </div>

            <button
                onClick={(e) => { e.stopPropagation(); prev(); }}
                className="absolute left-8 top-1/2 -translate-y-1/2 text-white text-5xl z-30 drop-shadow-md"
            >
                ◀
            </button>

            <button
                onClick={(e) => { e.stopPropagation(); next(); }}
                className="absolute right-8 top-1/2 -translate-y-1/2 text-white text-5xl z-30 drop-shadow-md"
            >
                ▶
            </button>
        </div>
    );
}