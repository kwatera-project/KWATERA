import { useEffect, useState } from "react";
import { getProperty, getUnits, getPropertyImages } from "../api/propertyApi";
import { useParams } from "react-router-dom";
import type { Unit, Property } from "../types/property";
import { checkAvailability } from "../api/availabilityApi";

export default function PropertyDetailsPage() {

    const { id } = useParams();
    const [property, setProperty] = useState<Property | null>(null);
    const [units, setUnits] = useState<Unit[]>([]);
    const [images, setImages] = useState<string[]>([]);
    const [mainImage, setMainImage] = useState("");
    const [from, setFrom] = useState("");
    const [to, setTo] = useState("");

    const [availabilityMap, setAvailabilityMap] = useState<
        Record<string, { available: boolean; message: string }>
    >({});

    const [loading, setLoading] = useState(false);
    const today = new Date().toISOString().split("T")[0];

    useEffect(() => {
        if (!id) return;

        getProperty(id).then(setProperty);
        getUnits(id).then(setUnits);

        getPropertyImages(id).then((data) => {
            setImages(data);
            if (data.length > 0) {
                setMainImage(data[0]);
            }
        });
    }, [id]);

    if (!property) {
        return <div className="p-6">Loading...</div>;
    }

    return (
        <div className="max-w-5xl mx-auto p-6">
            <img
                src={mainImage || property.imageUrl}
                className="w-full aspect-[16/9] object-cover rounded"
            />
            <div className="flex gap-2 mt-2">
                {images.map((img, i) => (
                    <img
                        key={i}
                        src={img}
                        onClick={() => setMainImage(img)}
                        className="w-20 aspect-square object-cover rounded cursor-pointer border"
                    />
                ))}
            </div>

            <h1 className="text-3xl font-bold mt-4">{property.title}</h1>
            <p className="text-details">{property.location}</p>

            <div className="mb-6 flex gap-2 mt-4">

                <input
                    type="date"
                    min={today}
                    value={from}
                    onChange={(e) => {
                        setFrom(e.target.value);
                        setAvailabilityMap({});
                    }}
                    className="border p-2 rounded"
                />

                <input
                    type="date"
                    min={from || today}
                    value={to}
                    onChange={(e) => {
                        setTo(e.target.value);
                        setAvailabilityMap({});
                    }}
                    className="border p-2 rounded"
                />

                <button
                    disabled={!from || !to || loading}
                    onClick={async () => {

                        if (!from || !to) return;

                        setLoading(true);

                        try {
                            const promises = units.map(u =>
                                checkAvailability(u.id, from, to)
                                    .then(res => ({
                                        id: u.id,
                                        available: res.available,
                                        message: res.message
                                    }))
                                    .catch((err) => ({
                                        id: u.id,
                                        available: false,
                                        message: err.message || "Error"
                                    }))
                            );

                            const results = await Promise.all(promises);

                            const resultMap: Record<string, { available: boolean; message: string }> = {};

                            results.forEach(r => {
                                resultMap[r.id] = {
                                    available: r.available,
                                    message: r.message
                                };
                            });

                            setAvailabilityMap(resultMap);

                        } finally {
                            setLoading(false);
                        }
                    }}
                    className={`px-4 py-2 rounded ${
                        !from || !to
                            ? "bg-gray-300"
                            : "bg-blue-500 text-white"
                    }`}
                >
                    {loading ? "Checking..." : "Check availability"}
                </button>

            </div>

            {loading && (
                <p className="text-gray-500 mt-2">Checking availability...</p>
            )}

            <h2 className="mt-6 text-xl font-bold">Units</h2>

            {units.map((u) => (
                <div
                    key={u.id}
                    className="bg-card rounded-xl mt-4 overflow-hidden p-4"
                >

                    {u.imageUrl && (
                        <img
                            src={u.imageUrl}
                            className="w-full aspect-[16/9] object-cover rounded"
                        />
                    )}

                    <h3 className="font-bold mt-2">{u.name}</h3>
                    <p className="text-details">{u.description}</p>

                    <p className="mt-2">{u.pricePerNight} zł</p>
                    <p>
                        {u.capacity} {u.capacity === 1 ? "person" : "people"}
                    </p>

                    {availabilityMap[u.id] && (
                        <p
                            className={
                                availabilityMap[u.id].available
                                    ? "text-green-600 mt-2"
                                    : "text-red-600 mt-2"
                            }
                        >
                            {availabilityMap[u.id].message}
                        </p>
                    )}

                </div>
            ))}

        </div>
    );
}