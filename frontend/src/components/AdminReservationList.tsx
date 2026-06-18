import { useEffect, useState, useCallback } from 'react';
import { Link } from "react-router-dom";
import { GATEWAY_BASE_URL } from '../api/apiConfig';
import { getSettlementDetails } from '../api/settlementApi';
import { ChevronDown } from 'lucide-react';
import {useTranslation} from "react-i18next"


interface ReservationOverview {
    id: string;
    guestName: string;
    unitName: string;
    startDate: string;
    endDate: string;
    status: string;
}

export default function AdminReservationList() {
    const [reservations, setReservations] = useState<ReservationOverview[]>([]);
    const [statusFilter, setStatusFilter] = useState<string>("");
    const [message, setMessage] = useState<{ text: string, type: 'success' | 'error' } | null>(null);
    const API_BASE_URL = GATEWAY_BASE_URL;
    const [settlementIds, setSettlementIds] = useState<Record<string, string>>({});
    const [unitIds, setUnitIds] = useState<Record<string, string>>({});
    const {t} = useTranslation();


    const fetchReservations = useCallback(() => {
        const url = statusFilter
            ? `${API_BASE_URL}/api/v1/admin/reservations?status=${statusFilter}`
            : `${API_BASE_URL}/api/v1/admin/reservations`;

        const token = localStorage.getItem("token");

        fetch(url, {
            method: "GET",
            headers: {
                "Authorization": `Bearer ${token}`,
                "Content-Type": "application/json"
            }
        })
            .then((res) => {
                if (res.status === 401) console.error("Token is incorrect!");
                if (!res.ok) throw new Error("Get data error");
                return res.json();
            })
            .then((data) => {
                setReservations(data);
                data.forEach(async (res: ReservationOverview) => {
                    try {
                        const settlement = await getSettlementDetails(res.id);
                        setSettlementIds(prev => ({ ...prev, [res.id]: settlement.id }));
                    } catch (err) {
                        console.error("Failed to load settlement details", err);
                    }
                    try {
                        const t = localStorage.getItem("token");
                        const resDetails = await fetch(
                            `${API_BASE_URL}/api/v1/reservations/${res.id}`,
                            { headers: { Authorization: `Bearer ${t}` } }
                        );
                        const resData = await resDetails.json();
                        setUnitIds(prev => ({ ...prev, [res.id]: resData.unitId }));
                    } catch (err) {
                        console.error("Failed to load reservation details", err);
                    }
                });
            })
            .catch((err) => console.error(err));
    }, [statusFilter, API_BASE_URL]);

    useEffect(() => {
        fetchReservations();
    }, [fetchReservations]);

    const handleStatusChange = (id: string, newStatus: string) => {
        const token = localStorage.getItem("token");
        fetch(`${API_BASE_URL}/api/v1/admin/reservations/${id}/status`, {
            method: "PATCH",
            headers: {
                "Authorization": `Bearer ${token}`,
                "Content-Type": "application/json"
            },
            body: JSON.stringify({ newStatus })
        })
            .then(async (res) => {
                if (res.ok) {
                    setMessage({ text: t('adminReservations.statusUpdated'), type: 'success' });
                    setReservations(prev => prev.map(r => r.id === id ? { ...r, status: newStatus } : r));
                } else {
                    const errorData = await res.json().catch(() => ({ message:  t('adminReservations.errorOccurred') }));
                    let errorMsg = errorData.message ||  t('adminReservations.errorOccurred');

                    if (res.status === 400) errorMsg = t('adminReservations.statusNotAllowed');
                    if (res.status === 401) errorMsg = t('adminReservations.sessionExpired');
                    if (res.status === 403) errorMsg = t('adminReservations.notAllowed');
                    if (res.status === 404) errorMsg = t('adminReservations.notFound')

                    setMessage({ text: errorMsg, type: 'error' });
                }
            })
            .catch((err) => {
                console.error(err);
                setMessage({ text: t('adminReservations.networkError'), type: 'error' });
            });
    };

    const filteredReservations = reservations;

    const formatGuestName = (name: string) => {
        if (!name) return "";
        const uuidRegex = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
        if (uuidRegex.test(name)) {
            return `#GST-${name.slice(-8)}`;
        }
        if (name.startsWith("Guest ")) {
            const val = name.substring(6).trim();
            return `#GST-${val.slice(-8)}`;
        }
        return name;
    };

    return (
        <div className="p-8 max-w-7xl mx-auto min-h-screen text-[#1A1A1A] space-y-6">
            <div className="border-b border-[#DACDCA] pb-6 mb-6 space-y-2">
                <div className="flex flex-col sm:flex-row justify-between sm:items-center gap-4">
                    <h1 className="text-3xl font-bold text-[#1A1A1A] tracking-tight">{t('adminReservations.title')}</h1>
                    <div className="flex bg-[#F7F7F7] p-1 rounded-lg border border-[#DACDCA] shadow-sm self-start sm:self-center">
                        <span className="px-4 py-2 text-sm font-bold rounded-md bg-[#FFFFFF] text-[#1A1A1A] shadow border border-[#DACDCA] cursor-default">
                            {t('adminReservations.listView')}
                        </span>
                        <Link
                            to="/admin/occupancy"
                            className="px-4 py-2 text-sm font-medium rounded-md text-[#7A7A7A] hover:bg-[#FFFFFF] hover:text-[#1A1A1A] hover:shadow-sm transition-all"
                        >
                            {t('adminReservations.calendarView')}
                        </Link>
                    </div>
                </div>
                <p className="text-sm text-[#7A7A7A]">{t('adminReservations.subtitle')}</p>
            </div>

            {message && (
                <div className={`flex items-center justify-between gap-3 p-4 border-l-4 rounded-r-xl animate-fade-in shadow-sm mb-6 ${
                    message.type === 'success' ? 'bg-emerald-50 border-emerald-500 text-emerald-700' : 'bg-red-50 border-red-500 text-red-700'
                }`}>
                    <div className="flex items-center gap-3">
                        <svg className="w-5 h-5 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
                            <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clipRule="evenodd"/>
                        </svg>
                        <span className="text-sm font-semibold">{message.text}</span>
                    </div>
                    <button onClick={() => setMessage(null)} className="font-bold text-lg hover:opacity-75 transition-opacity px-2">&times;</button>
                </div>
            )}

            {/* Compact, Inline Toolbar Section */}
            <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 bg-[#F7F7F7] border border-gray-100 rounded-xl p-4">
                <div className="text-sm font-medium text-[#7A7A7A]">
                    {t('adminReservations.showing')} <span className="font-bold text-[#1A1A1A]">{filteredReservations.length}</span> {t('adminReservations.reservation', {count: filteredReservations.length})}
                </div>
                <div className="flex items-center gap-2 w-full sm:w-auto">
                    <span className="text-sm font-medium text-gray-500 mr-2 shrink-0">{t('adminReservations.filterByStatus')}</span>
                    <div className="relative w-full sm:w-44">
                        <select
                            className="appearance-none block w-full bg-white border border-gray-300 rounded-md py-2 pl-3 pr-10 text-sm text-[#1A1A1A] font-semibold focus:outline-none focus:ring-2 focus:ring-brand-primary focus:border-brand-primary transition-all shadow-sm cursor-pointer"
                            value={statusFilter}
                            onChange={(e) => setStatusFilter(e.target.value)}
                        >
                            <option value="">{t('adminReservations.allStatuses')}</option>
                            <option value="PENDING">{t('common.pending')}</option>
                            <option value="CONFIRMED">{t('common.confirmed')}</option>
                            <option value="CANCELLED">{t('common.cancelled')}</option>
                            <option value="COMPLETED">{t('common.completed')}</option>
                        </select>
                        <div className="pointer-events-none absolute inset-y-0 right-0 flex items-center pr-3">
                            <ChevronDown className="h-4 w-4 text-[#7A7A7A]" />
                        </div>
                    </div>
                    {statusFilter && (
                        <button
                            onClick={() => setStatusFilter("")}
                            className="text-sm font-medium text-gray-500 hover:text-brand-main transition-colors shrink-0 cursor-pointer"
                        >
                            {t('common.clear')}
                        </button>
                    )}
                </div>
            </div>

            <div className="overflow-x-auto bg-white border border-[#DACDCA] rounded-xl shadow-sm">
                <table className="min-w-full table-auto">
                    <thead className="bg-[#F7F7F7] border-b border-[#DACDCA]">
                        <tr>
                            <th className="px-6 py-4 text-sm font-semibold text-[#7A7A7A] text-left">{t('adminReservations.guest')}</th>
                            <th className="px-6 py-4 text-sm font-semibold text-[#7A7A7A] text-left">{t('adminReservations.unitProperty')}</th>
                            <th className="px-6 py-4 text-sm font-semibold text-[#7A7A7A] text-left">{t('adminReservations.stayDates')}</th>
                            <th className="px-6 py-4 text-sm font-semibold text-[#7A7A7A] text-left">{t('common.status')}</th>
                            <th className="px-6 py-4 text-sm font-semibold text-[#7A7A7A] text-center">{t('common.actions')}</th>
                        </tr>
                    </thead>
                    <tbody className="divide-y divide-[#DACDCA]/60 bg-white">
                        {filteredReservations.map((res) => {
                            const isMeterReadingsEnabled = !!settlementIds[res.id] && !!unitIds[res.id] && (res.status === "CONFIRMED" || res.status === "COMPLETED");
                            const isConfirmEnabled = res.status === "PENDING";
                            const isCompleteEnabled = res.status === "CONFIRMED";
                            const isCancelEnabled = res.status === "PENDING" || res.status === "CONFIRMED";

                            return (
                                <tr key={res.id} className="hover:bg-[#F7F7F7] transition-colors">
                                    <td className="px-6 py-4 text-sm font-semibold text-[#1A1A1A]">
                                        {formatGuestName(res.guestName)}
                                    </td>
                                    <td className="px-6 py-4 text-sm text-[#1A1A1A]">
                                        {res.unitName}
                                    </td>
                                    <td className="px-6 py-4 text-sm text-[#1A1A1A] font-medium">
                                        <span className="whitespace-nowrap">
                                            {res.startDate} &rarr; {res.endDate}
                                        </span>
                                    </td>
                                    <td className="px-6 py-4 text-sm">
                                        <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium border uppercase tracking-wider ${
                                            res.status === 'CONFIRMED' ? 'bg-emerald-50 border-emerald-200 text-emerald-800' :
                                            res.status === 'PENDING' ? 'bg-amber-50 border-amber-200 text-amber-800' :
                                            res.status === 'COMPLETED' ? 'bg-blue-50 border-blue-200 text-blue-800' :
                                            res.status === 'CANCELLED' ? 'bg-red-50 border-red-200 text-red-800' :
                                            'bg-gray-50 border-gray-200 text-gray-800'
                                        }`}>
                                            {t(`statuses.${res.status}`)}
                                        </span>
                                    </td>
                                    <td className="px-6 py-4 text-sm text-center">
                                        <div className="flex flex-wrap justify-center items-center gap-2">
                                            <Link
                                                to={`/reservations/${res.id}`}
                                                className="px-3 py-1.5 text-xs font-semibold text-white bg-[#42211D] hover:bg-[#321815] rounded-lg transition-all shadow-sm active:scale-95 inline-flex items-center justify-center gap-1 shrink-0 cursor-pointer"
                                            >
                                                {t('common.viewDetails')}
                                            </Link>

                                            {isMeterReadingsEnabled ? (
                                                <Link
                                                    to={`/admin/settlements/${settlementIds[res.id]}/meter-readings?unitId=${unitIds[res.id]}`}
                                                    className="px-3 py-1.5 text-xs font-semibold text-gray-700 bg-white border border-gray-300 hover:bg-gray-50 rounded-lg transition-all shadow-sm active:scale-95 inline-flex items-center justify-center gap-1 shrink-0 cursor-pointer"
                                                >
                                                    {t('common.meterReadings')}
                                                </Link>
                                            ) : (
                                                <button
                                                    disabled
                                                    className="px-3 py-1.5 text-xs font-semibold rounded-lg border transition-all inline-flex items-center justify-center gap-1 shrink-0 text-gray-700 bg-white border-gray-300 disabled:opacity-50 disabled:cursor-not-allowed disabled:bg-gray-100 disabled:text-gray-400 disabled:border-gray-200"
                                                >
                                                    {t('common.meterReadings')}
                                                </button>
                                            )}

                                            <button
                                                disabled={!isConfirmEnabled}
                                                onClick={() => handleStatusChange(res.id, 'CONFIRMED')}
                                                className="px-3 py-1.5 text-xs font-semibold rounded-lg border transition-all inline-flex items-center justify-center gap-1 shrink-0 text-gray-700 bg-white border-gray-300 hover:bg-gray-50 active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed disabled:bg-gray-100 disabled:text-gray-400 disabled:border-gray-200 cursor-pointer disabled:cursor-not-allowed"
                                            >
                                                {t('common.confirm')}
                                            </button>

                                            <button
                                                disabled={!isCompleteEnabled}
                                                onClick={() => handleStatusChange(res.id, 'COMPLETED')}
                                                className="px-3 py-1.5 text-xs font-semibold rounded-lg border transition-all inline-flex items-center justify-center gap-1 shrink-0 text-gray-700 bg-white border-gray-300 hover:bg-gray-50 active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed disabled:bg-gray-100 disabled:text-gray-400 disabled:border-gray-200 cursor-pointer disabled:cursor-not-allowed"
                                            >
                                                {t('common.complete')}
                                            </button>

                                            <button
                                                disabled={!isCancelEnabled}
                                                onClick={() => handleStatusChange(res.id, 'CANCELLED')}
                                                className="px-3 py-1.5 text-xs font-semibold rounded-lg border border-transparent transition-all inline-flex items-center justify-center gap-1 shrink-0 text-red-600 bg-red-50 hover:bg-red-100 active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed disabled:bg-gray-100 disabled:text-gray-400 disabled:border-gray-200 cursor-pointer disabled:cursor-not-allowed"
                                            >
                                                {t('common.cancel')}
                                            </button>
                                        </div>
                                    </td>
                                </tr>
                            );
                        })}
                        {filteredReservations.length === 0 && (
                            <tr>
                                <td colSpan={5} className="px-6 py-8 text-center text-[#7A7A7A] font-medium bg-white">
                                    {t('adminReservations.noReservations')}
                                </td>
                            </tr>
                        )}
                    </tbody>
                </table>
            </div>
        </div>
    );
}

