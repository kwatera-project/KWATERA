import { useEffect, useMemo, useState } from "react";
import { getProperty, getUnits, getPropertyImages } from "../api/propertyApi";
import { useParams, useSearchParams, useNavigate } from "react-router-dom";
import type { Unit, Property } from "../types/property";
import { checkAvailability, getOccupiedDates } from "../api/availabilityApi";
import DatePicker from "react-datepicker";
import "react-datepicker/dist/react-datepicker.css";
import { format } from "date-fns";
import { useCurrency } from "../contexts/CurrencyContext";
import { CustomCalendarHeader } from "../components/SharedDatePicker";
import { formatSearchDate, parseGuests, parseSearchDate } from "../utils/searchDates";

interface AvailabilityResponse {
    available: boolean;
    message: string;
}

interface PropertyImage {
    id: string;
    url: string;
    isMain: boolean;
}

export default function PropertyDetailsPage() {
    const { id } = useParams();
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();
    const initialSearch = useMemo(() => ({
        checkIn: parseSearchDate(searchParams.get("checkIn")),
        checkOut: parseSearchDate(searchParams.get("checkOut")),
        guests: parseGuests(searchParams.get("guests")),
    }), [searchParams]);
    const hasInitialDateRange = !!initialSearch.checkIn && !!initialSearch.checkOut && initialSearch.checkIn < initialSearch.checkOut;
    const [property, setProperty] = useState<Property | null>(null);
    const [units, setUnits] = useState<Unit[]>([]);
    const [images, setImages] = useState<PropertyImage[]>([]);
    const [mainImage, setMainImage] = useState("");
    const [occupiedIntervals, setOccupiedIntervals] = useState<Record<string, { start: Date, end: Date }[]>>({});
    const [selectedDates, setSelectedDates] = useState<Record<string, [Date | null, Date | null]>>({});
    const [globalDates, setGlobalDates] = useState<[Date | null, Date | null]>([initialSearch.checkIn, initialSearch.checkOut]);
    const [showCalendar, setShowCalendar] = useState<Record<string, boolean>>({});

    const [bookingState, setBookingState] = useState<
        Record<string, { loading: boolean; success?: boolean; error?: string }>
    >({});
    const { currency } = useCurrency();

    useEffect(() => {
        if (!id) return;
        getProperty(id).then(setProperty);
        getUnits(id, currency).then(async (fetchedUnits: Unit[]) => {
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

            if (!initialSearch.guests && !hasInitialDateRange) {
                return;
            }

            const nextSelectedDates: Record<string, [Date | null, Date | null]> = {};
            const nextBookingState: Record<string, { loading: boolean; success?: boolean; error?: string }> = {};
            const checkIn = initialSearch.checkIn ? formatSearchDate(initialSearch.checkIn) : null;
            const checkOut = initialSearch.checkOut ? formatSearchDate(initialSearch.checkOut) : null;

            await Promise.all(fetchedUnits.map(async (unit) => {
                if (initialSearch.guests && unit.capacity < initialSearch.guests) {
                    nextSelectedDates[unit.id] = [null, null];
                    nextBookingState[unit.id] = {
                        loading: false,
                        error: `This unit hosts up to ${unit.capacity} ${unit.capacity === 1 ? "person" : "people"}.`,
                    };
                    return;
                }

                if (!hasInitialDateRange || !initialSearch.checkIn || !initialSearch.checkOut || !checkIn || !checkOut) {
                    nextBookingState[unit.id] = { loading: false, error: undefined };
                    return;
                }

                try {
                    const availability: AvailabilityResponse = await checkAvailability(unit.id, checkIn, checkOut);
                    if (availability.available) {
                        nextSelectedDates[unit.id] = [initialSearch.checkIn, initialSearch.checkOut];
                        nextBookingState[unit.id] = { loading: false, error: undefined };
                    } else {
                        nextSelectedDates[unit.id] = [null, null];
                        nextBookingState[unit.id] = {
                            loading: false,
                            error: "Room not available for the selected search dates.",
                        };
                    }
                } catch {
                    nextSelectedDates[unit.id] = [null, null];
                    nextBookingState[unit.id] = {
                        loading: false,
                        error: "Unable to confirm availability for the selected search dates.",
                    };
                }
            }));

            setSelectedDates(prev => ({ ...prev, ...nextSelectedDates }));
            setBookingState(prev => ({ ...prev, ...nextBookingState }));
        });

        getPropertyImages(id)
            .then((data: PropertyImage[]) => {
                if (data && data.length > 0) {
                    const mainImgObject =
                        data.find((img) => img.isMain) ?? data[0];

                    setImages(data);
                    setMainImage(mainImgObject.url);
                }
            });
    }, [id, currency, initialSearch.checkIn, initialSearch.checkOut, initialSearch.guests, hasInitialDateRange]);

    const handleGlobalDateChange = (dates: [Date | null, Date | null]) => {
        setGlobalDates(dates);
        const [start, end] = dates;
        units.forEach((u) => {
            if (initialSearch.guests && u.capacity < initialSearch.guests) {
                setSelectedDates(prev => ({ ...prev, [u.id]: [null, null] }));
                setBookingState(prev => ({
                    ...prev,
                    [u.id]: {
                        loading: false,
                        error: `This unit hosts up to ${u.capacity} ${u.capacity === 1 ? "person" : "people"}.`
                    }
                }));
                return;
            }

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
        const unit = units.find((u) => u.id === unitId);
        if (unit && initialSearch.guests && unit.capacity < initialSearch.guests) {
            setSelectedDates(prev => ({ ...prev, [unitId]: [null, null] }));
            setBookingState(prev => ({
                ...prev,
                [unitId]: {
                    loading: false,
                    error: `This unit hosts up to ${unit.capacity} ${unit.capacity === 1 ? "person" : "people"}.`
                }
            }));
            return;
        }

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

    const handleBook = (unitId: string) => {
        const unit = units.find((u) => u.id === unitId);
        if (!unit) return;

        if (initialSearch.guests && unit.capacity < initialSearch.guests) {
            setBookingState(prev => ({
                ...prev,
                [unitId]: {
                    loading: false,
                    error: `This unit hosts up to ${unit.capacity} ${unit.capacity === 1 ? "person" : "people"}.`
                }
            }));
            return;
        }

        const dates = selectedDates[unitId];
        if (!dates || !dates[0] || !dates[1]) {
            setBookingState(prev => ({
                ...prev,
                [unitId]: { loading: false, error: "Select a valid date range on the calendar" }
            }));
            return;
        }

        const token = localStorage.getItem("token");
        if (!token) {
            setBookingState(prev => ({
                ...prev,
                [unitId]: { loading: false, error: "Log in to book this unit" }
            }));
            return;
        }

        const from = format(dates[0], 'yyyy-MM-dd');
        const to = format(dates[1], 'yyyy-MM-dd');
        const nights = calculateNights(dates[0], dates[1]);
        const totalPrice = nights * unit.pricePerNight;

        navigate("/checkout", {
            state: {
                property,
                unit,
                checkIn: from,
                checkOut: to,
                nights,
                totalPrice,
                currency
            }
        });
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
        <div className="max-w-7xl mx-auto p-4 md:p-8 min-h-screen text-brand-main space-y-8">
            <div>
                <img
                    src={mainImage || property.imageUrl}
                    className="w-full aspect-[21/9] object-cover rounded-xl border border-brand-accent shadow-sm"
                    alt={property.title}
                />
                <div className="flex flex-wrap gap-3 mt-4">
                    {images.map((img, i) => (
                        <img
                            key={i}
                            src={img.url}
                            onClick={() => setMainImage(img.url)}
                            className={`w-20 h-20 object-cover rounded-lg cursor-pointer border-2 transition-all hover:scale-105 ${
                                mainImage === img.url ? "border-brand-primary shadow-md" : "border-brand-accent hover:border-gray-400"
                            }`}
                            alt={`Property thumbnail ${i + 1}`}
                        />
                    ))}
                </div>
            </div>

            <div className="border-b border-brand-accent pb-6">
                <h1 className="text-3xl font-bold text-brand-main tracking-tight">{property.title}</h1>
                <p className="text-sm text-brand-muted mt-1 font-medium">{property.city}</p>
            </div>

            {property.amenities && property.amenities.length > 0 && (
                <div className="bg-white border border-brand-accent rounded-xl shadow-sm p-6">
                    <h2 className="text-xl font-bold text-brand-main tracking-tight mb-4">Amenities</h2>
                    <div className="flex flex-wrap gap-2">
                        {property.amenities.map((amenity, idx) => (
                            <span
                                key={idx}
                                className="inline-flex items-center px-3 py-1 rounded-full text-xs font-bold bg-brand-bg border border-brand-accent text-brand-muted"
                            >
                                {amenity}
                            </span>
                        ))}
                    </div>
                </div>
            )}

            <div className="bg-white border border-brand-accent rounded-xl shadow-sm p-6 hover:shadow-md transition-all duration-300 flex flex-col md:flex-row items-center justify-between gap-6">
                <div className="max-w-md text-center md:text-left space-y-1">
                    <h3 className="font-bold text-xl text-brand-main tracking-tight">Select Stay Dates</h3>
                    <p className="text-sm text-brand-muted leading-relaxed">Choose your preferred check-in and check-out window to check general availability across all options.</p>
                </div>
                <div className="flex justify-center w-full md:w-auto mt-4 md:mt-0">
                    <DatePicker
                        selected={globalDates[0]}
                        onChange={handleGlobalDateChange}
                        startDate={globalDates[0] || undefined}
                        endDate={globalDates[1] || undefined}
                        selectsRange
                        inline
                        minDate={new Date()}
                        calendarClassName="custom-datepicker-has-header occupancy-calendar"
                        renderCustomHeader={(props) => <CustomCalendarHeader {...props} />}
                    />
                </div>
            </div>

            <div className="space-y-6">
                <h2 className="text-2xl font-bold text-brand-main tracking-tight">Units & Availability</h2>

                <div className="space-y-6">
                    {units.map((u) => {
                        const unitStart = selectedDates[u.id]?.[0];
                        const unitEnd = selectedDates[u.id]?.[1];
                        const nights = calculateNights(unitStart, unitEnd);
                        const totalPrice = nights * u.pricePerNight;
                        const lacksRequestedCapacity = !!initialSearch.guests && u.capacity < initialSearch.guests;

                        return (
                            <div key={u.id} className="bg-white border border-brand-accent rounded-xl shadow-sm p-4 md:p-6 hover:shadow-md transition-all duration-300 flex flex-col lg:flex-row gap-8">
                                <div className="flex-1 space-y-4">
                                    {u.imageUrl && (
                                        <img src={u.imageUrl} className="w-full h-64 object-cover rounded-lg border border-brand-accent" alt={u.name} />
                                    )}
                                    <div className="space-y-2">
                                        <h3 className="font-bold text-xl text-brand-main">{u.name}</h3>
                                        <p className="text-sm text-brand-muted leading-relaxed">{u.description}</p>
                                        <div className="flex flex-wrap items-center gap-4 pt-2">
                                            <p className="text-lg font-bold text-brand-primary">
                                                {u.convertedPricePerNight && u.currencyInfo && u.currencyInfo.displayCurrency !== 'PLN'
                                                    ? `${u.convertedPricePerNight.toFixed(2)} ${u.currencyInfo.displayCurrency} / night`
                                                    : `${u.pricePerNight} PLN / night`}
                                            </p>
                                            <div className="flex flex-wrap gap-2">
                                                <span className="inline-flex items-center px-3 py-1 rounded-full text-xs font-bold bg-brand-bg border border-brand-accent text-brand-muted">
                                                    Capacity: {u.capacity} {u.capacity === 1 ? "person" : "people"}
                                                </span>
                                                {u.bedrooms !== undefined && (
                                                    <span className="inline-flex items-center px-3 py-1 rounded-full text-xs font-bold bg-brand-bg border border-brand-accent text-brand-muted">
                                                        Bedrooms: {u.bedrooms}
                                                    </span>
                                                )}
                                                {u.beds !== undefined && (
                                                    <span className="inline-flex items-center px-3 py-1 rounded-full text-xs font-bold bg-brand-bg border border-brand-accent text-brand-muted">
                                                        Beds: {u.beds}
                                                    </span>
                                                )}
                                            </div>
                                        </div>
                                        {u.amenities && u.amenities.length > 0 && (
                                            <div className="flex flex-wrap gap-2 mt-3">
                                                {u.amenities.map((amenity, idx) => (
                                                    <span key={idx} className="inline-flex items-center px-2.5 py-1 rounded-md text-xs font-medium bg-gray-100 text-gray-800 border border-gray-200">
                                                        {amenity}
                                                    </span>
                                                ))}
                                            </div>
                                        )}
                                    </div>
                                </div>

                                <div className="flex-1 flex flex-col items-stretch justify-between border-t lg:border-t-0 lg:border-l border-brand-accent pt-6 lg:pt-0 lg:pl-8">
                                    <div className="w-full flex flex-col items-center lg:items-start">
                                        <button
                                            onClick={() => toggleCalendar(u.id)}
                                            className="text-sm font-bold text-brand-primary hover:text-brand-primary-hover hover:underline mb-4 flex items-center gap-1.5 focus:outline-none"
                                        >
                                            <svg className="w-4 h-4" fill="none" stroke="currentColor" strokeWidth={2.5} viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 002-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" /></svg>
                                            {showCalendar[u.id] ? "Hide Detailed Calendar" : "Check Detailed Occupancy Calendar"}
                                        </button>

                                        {showCalendar[u.id] && (
                                            <div className="flex flex-col items-center lg:items-start w-full max-w-sm mt-2">
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
                                                    calendarClassName="custom-datepicker-has-header occupancy-calendar"
                                                    renderCustomHeader={(props) => <CustomCalendarHeader {...props} />}
                                                    renderDayContents={(dayOfMonth, date) => {
                                                        const cls = getDayClass(date, u.id);
                                                        let tooltipTitle = undefined;
                                                        if (cls === "checkin-day") tooltipTitle = "Available for check-out only";
                                                        if (cls === "checkout-day") tooltipTitle = "Available for check-in only";
                                                        return <span title={tooltipTitle} className="w-full h-full block align-middle pt-0.5">{dayOfMonth}</span>;
                                                    }}
                                                />
                                                <div className="flex gap-4 items-center justify-center mt-4 text-xs font-bold text-brand-muted w-full">
                                                    <div className="flex items-center gap-1.5">
                                                        <div className="w-3 h-3 bg-white border border-brand-accent rounded-sm"></div>
                                                        <span>Available</span>
                                                    </div>
                                                    <div className="flex items-center gap-1.5">
                                                        <div className="w-3 h-3 bg-[#e5e5e5] border border-brand-accent rounded-sm"></div>
                                                        <span>Occupied</span>
                                                    </div>
                                                    <div className="flex items-center gap-1.5">
                                                        <div className="w-3 h-3 bg-brand-accent border border-brand-primary rounded-sm"></div>
                                                        <span>Selected</span>
                                                    </div>
                                                </div>
                                            </div>
                                        )}
                                    </div>

                                    {unitStart && unitEnd && nights > 0 && (
                                        <div className="w-full bg-brand-bg border border-brand-accent rounded-xl p-4 mt-6 text-sm flex flex-col gap-2">
                                            <p className="text-xs font-bold text-brand-main tracking-wider border-b border-brand-accent pb-2 mb-1">Stay Details Summary</p>
                                            <p className="flex justify-between"><span className="text-brand-muted font-medium">Check-in:</span> <span className="font-semibold text-brand-main">{format(unitStart, "EEEE, MMM dd, yyyy")}</span></p>
                                            <p className="flex justify-between"><span className="text-brand-muted font-medium">Check-out:</span> <span className="font-semibold text-brand-main">{format(unitEnd, "EEEE, MMM dd, yyyy")}</span></p>
                                            <p className="flex justify-between"><span className="text-brand-muted font-medium">Duration:</span> <span className="font-semibold text-brand-main">{nights} {nights === 1 ? "night" : "nights"}</span></p>
                                            <div className="mt-2 pt-2 border-t border-brand-accent text-base font-bold text-brand-primary flex justify-between items-center">
                                                <span>Total price:</span>
                                                <div className="text-right">
                                                    {u.convertedPricePerNight && u.currencyInfo && u.currencyInfo.displayCurrency !== 'PLN' ? (
                                                        <div>{(nights * u.convertedPricePerNight).toFixed(2)} {u.currencyInfo.displayCurrency}</div>
                                                    ) : (
                                                        <span>{totalPrice} PLN</span>
                                                    )}
                                                </div>
                                            </div>
                                        </div>
                                    )}

                                    <div className="mt-6 w-full space-y-4">
                                        <button
                                            onClick={() => handleBook(u.id)}
                                            disabled={bookingState[u.id]?.loading || bookingState[u.id]?.success || lacksRequestedCapacity || !unitStart || !unitEnd}
                                            className={`w-full px-6 py-3 font-bold rounded-lg transition-all duration-200 shadow-sm cursor-pointer flex items-center justify-center gap-2 ${
                                                bookingState[u.id]?.success
                                                    ? "bg-green-600 text-white cursor-default"
                                                    : bookingState[u.id]?.loading
                                                        ? "bg-brand-muted text-white cursor-wait"
                                                        : (lacksRequestedCapacity || !unitStart || !unitEnd)
                                                            ? "bg-brand-accent text-brand-main opacity-50 cursor-not-allowed"
                                                            : "bg-brand-primary text-white hover:bg-brand-primary-hover"
                                            }`}
                                        >
                                            {bookingState[u.id]?.loading ? "Processing..." :
                                                bookingState[u.id]?.success ? "Redirecting to payment..." :
                                                    lacksRequestedCapacity ? "Capacity too low" :
                                                        (!unitStart || !unitEnd) ? "Select dates to book" : "Book these dates"}
                                        </button>

                                        {bookingState[u.id]?.error && (
                                            <div className="flex items-center gap-3 p-4 bg-red-50 border-l-4 border-red-500 rounded-r-xl text-red-700 animate-fade-in shadow-sm">
                                                <svg className="w-5 h-5 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
                                                    <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clipRule="evenodd"/>
                                                </svg>
                                                <span className="text-xs font-semibold leading-relaxed">{bookingState[u.id].error}</span>
                                            </div>
                                        )}
                                    </div>
                                </div>
                            </div>
                        );
                    })}
                </div>
            </div>
        </div>
    );
}
