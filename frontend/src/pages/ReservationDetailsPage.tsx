import { useEffect, useState } from "react";
import { useParams, Link } from "react-router-dom";
import { getReservationDetails } from "../api/reservationApi";
import type { ReservationDetails } from "../types/reservation";
import { getUserRoles } from "../utils/jwtUtils";
import { Home, Calendar, User, CreditCard, Clock } from "lucide-react";

export default function ReservationDetailsPage() {
    const { id } = useParams();
    const [reservation, setReservation] = useState<ReservationDetails | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        if (!id) return;

        getReservationDetails(id)
            .then(setReservation)
            .catch((err) => setError(err.message))
            .finally(() => setLoading(false));
    }, [id]);

    if (loading) return <div className="p-8 text-center text-gray-500 font-medium">Loading reservation details...</div>;
    if (error) return <div className="p-8 text-center text-red-600 font-semibold">{error}</div>;
    if (!reservation) return <div className="p-8 text-center text-gray-500 font-semibold">Reservation not found.</div>;

    const token = localStorage.getItem("token");
    const roles = getUserRoles(token);
    const isAdminOrOwner = roles.includes("ROLE_ADMIN") || roles.includes("ROLE_OWNER");
    const displayCurrency = reservation.currencyInfo?.displayCurrency || 'PLN';
    const returnPath = isAdminOrOwner ? "/admin/reservations" : "/my-reservations";
    const returnLabel = isAdminOrOwner ? "Back to Reservations Overview" : "Back to My Reservations";

    const formatGuestName = (name: string) => {
        if (!name) return "";
        const uuidRegex = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
        if (uuidRegex.test(name)) {
            return `#GST-${name.slice(-8)}`;
        }
        if (name.startsWith("Guest ") && name.length > 15) {
            const parts = name.split(" ");
            const lastPart = parts[parts.length - 1];
            if (lastPart.length >= 8) {
                return `#GST-${lastPart.slice(-8)}`;
            }
        }
        return name;
    };

    return (
        <div className="max-w-7xl mx-auto p-8 min-h-screen text-[#1A1A1A] space-y-6 flex flex-col">
            {/* Top Navigation */}
            <Link
                to={returnPath}
                className="px-4 py-2 text-xs font-bold text-[#42211D] bg-[#F7F7F7] border border-[#DACDCA] hover:bg-gray-100 rounded-lg transition-colors shadow-sm inline-flex items-center gap-1.5 w-fit self-start"
            >
                &larr; {returnLabel}
            </Link>

            {/* Dashboard Header */}
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-[#DACDCA] pb-6">
                <div className="space-y-1.5">
                    <div className="flex flex-wrap items-center gap-3">
                        <h1 className="text-3xl font-black text-[#1A1A1A] tracking-tight">Reservation Dashboard</h1>
                        <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium border uppercase tracking-wider ${
                            reservation.status === 'CONFIRMED' ? 'bg-emerald-50 border-emerald-200 text-emerald-800' :
                            reservation.status === 'PENDING' ? 'bg-amber-50 border-amber-200 text-amber-800' :
                            reservation.status === 'COMPLETED' ? 'bg-blue-50 border-blue-200 text-blue-800' :
                            reservation.status === 'CANCELLED' ? 'bg-red-50 border-red-200 text-red-800' :
                            'bg-gray-50 border-gray-200 text-gray-800'
                        }`}>
                            {reservation.status}
                        </span>
                    </div>
                    <p className="text-sm text-[#7A7A7A]">
                        Reservation ID: <span className="font-mono font-bold text-gray-800 bg-[#F7F7F7] px-2 py-0.5 border border-[#DACDCA] rounded">#RES-{reservation.id.slice(-8)}</span>
                    </p>
                </div>
                <Link
                    to={`/settlements/${reservation.id}`}
                    className="px-5 py-2.5 bg-brand-primary text-white hover:bg-brand-primary-hover text-sm rounded-lg transition-all border border-brand-accent shadow-sm flex items-center justify-center gap-2 active:scale-95 shrink-0"
                >
                    View Bill
                </Link>
            </div>

            {/* Grid Layout Dashboard */}
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                {/* Column 1 & 2: Content Cards */}
                <div className="lg:col-span-2 space-y-6">
                    {/* Stay & Property Details Card */}
                    <div className="bg-white border border-[#DACDCA] rounded-xl shadow-sm p-6 hover:shadow-md transition-all duration-300 space-y-5">
                        <div className="flex items-center gap-2 border-b border-[#DACDCA] pb-3">
                            <Home size={18} className="text-[#42211D]" />
                            <h2 className="text-lg font-bold text-[#1A1A1A] tracking-tight">Stay & Property Info</h2>
                        </div>
                        
                        <div className="space-y-4">
                            <div>
                                <span className="block text-xs font-bold text-[#7A7A7A] uppercase tracking-wider mb-1">Property / Unit Name</span>
                                <p className="text-xl font-black text-[#1A1A1A] tracking-tight">
                                    {reservation.unitName || "Property name not available"}
                                </p>
                                {reservation.city && (
                                    <p className="text-sm font-semibold text-gray-600 mt-1 flex items-center gap-1">
                                        <span>City:</span>
                                        <span className="text-[#1A1A1A]">{reservation.city}</span>
                                    </p>
                                )}
                                <p className="text-xs text-[#7A7A7A] mt-1.5 flex items-center gap-1.5">
                                    <span>Unit ID UUID:</span>
                                    <span className="font-mono font-semibold text-gray-800 bg-[#F7F7F7] px-1.5 py-0.5 border border-[#DACDCA] rounded" title={reservation.unitId}>
                                        ...{reservation.unitId.slice(-8)}
                                    </span>
                                </p>
                            </div>

                            <div className="grid grid-cols-1 sm:grid-cols-2 gap-6 pt-2">
                                <div>
                                    <span className="block text-xs font-bold text-[#7A7A7A] uppercase tracking-wider mb-1 flex items-center gap-1.5">
                                        <Calendar size={14} className="text-[#42211D]" />
                                        Check-in Date
                                    </span>
                                    <p className="text-base font-bold text-[#1A1A1A]">
                                        {reservation.startDate}
                                    </p>
                                </div>
                                <div>
                                    <span className="block text-xs font-bold text-[#7A7A7A] uppercase tracking-wider mb-1 flex items-center gap-1.5">
                                        <Calendar size={14} className="text-[#42211D]" />
                                        Check-out Date
                                    </span>
                                    <p className="text-base font-bold text-[#1A1A1A]">
                                        {reservation.endDate}
                                    </p>
                                </div>
                            </div>

                            <div className="pt-4 border-t border-[#DACDCA]/40 flex items-center justify-between text-xs text-[#7A7A7A]">
                                <span className="flex items-center gap-1.5">
                                    <Clock size={14} />
                                    Booked On
                                </span>
                                <span className="font-semibold text-[#1A1A1A]">
                                    {new Date(reservation.createdAt).toLocaleString()}
                                </span>
                            </div>
                        </div>
                    </div>

                    {/* Guest Information Card */}
                    <div className="bg-white border border-[#DACDCA] rounded-xl shadow-sm p-6 hover:shadow-md transition-all duration-300 space-y-4">
                        <div className="flex items-center gap-2 border-b border-[#DACDCA] pb-3">
                            <User size={18} className="text-[#42211D]" />
                            <h2 className="text-lg font-bold text-[#1A1A1A] tracking-tight">Guest & Account Info</h2>
                        </div>

                        <div className="space-y-4">
                            <div>
                                <span className="block text-xs font-bold text-[#7A7A7A] uppercase tracking-wider mb-1">Guest Identity</span>
                                <p className="text-base font-bold text-[#1A1A1A]">
                                    {formatGuestName(reservation.guestName)}
                                </p>
                                {reservation.guestEmail && (
                                    <p className="text-xs text-gray-500 font-semibold mt-1">
                                        Email: <span className="text-gray-700">{reservation.guestEmail}</span>
                                    </p>
                                )}
                            </div>

                            {reservation.userId && (
                                <div>
                                    <span className="block text-xs font-bold text-[#7A7A7A] uppercase tracking-wider mb-1">Guest User ID</span>
                                    <p className="font-mono text-xs font-bold text-[#1A1A1A] bg-[#F7F7F7] px-2.5 py-1 border border-[#DACDCA] rounded-lg w-fit" title={reservation.userId}>
                                        #USR-{reservation.userId.slice(-8)}
                                    </p>
                                </div>
                            )}
                        </div>
                    </div>

                    {/* Owner Information Card */}
                    {reservation.ownerName && (
                        <div className="bg-white border border-[#DACDCA] rounded-xl shadow-sm p-6 hover:shadow-md transition-all duration-300 space-y-4">
                            <div className="flex items-center gap-2 border-b border-[#DACDCA] pb-3">
                                <User size={18} className="text-[#42211D]" />
                                <h2 className="text-lg font-bold text-[#1A1A1A] tracking-tight">Owner Info</h2>
                            </div>

                            <div className="space-y-4">
                                <div>
                                    <span className="block text-xs font-bold text-[#7A7A7A] uppercase tracking-wider mb-1">Property Owner</span>
                                    <p className="text-base font-bold text-[#1A1A1A]">
                                        {reservation.ownerName}
                                    </p>
                                    {reservation.ownerEmail && (
                                        <p className="text-xs text-gray-500 font-semibold mt-1">
                                            Email: <span className="text-gray-700">{reservation.ownerEmail}</span>
                                        </p>
                                    )}
                                </div>
                            </div>
                        </div>
                    )}
                </div>

                {/* Column 3: Financial Summary Card */}
                <div className="lg:col-span-1">
                    <div className="bg-white border border-[#DACDCA] rounded-xl shadow-sm p-6 hover:shadow-md transition-all duration-300 space-y-4 h-full flex flex-col justify-between">
                        <div className="space-y-4 w-full">
                            <div className="flex items-center gap-2 border-b border-[#DACDCA] pb-3">
                                <CreditCard size={18} className="text-[#42211D]" />
                                <h2 className="text-lg font-bold text-[#1A1A1A] tracking-tight">Financial Summary</h2>
                            </div>

                            <div className="space-y-2.5">
                                {reservation.pricePerNightSnapshot != null && (
                                    <div className="flex justify-between items-center text-sm py-2 border-b border-[#DACDCA]/40">
                                        <span className="text-[#7A7A7A] font-medium">Price per Night Snapshot</span>
                                        <span className="font-bold text-[#1A1A1A]">
                                            {reservation.pricePerNightSnapshot.toFixed(2)} PLN
                                        </span>
                                    </div>
                                )}

                                {reservation.totalPrice != null && (
                                    <div className="flex justify-between items-center text-sm py-2 border-b border-[#DACDCA]/40">
                                        <span className="text-[#7A7A7A] font-medium">Base Total Price</span>
                                        <span className="font-bold text-[#1A1A1A]">
                                            {reservation.totalPrice.toFixed(2)} PLN
                                        </span>
                                    </div>
                                )}

                                {reservation.currencyInfo && displayCurrency !== 'PLN' && (
                                    <div className="flex justify-between items-center text-xs py-2 text-[#7A7A7A] italic border-b border-[#DACDCA]/20">
                                        <span>Exchange Rate Snapshot</span>
                                        <span>
                                            {reservation.currencyInfo.exchangeRate.toFixed(4)} {displayCurrency}/PLN
                                        </span>
                                    </div>
                                )}
                            </div>
                        </div>

                        {reservation.convertedTotalPrice != null && (
                            <div className="mt-6 pt-4 border-t border-[#DACDCA]/60 flex flex-col gap-1 w-full">
                                <span className="text-xs font-bold text-[#7A7A7A] uppercase tracking-wider">Total Converted Price</span>
                                <span className="text-3xl font-black text-[#42211D] tracking-tight whitespace-nowrap">
                                    {reservation.convertedTotalPrice.toFixed(2)} <span className="text-base font-bold text-[#7A7A7A]">{displayCurrency}</span>
                                </span>
                            </div>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
}