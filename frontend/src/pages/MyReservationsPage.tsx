import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getMyReservations, getReservationDetails } from '../api/reservationApi';
import { getSettlementDetails } from '../api/settlementApi';
import type {GuestReservation} from '../types/reservation';
import { Home, Calendar, CreditCard, Info, Receipt, Droplet } from 'lucide-react';

export default function MyReservationsPage() {
    const [reservations, setReservations] = useState<GuestReservation[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [settlementIds, setSettlementIds] = useState<Record<string, string>>({});
    const [unitNames, setUnitNames] = useState<Record<string, string>>({});


    useEffect(() => {
        getMyReservations()
            .then(data => {
                setReservations(data);
                setLoading(false);
                data.forEach(async (res: GuestReservation) => {
                    try {
                        const settlement = await getSettlementDetails(res.id);
                        setSettlementIds(prev => ({ ...prev, [res.id]: settlement.id }));
                    } catch (err) {
                        console.error("Failed to load settlement details", err);
                    }
                    try {
                        const details = await getReservationDetails(res.id);
                        setUnitNames(prev => ({ ...prev, [res.id]: details.unitName }));
                    } catch (err) {
                        console.error("Failed to load reservation details", err);
                    }
                });
            })
            .catch(err => {
                setError(err.message);
                setLoading(false);
            });
    }, []);

    if (loading) return <div className="p-8 text-center text-gray-500 font-medium">Loading your reservations...</div>;
    if (error) return <div className="p-8 text-center text-red-600 font-semibold">Error: {error}</div>;

    return (
        <div className="p-8 max-w-7xl mx-auto min-h-screen text-[#1A1A1A] space-y-6">
            <div>
                <h1 className="text-3xl font-bold text-[#1A1A1A] tracking-tight">My Reservations</h1>
                <p className="text-sm text-[#7A7A7A] mt-1">Manage your booked stays, view settlement billing, and upload meter readings.</p>
            </div>

            {reservations.length === 0 ? (
                <div className="bg-white border border-[#DACDCA] rounded-xl shadow-sm p-8 text-center space-y-4">
                    <p className="text-[#7A7A7A] font-medium">You don't have any reservations yet.</p>
                    <Link to="/catalog" className="inline-block px-6 py-2.5 bg-[#42211D] text-white font-bold hover:bg-[#2a1412] text-sm rounded-lg transition-colors border border-[#DACDCA] shadow-sm">
                        Browse Catalog
                    </Link>
                </div>
            ) : (
                <div className="grid gap-6">
                    {reservations.map(res => {
                        const isBillEnabled = !!settlementIds[res.id];
                        const isWaterMeterEnabled = !!settlementIds[res.id] && (res.status === "CONFIRMED" || res.status === "COMPLETED");

                        return (
                            <div key={res.id} className="bg-white border border-[#DACDCA] rounded-xl shadow-sm p-6 hover:shadow-md transition-all duration-300">
                                <div className="grid grid-cols-1 lg:grid-cols-[240px_1fr_auto] gap-6 items-center">
                                    {/* Column 1: Unit Name & Reservation ID */}
                                    <div className="space-y-2.5">
                                        <div className="flex items-center gap-3">
                                            <div className="p-2.5 bg-[#F7F7F7] border border-[#DACDCA] rounded-xl text-[#42211D]">
                                                <Home size={20} />
                                            </div>
                                            <div>
                                                <h2 className="text-lg font-black text-[#1A1A1A] tracking-tight">
                                                    {unitNames[res.id] || `Unit: ...${res.unitId.slice(-8)}`}
                                                </h2>
                                                <div className="text-sm text-gray-500 mt-1" title={res.id}>
                                                    Reservation ID: <span className="text-gray-900 font-semibold font-mono">#RES-{res.id.slice(-8)}</span>
                                                </div>
                                            </div>
                                        </div>
                                    </div>

                                    {/* Column 2: Period (Dates) and Total Price */}
                                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-6 lg:px-4">
                                        {/* Period/Dates */}
                                        <div className="space-y-1">
                                            <span className="block text-xs font-bold text-[#7A7A7A] uppercase tracking-wider">Stay Dates</span>
                                            <div className="flex items-center gap-2 text-sm text-[#1A1A1A]">
                                                <Calendar size={16} className="text-[#42211D] shrink-0" />
                                                <span className="font-semibold text-gray-800 whitespace-nowrap">{res.startDate} &rarr; {res.endDate}</span>
                                            </div>
                                        </div>

                                        {/* Total Price */}
                                        {res.convertedTotalPrice && res.currencyInfo && (
                                            <div className="space-y-1">
                                                <span className="block text-xs font-bold text-[#7A7A7A] uppercase tracking-wider">Total Price</span>
                                                <div className="flex items-center gap-2 text-sm text-[#42211D]">
                                                    <CreditCard size={16} className="shrink-0" />
                                                    <span className="text-lg font-black tracking-tight whitespace-nowrap">
                                                        {res.convertedTotalPrice.toFixed(2)} {res.currencyInfo.displayCurrency}
                                                    </span>
                                                </div>
                                            </div>
                                        )}
                                    </div>

                                    {/* Column 3: Status badge and Action buttons */}
                                    <div className="flex flex-col sm:flex-row lg:flex-col items-start sm:items-center lg:items-end justify-between lg:justify-center gap-4 w-full lg:w-auto border-t lg:border-t-0 pt-4 lg:pt-0 border-[#DACDCA]">
                                        {/* Status Badge */}
                                        <span className={`inline-flex items-center px-3 py-1 rounded-full text-xs font-bold border tracking-wider uppercase ${
                                            res.status === 'CONFIRMED' ? 'bg-emerald-50 border-emerald-200 text-emerald-800' :
                                            res.status === 'PENDING' ? 'bg-amber-50 border-amber-200 text-amber-800' :
                                            res.status === 'COMPLETED' ? 'bg-blue-50 border-blue-200 text-blue-800' :
                                            'bg-gray-50 border-gray-200 text-gray-800'
                                        }`}>
                                            {res.status}
                                        </span>

                                        {/* Action Buttons */}
                                        <div className="flex flex-wrap items-center gap-2.5 w-full sm:w-auto justify-start sm:justify-end">
                                            {/* 1. View Details (always active, solid background) */}
                                            <Link
                                                to={`/reservations/${res.id}`}
                                                className="px-4 py-2 text-xs font-bold text-white bg-[#42211D] hover:bg-[#321815] rounded-lg transition-all shadow-sm active:scale-95 inline-flex items-center justify-center gap-1.5 shrink-0"
                                            >
                                                <Info size={14} />
                                                View Details
                                            </Link>

                                            {/* 2. View Bill (always rendered, conditionally disabled with Tailwind classes) */}
                                            {isBillEnabled ? (
                                                <Link
                                                    to={`/settlements/${res.id}`}
                                                    className="px-4 py-2 text-xs font-bold bg-white border border-gray-300 text-gray-700 hover:bg-gray-50 rounded-lg transition-all shadow-sm active:scale-95 inline-flex items-center justify-center gap-1.5 shrink-0"
                                                >
                                                    <Receipt size={14} />
                                                    View Bill
                                                </Link>
                                            ) : (
                                                <button
                                                    disabled
                                                    className="px-4 py-2 text-xs font-bold rounded-lg border transition-all shadow-sm inline-flex items-center justify-center gap-1.5 shrink-0 text-[#42211D] bg-white border-[#DACDCA] disabled:opacity-50 disabled:cursor-not-allowed disabled:bg-gray-100 disabled:text-gray-400 disabled:border-gray-200"
                                                >
                                                    <Receipt size={14} />
                                                    View Bill
                                                </button>
                                            )}

                                            {/* 3. Water Meter (always rendered, conditionally disabled with Tailwind classes) */}
                                            {isWaterMeterEnabled ? (
                                                <Link
                                                    to={`/settlements/${settlementIds[res.id]}/meter-readings?unitId=${res.unitId}`}
                                                    className="px-4 py-2 text-xs font-bold bg-white border border-gray-300 text-gray-700 hover:bg-gray-50 rounded-lg transition-all shadow-sm active:scale-95 inline-flex items-center justify-center gap-1.5 shrink-0"
                                                >
                                                    <Droplet size={14} />
                                                    Water Meter
                                                </Link>
                                            ) : (
                                                <button
                                                    disabled
                                                    className="px-4 py-2 text-xs font-bold rounded-lg border transition-all shadow-sm inline-flex items-center justify-center gap-1.5 shrink-0 text-[#42211D] bg-white border-[#DACDCA] disabled:opacity-50 disabled:cursor-not-allowed disabled:bg-gray-100 disabled:text-gray-400 disabled:border-gray-200"
                                                >
                                                    <Droplet size={14} />
                                                    Water Meter
                                                </button>
                                            )}
                                        </div>
                                    </div>
                                </div>
                            </div>
                        );
                    })}
                </div>
            )}
        </div>
    );
}
