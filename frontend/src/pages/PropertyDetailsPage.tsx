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
import { useCurrency } from "../contexts/CurrencyContext";

export default function PropertyDetailsPage() {
    const { id } = useParams();
    const [property, setProperty] = useState<Property | null>(null);
    const [units, setUnits] = useState<Unit[]>([]);
    const [images, setImages] = useState<string[]>([]);
    const [mainImage, setMainImage] = useState("");
    const [occupiedIntervals, setOccupiedIntervals] = useState<Record<string, { start: Date, end: Date }[]>>({});
    const [selectedDates, setSelectedDates] = useState<Record<string, [Date | null, Date | null]>>({});
    const [globalDates, setGlobalDates] = useState<[Date | null, Date | null]>([null, null]);
    const [showCalendar, setShowCalendar] = useState<Record<string, boolean>>({});

    const [bookingState, setBookingState] = useState<
        Record<string, { loading: boolean; success?: boolean; error?: string }>
    >({});
    const { currency } = useCurrency();

    useEffect(() => {
        if (!id) return;
        getProperty(id).then(setProperty);
        getUnits(id, currency).then((fetchedUnits: Unit[]) => {
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
    }, [id, currency]);

    const handleGlobalDateChange = (dates: [Date | null, Date | null]) => {
        setGlobalDates(dates);
        const [start, end] = dates;
        units.forEach((u) => {
            if (start && end) {
                let hasOverlap = false;
                const startStr = format(start, 'yyyy-MM-dd');
                const endStr = format(end, 'yyyy-MM-dd');
                const intervals = occupiedIntervals[u.id] || [];
                for (const interval of intervals) {
                    const intStart = format(interval.start, 'yyyy-MM-dd');
                    const intEnd = format(interval.end, 'yyyy-MM-dd');
                    if (startStr < intEnd && endStr > intStart) {
                        hasOverlap = true;
                        break;
                    }
                }
                if (!hasOverlap && start.getTime() !== end.getTime()) {
                    setSelectedDates(prev => ({ ...prev, [u.id]: dates }));
                    setBookingState(prev => ({ ...prev, [u.id]: { loading: false, error: undefined } }));
                } else if (hasOverlap) {
                    setSelectedDates(prev => ({ ...prev, [u.id]: [null, null] }));
                    setBookingState(prev => ({
                        ...prev,
                        [u.id]: { loading: false, error: "Room not available for these dates. Check the detailed occupancy calendar above to find available slots." }
                    }));
                } else {
                    setSelectedDates(prev => ({ ...prev, [u.id]: [null, null] }));
                }
            } else {
                setSelectedDates(prev => ({ ...prev, [u.id]: dates }));
                setBookingState(prev => ({ ...prev, [u.id]: { loading: false, error: undefined } }));
            }
        });
    };

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
            const res = await createReservation(unitId, from, to, currency);
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

            if (!checkoutRes.ok) {
                throw new Error(`Checkout failed: ${checkoutRes.status}`);
            }

            const checkoutUrl = await checkoutRes.text();

            try {
                new URL(checkoutUrl);
            } catch {
                throw new Error("Invalid checkout URL received");
            }

            window.location.assign(checkoutUrl);

        } catch (err: unknown) {
            const message = err instanceof Error ? err.message : "An error occurred";
            setBookingState(prev => ({ ...prev, [unitId]: { loading: false, error: message } }));
        }
    };

    const calculateNights = (start: Date | null, end: Date | null) => {
        if (!start || !end) return 0;
        return Math.ceil((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24));
    };

    const toggleCalendar = (unitId: string) => {
        setShowCalendar(prev => ({ ...prev, [unitId]: !prev[unitId] }));
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

            <div className="bg-[#F7F7F7] border border-[#DACDCA] rounded-xl p-6 mt-6 flex flex-col md:flex-row items-center justify-between gap-6">
                <div className="max-w-xs text-center md:text-left">
                    <h3 className="font-bold text-xl">Select Stay Dates</h3>
                    <p className="text-sm text-[#7A7A7A] mt-1">Choose your preferred check-in and check-out window to check general availability across all options.</p>
                </div>
                <div className="flex justify-center w-full md:w-auto">
                    <DatePicker
                        selected={globalDates[0]}
                        onChange={handleGlobalDateChange}
                        startDate={globalDates[0] || undefined}
                        endDate={globalDates[1] || undefined}
                        selectsRange
                        inline
                        minDate={new Date()}
                        previousMonthButtonLabel={
                            <svg fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" d="M15 19l-7-7 7-7" /></svg>
                        }
                        nextMonthButtonLabel={
                            <svg fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" d="M9 5l7 7-7 7" /></svg>
                        }
                    />
                </div>
            </div>

            <h2 className="mt-8 text-xl font-bold text-[#1A1A1A]">Units & Availability</h2>

            {units.map((u) => {
                const unitStart = selectedDates[u.id]?.[0];
                const unitEnd = selectedDates[u.id]?.[1];
                const nights = calculateNights(unitStart, unitEnd);
                const totalPrice = nights * u.pricePerNight;

                return (
                    <div key={u.id} className="bg-[#F7F7F7] rounded-xl mt-4 overflow-hidden p-4 border border-[#DACDCA] flex flex-col md:flex-row gap-6">
                        <div className="flex-1">
                            {u.imageUrl && (
                                <img src={u.imageUrl} className="w-full h-48 object-cover rounded mb-4" />
                            )}
                            <h3 className="font-bold text-lg text-[#1A1A1A]">{u.name}</h3>
                            <p className="text-[#7A7A7A]">{u.description}</p>
                            <p className="mt-2 font-semibold text-[#42211D]">
                                {u.convertedPricePerNight && u.currencyInfo && u.currencyInfo.displayCurrency !== 'PLN' 
                                    ? `${u.convertedPricePerNight.toFixed(2)} ${u.currencyInfo.displayCurrency} / night` 
                                    : `${u.pricePerNight} zł / night`}
                            </p>
                            <p className="text-sm text-[#7A7A7A]">Capacity: {u.capacity} {u.capacity === 1 ? "person" : "people"}</p>
                        </div>

                        <div className="flex-1 flex flex-col items-center md:items-start justify-between">
                            <div className="w-full flex flex-col items-center md:items-start">
                                <button
                                    onClick={() => toggleCalendar(u.id)}
                                    className="text-sm font-semibold text-[#42211D] hover:underline mb-4 flex items-center gap-1 focus:outline-none"
                                >
                                    <svg className="w-4 h-4" fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 002-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" /></svg>
                                    {showCalendar[u.id] ? "Hide detailed occupancy calendar" : "Check detailed occupancy calendar"}
                                </button>

                                {showCalendar[u.id] && (
                                    <div className="relative">
                                        <DatePicker
                                            selected={unitStart}
                                            onChange={(dates) => handleDateChange(u.id, dates)}
                                            startDate={unitStart || undefined}
                                            endDate={unitEnd || undefined}
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
                                                if (cls === "checkin-day") tooltipTitle = "Available for check-out only";
                                                if (cls === "checkout-day") tooltipTitle = "Available for check-in only";
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
                                )}
                            </div>

                            {unitStart && unitEnd && nights > 0 && (
                                <div className="w-full bg-white border border-[#DACDCA] rounded-xl p-4 mt-4 text-sm flex flex-col gap-1">
                                    <p className="font-bold text-[#1A1A1A] border-b border-[#F7F7F7] pb-1 mb-1">Stay Details Summary</p>
                                    <p><span className="text-[#7A7A7A]">Check-in:</span> <span className="font-medium">{format(unitStart, "EEEE, MMMM dd, yyyy")}</span></p>
                                    <p><span className="text-[#7A7A7A]">Check-out:</span> <span className="font-medium">{format(unitEnd, "EEEE, MMMM dd, yyyy")}</span></p>
                                    <p><span className="text-[#7A7A7A]">Duration:</span> <span className="font-medium">{nights} {nights === 1 ? "night" : "nights"}</span></p>
                                    <div className="mt-1 pt-1 border-t border-[#F7F7F7] text-base font-bold text-[#42211D] flex justify-between items-center">
                                        <span>Total price:</span>
                                        <div className="text-right">
                                            {u.convertedPricePerNight && u.currencyInfo && u.currencyInfo.displayCurrency !== 'PLN' ? (
                                                <div>{(nights * u.convertedPricePerNight).toFixed(2)} {u.currencyInfo.displayCurrency}</div>
                                            ) : (
                                                <span>{totalPrice} zł</span>
                                            )}
                                        </div>
                                    </div>
                                </div>
                            )}

                            <div className="mt-4 w-full text-center md:text-left">
                                <button
                                    onClick={() => handleBook(u.id)}
                                    disabled={bookingState[u.id]?.loading || bookingState[u.id]?.success || !unitStart || !unitEnd}
                                    className={`px-6 py-2 font-bold rounded w-full md:w-auto transition-colors duration-200 ${
                                        bookingState[u.id]?.success
                                            ? "bg-green-600 text-white cursor-default"
                                            : bookingState[u.id]?.loading
                                                ? "bg-[#7A7A7A] text-white cursor-wait"
                                                : (!unitStart || !unitEnd)
                                                    ? "bg-[#DACDCA] text-[#7A7A7A] cursor-not-allowed"
                                                    : "bg-[#42211D] text-[#FFFFFF] hover:bg-[#2a1412]"
                                    }`}
                                >
                                    {bookingState[u.id]?.loading ? "Processing..." :
                                        bookingState[u.id]?.success ? "Redirecting to payment..." :
                                            (!unitStart || !unitEnd) ? "Select dates to book" : "Book these dates"}
                                </button>

                                {bookingState[u.id]?.error && (
                                    <p className="text-red-500 text-sm mt-2">{bookingState[u.id].error}</p>
                                )}
                            </div>
                        </div>
                    </div>
                );
            })}
        </div>
    );
}