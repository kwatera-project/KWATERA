import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getMyReservations, getReservationDetails } from '../api/reservationApi';
import { getSettlementDetails, downloadInvoice } from '../api/settlementApi';
import type {GuestReservation} from '../types/reservation';
import type {SettlementDetails} from '../types/settlement';
import { Home, Calendar, CreditCard, Info, Receipt, Droplet, FileText } from 'lucide-react';
import {useTranslation} from "react-i18next"

export default function MyReservationsPage() {
    const [reservations, setReservations] = useState<GuestReservation[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [settlements, setSettlements] = useState<Record<string, SettlementDetails>>({})
    const [unitNames, setUnitNames] = useState<Record<string, string>>({});

    const handleDownloadInvoice = async (reservationId: string) => {
        try {
            await downloadInvoice(reservationId);
        } catch (err: unknown) {
            alert(err instanceof Error ? err.message : "Failed to download invoice");
        }
    };
    const {t} = useTranslation();

    useEffect(() => {
        getMyReservations()
            .then(data => {
                setReservations(data);
                setLoading(false);
                data.forEach(async (res: GuestReservation) => {
                    try {
                        const settlement = await getSettlementDetails(res.id);
                        setSettlements(prev => ({ ...prev, [res.id]: settlement }));
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

    if (loading) return <div className="p-8 text-center text-gray-500 font-medium">{t('myReservations.loading')}</div>;
    if (error) return <div className="p-8 text-center text-red-600 font-semibold">{t('myReservations.error', {error})}</div>;

    return (
        <div className="p-4 sm:p-8 max-w-7xl mx-auto min-h-screen text-[#1A1A1A] space-y-6">
            <div>
                <h1 className="text-3xl font-bold text-[#1A1A1A] tracking-tight">{t('myReservations.title')}</h1>
                <p className="text-sm text-[#7A7A7A] mt-1">{t('myReservations.subtitle')}</p>
            </div>

            {reservations.length === 0 ? (
                <div className="bg-white border border-[#DACDCA] rounded-xl shadow-sm p-5 sm:p-8 text-center space-y-4">
                    <p className="text-[#7A7A7A] font-medium">{t('myReservations.noReservations')}</p>
                    <Link to="/properties" className="inline-block px-6 py-2.5 bg-[#42211D] text-white font-bold hover:bg-[#2a1412] text-sm rounded-lg transition-colors border border-[#DACDCA] shadow-sm">
                        {t('myReservations.browseCatalog')}
                    </Link>
                </div>
            ) : (
                <div className="grid gap-6">
                    {reservations.map(res => {
                        const isBillEnabled = !!settlements[res.id]?.id;
                        const isWaterMeterEnabled = !!settlements[res.id]?.id && (res.status === "CONFIRMED" || res.status === "COMPLETED");
                        const showInvoice = !!settlements[res.id]?.invoiceRequested && (res.status === "COMPLETED" || settlements[res.id]?.status === "PAID");

                        return (
                            <div key={res.id} className="bg-white border border-[#DACDCA] rounded-xl shadow-sm p-6 hover:shadow-md transition-all duration-300">
                                <div className="grid grid-cols-1 lg:grid-cols-[240px_1fr_auto] gap-6 items-center">
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
                                                    {t('myReservations.reservationId')} <span className="text-gray-900 font-semibold font-mono">#RES-{res.id.slice(-8)}</span>
                                                </div>
                                            </div>
                                        </div>
                                    </div>

                                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-6 lg:px-4">
                                        <div className="space-y-1">
                                            <span className="block text-xs font-bold text-[#7A7A7A] uppercase tracking-wider">{t('adminReservations.stayDates')}</span>
                                            <div className="flex items-center gap-2 text-sm text-[#1A1A1A]">
                                                <Calendar size={16} className="text-[#42211D] shrink-0" />
                                                <span className="font-semibold text-gray-800 whitespace-nowrap">{res.startDate} &rarr; {res.endDate}</span>
                                            </div>
                                        </div>

                                        {res.convertedTotalPrice && res.currencyInfo && (
                                            <div className="space-y-1">
                                                <span className="block text-xs font-bold text-[#7A7A7A] uppercase tracking-wider">{t('checkout.totalPrice')}</span>
                                                <div className="flex items-center gap-2 text-sm text-[#42211D]">
                                                    <CreditCard size={16} className="shrink-0" />
                                                    <span className="text-lg font-black tracking-tight whitespace-nowrap">
                                                        {res.convertedTotalPrice.toFixed(2)} {res.currencyInfo.displayCurrency}
                                                    </span>
                                                </div>
                                            </div>
                                        )}
                                    </div>

                                    <div className="flex flex-col sm:flex-row lg:flex-col items-start sm:items-center lg:items-end justify-between lg:justify-center gap-4 w-full lg:w-auto border-t lg:border-t-0 pt-4 lg:pt-0 border-[#DACDCA]">
                                        <span className={`inline-flex items-center px-3 py-1 rounded-full text-xs font-bold border tracking-wider uppercase ${
                                            res.status === 'CONFIRMED' ? 'bg-emerald-50 border-emerald-200 text-emerald-800' :
                                            res.status === 'PENDING' ? 'bg-amber-50 border-amber-200 text-amber-800' :
                                            res.status === 'COMPLETED' ? 'bg-blue-50 border-blue-200 text-blue-800' :
                                            'bg-gray-50 border-gray-200 text-gray-800'
                                        }`}>
                                            {t(`statuses.${res.status}`)}
                                        </span>

                                        <div className="flex flex-wrap items-center gap-2.5 w-full sm:w-auto justify-start sm:justify-end">
                                            <Link
                                                to={`/reservations/${res.id}`}
                                                className="px-4 py-2 text-xs font-bold text-white bg-[#42211D] hover:bg-[#321815] rounded-lg transition-all shadow-sm active:scale-95 inline-flex items-center justify-center gap-1.5 shrink-0"
                                            >
                                                <Info size={14} />
                                                {t('common.viewDetails')}
                                            </Link>

                                            {isBillEnabled ? (
                                                <Link
                                                    to={`/settlements/${res.id}`}
                                                    className="px-4 py-2 text-xs font-bold bg-white border border-gray-300 text-gray-700 hover:bg-gray-50 rounded-lg transition-all shadow-sm active:scale-95 inline-flex items-center justify-center gap-1.5 shrink-0"
                                                >
                                                    <Receipt size={14} />
                                                    {t('myReservations.viewBill')}
                                                </Link>
                                            ) : (
                                                <button
                                                    disabled
                                                    className="px-4 py-2 text-xs font-bold rounded-lg border transition-all shadow-sm inline-flex items-center justify-center gap-1.5 shrink-0 text-[#42211D] bg-white border-[#DACDCA] disabled:opacity-50 disabled:cursor-not-allowed disabled:bg-gray-100 disabled:text-gray-400 disabled:border-gray-200"
                                                >
                                                    <Receipt size={14} />
                                                    {t('myReservations.viewBill')}
                                                </button>
                                            )}

                                            {isWaterMeterEnabled ? (
                                                <Link
                                                    to={`/settlements/${settlements[res.id]?.id}/meter-readings?unitId=${res.unitId}`}
                                                    className="px-4 py-2 text-xs font-bold bg-white border border-gray-300 text-gray-700 hover:bg-gray-50 rounded-lg transition-all shadow-sm active:scale-95 inline-flex items-center justify-center gap-1.5 shrink-0"
                                                >
                                                    <Droplet size={14} />
                                                    {t('myReservations.waterMeter')}
                                                </Link>
                                            ) : (
                                                <button
                                                    disabled
                                                    className="px-4 py-2 text-xs font-bold rounded-lg border transition-all shadow-sm inline-flex items-center justify-center gap-1.5 shrink-0 text-[#42211D] bg-white border-[#DACDCA] disabled:opacity-50 disabled:cursor-not-allowed disabled:bg-gray-100 disabled:text-gray-400 disabled:border-gray-200"
                                                >
                                                    <Droplet size={14} />
                                                    {t('myReservations.waterMeter')}
                                                </button>
                                            )}

                                            {showInvoice && (
                                                <button
                                                    onClick={() => handleDownloadInvoice(res.id)}
                                                    className="px-4 py-2 text-xs font-bold bg-white border border-gray-300 text-gray-700 hover:bg-gray-50 rounded-lg transition-all shadow-sm active:scale-95 inline-flex items-center justify-center gap-1.5 shrink-0"
                                                >
                                                    <FileText size={14} />
                                                    Download Invoice
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