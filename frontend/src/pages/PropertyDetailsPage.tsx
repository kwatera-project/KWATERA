import { useEffect, useState } from "react";
import { getProperty, getUnits, getPropertyImages } from "../api/propertyApi";
import { useParams } from "react-router-dom";
import type { Unit, Property } from "../types/property";
import { checkAvailability } from "../api/availabilityApi";
import { createReservation } from "../api/reservationApi";

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

    const [bookingState, setBookingState] = useState<
        Record<string, { loading: boolean; success?: boolean; error?: string }>
    >({});

    const [loading, setLoading] = useState(false);
    const today = new Date().toISOString().split("T")[0];
    const isInvalid = !from || !to || loading || new Date(from) >= new Date(to);

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

    const handleBook = async (unitId: string) => {
        setBookingState(prev => ({ ...prev, [unitId]: { loading: true } }));
        try {
            await createReservation(unitId, from, to);
            setBookingState(prev => ({ ...prev, [unitId]: { loading: false, success: true } }));
        } catch (err: unknown) {
            const message = err instanceof Error ? err.message : "An error occurred";
            setBookingState(prev => ({ ...prev, [unitId]: { loading: false, error: message } }));
        }
    };

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
                        setBookingState({});
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
                        setBookingState({});
                    }}
                    className="border p-2 rounded"
                />

                <button
                    disabled={isInvalid}
                    onClick={async () => {

                        if (!from || !to) return;
                        if (new Date(from) >= new Date(to)) return;

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
                        isInvalid
                            ? "bg-gray-300 cursor-not-allowed"
                            : "bg-blue-500 text-white"
                    }`}
                >
                    {loading ? "Checking..." : "Check availability"}
                </button>
                {from && to && new Date(from) >= new Date(to) && (
                    <p className="text-red-500 mt-2">
                        End date must be after start date
                    </p>
                )}

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

                    {availabilityMap[u.id]?.available && (
                        <div className="mt-4">
                            <button
                                onClick={() => handleBook(u.id)}
                                disabled={bookingState[u.id]?.loading || bookingState[u.id]?.success}
                                className={`px-4 py-2 font-bold rounded ${
                                    bookingState[u.id]?.success
                                        ? "bg-green-500 text-white cursor-default"
                                        : bookingState[u.id]?.loading
                                            ? "bg-gray-400 text-white cursor-wait"
                                            : "bg-blue-600 text-white hover:bg-blue-700"
                                }`}
                            >
                                {bookingState[u.id]?.loading ? "Processing..." :
                                    bookingState[u.id]?.success ? "Booked successfully!" :
                                        "Book this unit"}
                            </button>

                            {bookingState[u.id]?.error && (
                                <p className="text-red-500 text-sm mt-1">{bookingState[u.id].error}</p>
                            )}
                        </div>
                    )}

                </div>
            ))}

        </div>
    );
}