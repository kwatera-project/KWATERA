import { useEffect, useState } from "react";
import { getProperties } from "../api/propertyApi";
import type { Property } from "../types/property";
import { Link } from "react-router-dom";

export default function PropertiesPage() {
    const [properties, setProperties] = useState<Property[]>([]);

    useEffect(() => {
        getProperties().then(setProperties);
    }, []);

    return (
        <div className="p-8 max-w-7xl mx-auto min-h-screen text-[#1A1A1A]">
            <div className="mb-8">
                <h1 className="text-3xl font-bold text-[#1A1A1A] tracking-tight">Properties</h1>
                <p className="text-sm text-[#7A7A7A] mt-1">Explore our curated selection of properties.</p>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                {properties.map(p => (
                    <Link key={p.id} to={`/property/${p.id}`} className="group block">
                        <div className="bg-white border border-[#DACDCA] rounded-xl shadow-sm p-6 hover:shadow-md transition-all duration-300 transform hover:-translate-y-1">
                            <img src={p.imageUrl} className="w-full h-100 object-cover rounded-lg" alt={p.title} />
                            <h2 className="text-2xl font-bold text-[#1A1A1A] tracking-tight mt-4 group-hover:text-[#42211D] transition-colors">{p.title}</h2>
                            <p className="text-sm text-[#7A7A7A] mt-1 font-medium">{p.city}</p>
                        </div>
                    </Link>
                ))}
            </div>
        </div>
    );
}