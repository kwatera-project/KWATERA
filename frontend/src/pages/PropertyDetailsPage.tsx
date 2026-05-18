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

    const isDateBlocked = (date: Date, unitId: string) => {
        const intervals = occupiedIntervals[unitId] || [];
        const d = format(date, 'yyyy-MM-dd');

        let isCheckin = false;
        let isCheckout = false;

        for (const interval of intervals) {
            const startStr = format(interval.start, 'yyyy-MM-dd');
            const endStr = format(interval.end, 'yyyy-MM-dd');

            if (d > startStr && d < endStr) return true;
            if (d === startStr) isCheckin = true;
            if (d === endStr) isCheckout = true;
        }

        return isCheckin && isCheckout;
    };

    const getDayClass = (date: Date, unitId: string) => {
        const intervals = occupiedIntervals[unitId] || [];
        const d = format(date, 'yyyy-MM-dd');

        let isCheckin = false;
        let isCheckout = false;

        for (const interval of intervals) {
            const startStr = format(interval.start, 'yyyy-MM-dd');
            const endStr = format(interval.end, 'yyyy-MM-dd');
            if (d === startStr) isCheckin = true;
            if (d === endStr) isCheckout = true;
        }

        if (isCheckin && !isCheckout) return "checkin-day";
        if (isCheckout && !isCheckin) return "checkout-day";
        return "";
    };

    const handleDateChange = (unitId: string, dates: [Date | null, Date | null]) => {
        const [start, end] = dates;

        if (start && end) {
            if (start.getTime() === end.getTime()) {
                setSelectedDates(prev => ({ ...prev, [unitId]: [start, null] }));
                setBookingState(prev => ({
                    ...prev,
                    [unitId]: { loading: false, error: "Minimum stay is 1 night. Select a different end date." }
                }));
                return;
            }

            const startStr = format(start, 'yyyy-MM-dd');
            const endStr = format(end, 'yyyy-MM-dd');
            const intervals = occupiedIntervals[unitId] || [];

            let hasOverlap = false;
            for (const interval of intervals) {
                const intStart = format(interval.start, 'yyyy-MM-dd');
                const intEnd = format(interval.end, 'yyyy-MM-dd');

                if (startStr < intEnd && endStr > intStart) {
                    hasOverlap = true;
                    break;
                }
            }

            if (hasOverlap) {
                setSelectedDates(prev => ({ ...prev, [unitId]: [start, null] }));
                setBookingState(prev => ({
                    ...prev,
                    [unitId]: { loading: false, error: "Selected dates overlap with an existing reservation." }
                }));
                return;
            }
        }

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
        <div className="max-w-5xl mx-auto p-6 text-[#1A1A1A]">
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
                        className="w-20 aspect-square object-cover rounded cursor-pointer border border-[#DACDCA]"
                    />
                ))}
            </div>

            <h1 className="text-3xl font-bold mt-4 text-[#1A1A1A]">{property.title}</h1>
            <p className="text-[#7A7A7A]">{property.location}</p>

            <h2 className="mt-6 text-xl font-bold text-[#1A1A1A]">Units & Availability</h2>

            {units.map((u) => (
                <div key={u.id} className="bg-[#F7F7F7] rounded-xl mt-4 overflow-hidden p-4 border border-[#DACDCA] flex flex-col md:flex-row gap-6">
                    <div className="flex-1">
                        {u.imageUrl && (
                            <img src={u.imageUrl} className="w-full h-48 object-cover rounded mb-4" />
                        )}
                        <h3 className="font-bold text-lg text-[#1A1A1A]">{u.name}</h3>
                        <p className="text-[#7A7A7A]">{u.description}</p>
                        <p className="mt-2 font-semibold text-[#42211D]">{u.pricePerNight} zł / night</p>
                        <p className="text-sm text-[#7A7A7A]">Capacity: {u.capacity} {u.capacity === 1 ? "person" : "people"}</p>
                    </div>

                    <div className="flex-1 flex flex-col items-center md:items-start">
                        <p className="mb-2 font-semibold text-[#1A1A1A]">Select dates to book:</p>

                        <div className="relative">
                            <DatePicker
                                selected={selectedDates[u.id]?.[0]}
                                onChange={(dates) => handleDateChange(u.id, dates)}
                                startDate={selectedDates[u.id]?.[0] || undefined}
                                endDate={selectedDates[u.id]?.[1] || undefined}
                                selectsRange
                                inline
                                minDate={new Date()}
                                filterDate={(date) => !isDateBlocked(date, u.id)}
                                dayClassName={(date) => getDayClass(date, u.id)}
                                previousMonthButtonLabel={
                                    <svg fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" d="M15 19l-7-7 7-7" /></svg>
                                }
                                nextMonthButtonLabel={
                                    <svg fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" d="M9 5l7 7-7 7" /></svg>
                                }
                                renderDayContents={(dayOfMonth, date) => {
                                    const cls = getDayClass(date, u.id);
                                    let tooltipTitle = undefined;
                                    if (cls === "checkin-day") tooltipTitle = "Available for check-in only";
                                    if (cls === "checkout-day") tooltipTitle = "Available for check-out only";
                                    return <span title={tooltipTitle} className="w-full h-full block align-middle pt-0.5">{dayOfMonth}</span>;
                                }}
                            />
                            <div className="flex gap-4 items-center justify-center mt-4 text-xs text-[#7A7A7A] w-full mb-2">
                                <div className="flex items-center gap-1.5">
                                    <div className="w-3 h-3 bg-[#FFFFFF] border border-[#DACDCA] rounded-sm"></div>
                                    <span>Available</span>
                                </div>
                                <div className="flex items-center gap-1.5">
                                    <div className="w-3 h-3 bg-[#e5e5e5] border border-[#DACDCA] rounded-sm"></div>
                                    <span>Occupied</span>
                                </div>
                                <div className="flex items-center gap-1.5">
                                    <div className="w-3 h-3 bg-[#DACDCA] border border-[#42211D] rounded-sm"></div>
                                    <span>Selected</span>
                                </div>
                            </div>
                        </div>

                        <div className="mt-2 w-full text-center md:text-left">
                            <button
                                onClick={() => handleBook(u.id)}
                                disabled={bookingState[u.id]?.loading || bookingState[u.id]?.success}
                                className={`px-6 py-2 font-bold rounded w-full md:w-auto transition-colors duration-200 ${
                                    bookingState[u.id]?.success
                                        ? "bg-green-600 text-white cursor-default"
                                        : bookingState[u.id]?.loading
                                            ? "bg-[#7A7A7A] text-white cursor-wait"
                                            : "bg-[#42211D] text-[#FFFFFF] hover:bg-[#2a1412]"
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