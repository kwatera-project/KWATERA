import { useEffect, useState } from "react";
import { getProperty, getUnits, getPropertyImages } from "../api/propertyApi";
import { useParams } from "react-router-dom";
import type { Unit, Property } from "../types/property";
import { getOccupiedDates } from "../api/availabilityApi";
import { createReservation } from "../api/reservationApi";
import { GATEWAY_BASE_URL } from "../api/apiConfig";
import DatePicker from "react-datepicker";
import "react-datepicker/dist/react-datepicker.css";
import { format } from "date-fns";

export default function PropertyDetailsPage() {
    const { id } = useParams();
    const [property, setProperty] = useState<Property | null>(null);
    const [units, setUnits] = useState<Unit[]>([]);
    const [images, setImages] = useState<string[]>([]);
    const [mainImage, setMainImage] = useState("");
    const [occupiedIntervals, setOccupiedIntervals] = useState<Record<string, { start: Date, end: Date }[]>>({});
    const [selectedDates, setSelectedDates] = useState<Record<string, [Date | null, Date | null]>>({});

    const [bookingState, setBookingState] = useState<
        Record<string, { loading: boolean; success?: boolean; error?: string }>
    >({});

    useEffect(() => {
        if (!id) return;
        getProperty(id).then(setProperty);
        getUnits(id).then((fetchedUnits: Unit[]) => {
            setUnits(fetchedUnits);
            fetchedUnits.forEach((u: Unit) => {
                getOccupiedDates(u.id).then((dates: { startDate: string, endDate: string }[]) => {
                    const intervals = dates.map(d => ({
                        start: new Date(d.startDate),
                        end: new Date(d.endDate)
                    }));
                    setOccupiedIntervals(prev => ({ ...prev, [u.id]: intervals }));
                });
            });
        });

        getPropertyImages(id).then((data) => {
            setImages(data);
            if (data.length > 0) {
                setMainImage(data[0]);
            }
        });
    }, [id]);

    const handleDateChange = (unitId: string, dates: [Date | null, Date | null]) => {
        setSelectedDates(prev => ({ ...prev, [unitId]: dates }));
        setBookingState(prev => ({ ...prev, [unitId]: { loading: false, error: undefined } }));
    };

    const handleBook = async (unitId: string) => {
        const dates = selectedDates[unitId];
        if (!dates || !dates[0] || !dates[1]) {
            setBookingState(prev => ({
                ...prev,
                [unitId]: { loading: false, error: "Select a valid date range on the calendar" }
            }));
            return;
        }

        const from = format(dates[0], 'yyyy-MM-dd');
        const to = format(dates[1], 'yyyy-MM-dd');

        setBookingState(prev => ({ ...prev, [unitId]: { loading: true } }));

        try {
            const res = await createReservation(unitId, from, to);
            setBookingState(prev => ({ ...prev, [unitId]: { loading: false, success: true } }));
            const token = localStorage.getItem("token");

            const checkoutRes = await fetch(
                `${GATEWAY_BASE_URL}/api/billing/checkout/${res.id}`,
                {
                    method: "POST",
                    headers: {
                        Authorization: `Bearer ${token}`,
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify({
                        type: "ACCOMMODATION",
                        description: "Accommodation fee",
                        quantity: 1,
                        unitPrice: res.totalPrice
                    })
                }
            );

            const checkoutUrl = await checkoutRes.text();
            window.location.assign(checkoutUrl);
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

            <h2 className="mt-6 text-xl font-bold">Units & Availability</h2>

            {units.map((u) => (
                <div key={u.id} className="bg-card rounded-xl mt-4 overflow-hidden p-4 border flex flex-col md:flex-row gap-6">
                    <div className="flex-1">
                        {u.imageUrl && (
                            <img src={u.imageUrl} className="w-full h-48 object-cover rounded mb-4" />
                        )}
                        <h3 className="font-bold text-lg">{u.name}</h3>
                        <p className="text-details">{u.description}</p>
                        <p className="mt-2 font-semibold text-blue-600">{u.pricePerNight} zł / night</p>
                        <p className="text-sm text-gray-500">Capacity: {u.capacity} {u.capacity === 1 ? "person" : "people"}</p>
                    </div>

                    <div className="flex-1 flex flex-col items-center md:items-start">
                        <p className="mb-2 font-semibold">Select dates to book:</p>
                        <DatePicker
                            selected={selectedDates[u.id]?.[0]}
                            onChange={(dates) => handleDateChange(u.id, dates)}
                            startDate={selectedDates[u.id]?.[0] || undefined}
                            endDate={selectedDates[u.id]?.[1] || undefined}
                            selectsRange
                            inline
                            minDate={new Date()}
                            excludeDateIntervals={occupiedIntervals[u.id] || []}
                        />

                        <div className="mt-4 w-full text-center md:text-left">
                            <button
                                onClick={() => handleBook(u.id)}
                                disabled={bookingState[u.id]?.loading || bookingState[u.id]?.success}
                                className={`px-6 py-2 font-bold rounded w-full md:w-auto ${
                                    bookingState[u.id]?.success
                                        ? "bg-green-500 text-white cursor-default"
                                        : bookingState[u.id]?.loading
                                            ? "bg-gray-400 text-white cursor-wait"
                                            : "bg-blue-600 text-white hover:bg-blue-700"
                                }`}
                            >
                                {bookingState[u.id]?.loading ? "Processing..." :
                                    bookingState[u.id]?.success ? "Redirecting to payment..." :
                                        "Book these dates"}
                            </button>

                            {bookingState[u.id]?.error && (
                                <p className="text-red-500 text-sm mt-2">{bookingState[u.id].error}</p>
                            )}
                        </div>
                    </div>
                </div>
            ))}
        </div>
    );
}