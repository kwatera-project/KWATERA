import {useEffect, useState, useRef, useCallback} from "react";
import {Link} from "react-router-dom";
import {getOccupancy, updateAdminReservationStatus} from "../api/adminApi";
import {
    format,
    addDays,
    startOfToday,
    isAfter,
    isBefore,
    startOfWeek,
    endOfWeek,
    startOfMonth,
    endOfMonth,
    addMonths
} from "date-fns";
import SharedDatePicker from "../components/SharedDatePicker";
import ManualReservationModal from "../components/ManualReservationModal";
import BlockDatesModal from "../components/BlockDatesModal";
import {useTranslation} from "react-i18next"
import {getDateFnsLocale, getLocaleCode} from "../utils/locale";

interface Occupancy {
    reservationId: string;
    unitId: string;
    unitName?: string;
    startDate: string;
    endDate: string;
    status: string;
    guestName?: string;
    totalPrice?: number;
    guestEmail?: string;
}

export default function OccupancyCalendarPage() {
    const [occupancies, setOccupancies] = useState<Occupancy[]>([]);
    const [dateRange, setDateRange] = useState<[Date | null, Date | null]>([
        startOfMonth(startOfToday()),
        endOfMonth(startOfToday())
    ]);
    const {t, i18n} = useTranslation();
    const [startDate] = dateRange;
    const [selectedOcc, setSelectedOcc] = useState<Occupancy | null>(null);
    const [quickAction, setQuickAction] = useState<{ date: Date } | null>(null);
    const [isManualReservationOpen, setIsManualReservationOpen] = useState(false);
    const [isBlockDatesOpen, setIsBlockDatesOpen] = useState(false);
    const [selectedQuickActionDate, setSelectedQuickActionDate] = useState<Date | null>(null);

    const calendarRef = useRef<HTMLDivElement>(null);
    const todayStr = format(startOfToday(), 'yyyy-MM-dd');

    const anchorDate = startDate || startOfToday();
    const firstDayOfMonth = startOfMonth(anchorDate);
    const lastDayOfMonth = endOfMonth(anchorDate);

    const firstCalendarDay = startOfWeek(firstDayOfMonth, {weekStartsOn: 1});
    const lastCalendarDay = endOfWeek(lastDayOfMonth, {weekStartsOn: 1});

    const [message, setMessage] = useState<{ text: string, type: 'success' | 'error' } | null>(null);

    const isConfirmEnabled = selectedOcc?.status === "PENDING";
    const isCompleteEnabled = selectedOcc?.status === "CONFIRMED";
    const isCancelEnabled = selectedOcc?.status === "PENDING" || selectedOcc?.status === "CONFIRMED";

    const dates: Date[] = [];
    let current = firstCalendarDay;
    while (!isAfter(current, lastCalendarDay)) {
        dates.push(current);
        current = addDays(current, 1);
    }

    const weeks: Date[][] = [];
    for (let i = 0; i < dates.length; i += 7) {
        weeks.push(dates.slice(i, i + 7));
    }

    const fetchOccupancyData = useCallback(() => {
        const anchor = startDate || startOfToday();
        const firstDay = startOfMonth(anchor);
        const lastDay = endOfMonth(anchor);
        const firstCal = startOfWeek(firstDay, {weekStartsOn: 1});
        const lastCal = endOfWeek(lastDay, {weekStartsOn: 1});

        const startStr = format(firstCal, 'yyyy-MM-dd');
        const endStr = format(lastCal, 'yyyy-MM-dd');

        getOccupancy(startStr, endStr)
            .then(setOccupancies)
            .catch(console.error);
    }, [startDate]);

    useEffect(() => {
        fetchOccupancyData();
    }, [fetchOccupancyData]);

    const unitMap = new Map<string, string>();
    occupancies.forEach(o => {
        if (!unitMap.has(o.unitId)) {
            unitMap.set(o.unitId, o.unitName || o.unitId);
        }
    });

    const uniqueUnits = Array.from(unitMap.entries()).map(([id, name]) => ({
        id,
        name
    }));

    const handlePrevMonth = () => {
        const prev = addMonths(anchorDate, -1);
        setDateRange([startOfMonth(prev), endOfMonth(prev)]);
    };

    const handleNextMonth = () => {
        const next = addMonths(anchorDate, 1);
        setDateRange([startOfMonth(next), endOfMonth(next)]);
    };

    const getLanesForChunk = (chunkDates: Date[]) => {
        if (chunkDates.length === 0) return [];
        const chunkStartStr = format(chunkDates[0], 'yyyy-MM-dd');
        const chunkEndStr = format(chunkDates[chunkDates.length - 1], 'yyyy-MM-dd');

        const overlapping = occupancies.filter(occ =>
            occ.status !== 'CANCELLED' && occ.startDate <= chunkEndStr && occ.endDate >= chunkStartStr
        );

        overlapping.sort((a, b) => {
            if (a.startDate === b.startDate) return a.unitId.localeCompare(b.unitId);
            return a.startDate.localeCompare(b.startDate);
        });

        const lanes: Occupancy[][] = [];
        for (const occ of overlapping) {
            let placed = false;
            for (const lane of lanes) {
                const lastOcc = lane[lane.length - 1];
                if (lastOcc.endDate < occ.startDate) {
                    lane.push(occ);
                    placed = true;
                    break;
                }
            }
            if (!placed) lanes.push([occ]);
        }
        return lanes;
    };

    const renderLaneCellsForWeek = (lane: Occupancy[], weekDates: Date[]) => {
        const elements = [];
        const weekStartStr = format(weekDates[0], 'yyyy-MM-dd');
        const weekEndStr = format(weekDates[6], 'yyyy-MM-dd');

        let i = 0;
        while (i < weekDates.length) {
            const d = weekDates[i];
            const dateStr = format(d, 'yyyy-MM-dd');
            const occ = lane.find(o => o.startDate <= dateStr && o.endDate >= dateStr);

            if (occ) {
                const visibleEndIndex = weekDates.findIndex(date => format(date, 'yyyy-MM-dd') === occ.endDate);
                const endIdx = visibleEndIndex !== -1 ? visibleEndIndex : weekDates.length - 1;
                const span = endIdx - i + 1;

                const isStart = occ.startDate >= weekStartStr && occ.startDate === dateStr;
                const isEnd = occ.endDate <= weekEndStr && occ.endDate === format(weekDates[endIdx], 'yyyy-MM-dd');

                const roundedClass = `${isStart ? 'rounded-l-full pl-3' : 'rounded-l pl-1 border-l border-white/20'} ${isEnd ? 'rounded-r-full pr-3' : 'rounded-r pr-1 border-r border-white/20'}`;

                let bgColor = "bg-brand-muted hover:bg-[#5A5A5A]";
                let textColor = "text-white";
                if (occ.status === 'CONFIRMED') {
                    bgColor = "bg-emerald-600 hover:bg-emerald-700";
                } else if (occ.status === 'PENDING') {
                    bgColor = "bg-amber-500 hover:bg-amber-600";
                    textColor = "text-brand-main";
                }

                const unitName = unitMap.get(occ.unitId) || occ.unitId;
                const displayText = `${unitName} - ${occ.guestName || t(`statuses.${occ.status}`, { defaultValue: occ.status })}`;

                elements.push(
                    <button
                        key={occ.reservationId || occ.unitId + dateStr}
                        onClick={() => setSelectedOcc(occ)}
                        style={{ gridColumn: `${i + 1} / span ${span}` }}
                        className={`h-6 md:h-8 ${roundedClass} ${bgColor} ${textColor} text-[9px] md:text-xs font-bold flex items-center justify-center md:justify-start shadow-sm transition-colors truncate cursor-pointer focus:outline-none focus:ring-2 focus:ring-offset-1 focus:ring-brand-primary pointer-events-auto min-w-0`}
                        title={displayText}
                    >
                        <span className="truncate hidden md:block">{displayText}</span>
                        <span className="truncate md:hidden text-[8px] font-black px-0.5">{unitName.slice(0, 3)}</span>
                    </button>
                );
                i += span;
            } else {
                i++;
            }
        }
        return elements;
    };

    const formatGuestLabel = (name?: string) => {
        if (!name || name === 'N/A') return t('occupancy.unassignedGuest');
        if (name.startsWith('Guest ') && name.length > 15) return `#GST-${name.slice(-8)}`;
        return name;
    };

    const handleStatusChange = (id: string, newStatus: string) => {
        updateAdminReservationStatus(id, newStatus)
            .then(() => {
                setMessage({text: t('adminReservations.statusUpdated'), type: 'success'});
                setSelectedOcc(prev => prev && prev.reservationId === id ? {...prev, status: newStatus} : prev);
                fetchOccupancyData();
            })
            .catch((err) => {
                console.error(err);
                setMessage({text: err instanceof Error ? err.message : t('adminReservations.networkError'), type: 'error'});
            });
    };

    return (
        <div className="p-4 md:p-8 max-w-7xl mx-auto min-h-screen text-brand-main space-y-6">
            <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 relative z-[100]">
                <div>
                    <h1 className="text-3xl font-bold text-brand-main">{t('occupancy.title')}</h1>
                    <p className="text-sm text-brand-muted mt-0.5">{t('occupancy.subtitle')}</p>
                </div>
                <div className="flex bg-brand-bg p-1 rounded-lg border border-brand-accent shadow-sm">
                    <Link to="/admin/reservations"
                          className="px-4 py-2 text-sm font-medium rounded-md text-brand-muted hover:bg-[#FFFFFF] hover:text-brand-main hover:shadow-sm transition-all">
                        {t('adminReservations.listView')}
                    </Link>
                    <span
                        className="px-4 py-2 text-sm font-bold rounded-md bg-[#FFFFFF] text-brand-main shadow border border-brand-accent cursor-default">
                        {t('adminReservations.calendarView')}
                    </span>
                </div>
            </div>

            <div
                className="flex flex-col gap-4 relative z-[90] bg-[#FFFFFF] border border-brand-accent p-6 rounded-xl shadow-sm">
                <div className="flex gap-6 items-center flex-wrap justify-between">
                    <div className="flex gap-4 items-center flex-wrap">
                        <span className="text-sm font-bold text-brand-muted">{t('occupancy.dateAnchor')}</span>
                        <div
                            className="flex items-center bg-brand-bg border border-brand-accent rounded-lg px-3 py-2 shadow-sm focus-within:ring-1 focus-within:ring-brand-primary z-[100] gap-2">
                            <svg className="w-4 h-4 text-brand-muted" fill="none" stroke="currentColor"
                                 viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2"
                                      d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"></path>
                            </svg>
                            <SharedDatePicker
                                selected={startDate}
                                onChange={(date: Date | null) => {
                                    if (date) setDateRange([date, endOfMonth(date)])
                                }}
                                placeholderText={t('occupancy.selectMonth')}
                                allowPastDates={true}
                            />
                        </div>
                    </div>
                    <div className="flex items-center gap-3">
                        <button
                            onClick={() => setIsManualReservationOpen(true)}
                            className="px-4 py-2 text-sm font-bold text-white bg-brand-primary hover:bg-brand-primary-hover rounded-lg shadow-sm transition-all flex items-center gap-1.5 cursor-pointer"
                        >
                            <svg className="w-4 h-4" fill="none" stroke="currentColor" strokeWidth="2"
                                 viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" d="M12 4v16m8-8H4"/>
                            </svg>
                            {t('occupancy.addReservation')}
                        </button>
                        <button
                            onClick={() => setIsBlockDatesOpen(true)}
                            className="px-4 py-2 text-sm font-bold text-brand-main bg-white border border-brand-accent hover:bg-gray-50 rounded-lg shadow-sm transition-all flex items-center gap-1.5 cursor-pointer"
                        >
                            <svg className="w-4 h-4" fill="none" stroke="currentColor" strokeWidth="2"
                                 viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round"
                                      d="M18.364 18.364A9 9 0 005.636 5.636m12.728 12.728A9 9 0 015.636 5.636m12.728 12.728L5.636 5.636"/>
                            </svg>
                            {t('blockDates.title')}
                        </button>
                    </div>
                </div>
            </div>

            <div
                className="flex justify-between items-center bg-white border border-brand-accent p-4 rounded-xl shadow-sm mb-6">
                <button onClick={handlePrevMonth}
                        className="p-2 border border-brand-accent rounded-lg hover:bg-gray-50 transition">
                    <svg className="w-6 h-6 text-brand-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 19l-7-7 7-7"/>
                    </svg>
                </button>
                <h2 className="text-xl font-black text-brand-main tracking-tight">
                    {format(anchorDate, 'LLLL yyyy', { locale: getDateFnsLocale(i18n.language) })}
                </h2>
                <button onClick={handleNextMonth}
                        className="p-2 border border-brand-accent rounded-lg hover:bg-gray-50 transition">
                    <svg className="w-6 h-6 text-brand-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 5l7 7-7 7"/>
                    </svg>
                </button>
            </div>

            <div className="w-full overflow-x-auto relative z-10 whitespace-nowrap border border-brand-accent rounded-xl shadow-sm" ref={calendarRef}>
                <div className="w-full md:min-w-[800px] min-w-0 flex flex-col bg-white">
                    <div className="grid grid-cols-7 bg-brand-bg text-center py-2 md:py-3 font-bold text-xs uppercase tracking-wider border-b border-brand-accent">
                        <div className="text-brand-muted"><span className="hidden md:inline">{t('occupancy.mon')}</span><span className="md:hidden">{t('occupancy.mon').slice(0, 1)}</span></div>
                        <div className="text-brand-muted"><span className="hidden md:inline">{t('occupancy.tue')}</span><span className="md:hidden">{t('occupancy.tue').slice(0, 1)}</span></div>
                        <div className="text-brand-muted"><span className="hidden md:inline">{t('occupancy.wed')}</span><span className="md:hidden">{t('occupancy.wed').slice(0, 1)}</span></div>
                        <div className="text-brand-muted"><span className="hidden md:inline">{t('occupancy.thu')}</span><span className="md:hidden">{t('occupancy.thu').slice(0, 1)}</span></div>
                        <div className="text-brand-muted"><span className="hidden md:inline">{t('occupancy.fri')}</span><span className="md:hidden">{t('occupancy.fri').slice(0, 1)}</span></div>
                        <div className="text-brand-muted bg-gray-50/60"><span className="hidden md:inline">{t('occupancy.sat')}</span><span className="md:hidden">{t('occupancy.sat').slice(0, 1)}</span></div>
                        <div className="text-brand-muted bg-gray-50/60"><span className="hidden md:inline">{t('occupancy.sun')}</span><span className="md:hidden">{t('occupancy.sun').slice(0, 1)}</span></div>
                    </div>

                    <div className="w-full bg-white divide-y divide-brand-accent/20">
                        {weeks.map((weekDates, weekIdx) => (
                            <div key={weekIdx} className="min-h-[85px] md:min-h-[110px] flex flex-col justify-between hover:bg-[#F7F7F7]/30 transition-colors">
                                <div className="flex-1 relative min-h-[85px] md:min-h-[110px]">
                                    <div className="absolute inset-0 grid grid-cols-7">
                                        {weekDates.map((d, dayIdx) => {
                                            const dateStr = format(d, 'yyyy-MM-dd');
                                            const isCurrentMonth = format(d, 'MM') === format(anchorDate, 'MM');
                                            const isToday = dateStr === todayStr;
                                            const isSelected = selectedQuickActionDate && dateStr === format(selectedQuickActionDate, 'yyyy-MM-dd');
                                            const isHighlighted = isToday || isSelected;
                                            const isPast = isBefore(d, startOfToday()) && !isToday;
                                            const isWeekend = dayIdx >= 5;
                                            return (
                                                <div
                                                    key={d.toISOString()}
                                                    onClick={() => {
                                                        setQuickAction({ date: d });
                                                        setSelectedQuickActionDate(d);
                                                    }}
                                                    className={`p-1.5 pr-2.5 text-right font-semibold text-xs border-r border-brand-accent/10 last:border-r-0 cursor-pointer transition-colors ${
                                                        isWeekend ? 'bg-gray-50/60' : ''
                                                    } ${
                                                        isHighlighted ? 'bg-brand-accent/20 border-t-4 border-t-brand-primary border-x border-brand-accent/30 shadow-sm hover:bg-brand-accent/30' : 'hover:bg-brand-accent/10'
                                                    }`}
                                                >
                                                    <span
                                                        className={`${isPast ? 'text-gray-300' : isCurrentMonth ? 'text-brand-main' : 'text-gray-300'} ${isHighlighted ? 'font-black text-brand-primary' : ''}`}>
                                                        {format(d, 'd')}
                                                    </span>
                                                </div>
                                            );
                                        })}
                                    </div>
                                    <div className="relative z-[1] p-2 space-y-1 pointer-events-none mt-6">
                                        {getLanesForChunk(weekDates).map((lane, laneIdx) => (
                                            <div key={laneIdx} className="grid grid-cols-7 gap-1">
                                                {renderLaneCellsForWeek(lane, weekDates)}
                                            </div>
                                        ))}
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            </div>

            {quickAction && (
                <>
                    <div className="fixed inset-0 bg-black/50 z-[9998] backdrop-blur-sm" onClick={() => {
                        setQuickAction(null);
                        setSelectedQuickActionDate(null);
                    }}/>
                    <div
                        className="fixed top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 z-[9999] bg-[#FFFFFF] rounded-2xl shadow-2xl border border-brand-accent p-8 w-full max-w-sm space-y-6">
                        <div className="text-center space-y-1">
                            <h3 className="text-lg font-bold text-brand-main">{t('occupancy.quickAction')}</h3>
                            <p className="text-sm text-brand-muted">{t('occupancy.date')} <span className="font-bold text-brand-main">{format(quickAction.date, 'EEEE, d MMMM yyyy', { locale: getDateFnsLocale(i18n.language) })}</span></p>
                        </div>
                        <button onClick={() => { setIsManualReservationOpen(true); setQuickAction(null); }} className="w-full py-3 bg-brand-primary text-white font-bold rounded-lg hover:opacity-90 transition cursor-pointer">{t('occupancy.newBooking')}</button>
                        <button onClick={() => { setIsBlockDatesOpen(true); setQuickAction(null); }} className="w-full py-3 bg-[#FFFFFF] border border-brand-accent text-brand-main font-bold rounded-lg hover:bg-gray-50 transition cursor-pointer">{t('blockDates.title')}</button>
                        <button onClick={() => { setQuickAction(null); setSelectedQuickActionDate(null); }} className="w-full py-3 border border-brand-accent rounded-lg text-brand-main font-bold hover:bg-gray-50 transition cursor-pointer">{t('common.cancel')}</button>
                    </div>
                </>
            )}

            {selectedOcc && (
                <>
                    <div className="fixed inset-0 bg-black/50 z-[9998] backdrop-blur-sm animate-in fade-in duration-200"
                         onClick={() => setSelectedOcc(null)}></div>
                    <div
                        className="fixed top-0 right-0 w-full sm:w-[420px] h-full bg-[#FFFFFF] shadow-2xl z-[9999] border-l border-brand-accent flex flex-col justify-between animate-in slide-in-from-right duration-300">

                        <div className="p-6 border-b border-brand-accent flex items-center justify-between">
                            <div>
                                <span className="text-[10px] font-bold text-brand-muted uppercase tracking-widest block mb-1">{t('occupancy.reservationInfo')}</span>
                                <h2 className="text-xl font-black text-brand-main tracking-tight">
                                    #RES-{selectedOcc.reservationId.slice(-8)}
                                </h2>
                            </div>
                            <button
                                onClick={() => setSelectedOcc(null)}
                                className="p-2 text-brand-muted hover:text-brand-main hover:bg-brand-bg rounded-full transition-colors cursor-pointer"
                            >
                                <svg className="w-5 h-5" fill="none" stroke="currentColor" strokeWidth="2.5"
                                     viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12"/>
                                </svg>
                            </button>
                        </div>

                        <div className="flex-1 overflow-y-auto p-6 space-y-6">

                            <div>
                                <span className="block text-xs font-bold text-brand-muted uppercase tracking-wider mb-2">
                                    {t('occupancy.reservationStatus')}
                                </span>

                                {selectedOcc.status === 'CONFIRMED' ? (
                                    <span
                                        className="inline-flex items-center px-3 py-1 rounded-full text-xs font-bold border uppercase tracking-wider bg-emerald-50 border-emerald-200 text-emerald-800 animate-fade-in">
                                        {t('common.confirmed')}
                                    </span>
                                ) : selectedOcc.status === 'PENDING' ? (
                                    <span
                                        className="inline-flex items-center px-3 py-1 rounded-full text-xs font-bold border uppercase tracking-wider bg-amber-50 border-amber-200 text-amber-800 animate-fade-in">
                                        {t('common.pending')}
                                    </span>
                                ) : selectedOcc.status === 'COMPLETED' ? (
                                    <span
                                        className="inline-flex items-center px-3 py-1 rounded-full text-xs font-bold border uppercase tracking-wider bg-blue-50 border-blue-200 text-blue-800 animate-fade-in">
                                        {t('common.completed')}
                                    </span>
                                ) : selectedOcc.status === 'CANCELLED' ? (
                                    <span
                                        className="inline-flex items-center px-3 py-1 rounded-full text-xs font-bold border uppercase tracking-wider bg-red-50 border-red-200 text-red-800 animate-fade-in">
                                        {t('common.cancelled')}
                                    </span>
                                ) : (
                                    <span
                                        className="inline-flex items-center px-3 py-1 rounded-full text-xs font-bold border uppercase tracking-wider bg-gray-50 border-gray-200 text-gray-800 animate-fade-in">
                                        {t(`statuses.${selectedOcc.status}`, { defaultValue: selectedOcc.status })}
                                    </span>
                                )}
                            </div>

                            <div className="space-y-4">
                                <span className="block text-xs font-bold text-brand-muted uppercase tracking-wider">{t('occupancy.stayAccommodation')}</span>

                                <div className="bg-white border border-brand-accent rounded-xl p-5 shadow-sm space-y-4">
                                    <div>
                                        <span
                                            className="block text-[10px] font-bold text-brand-muted uppercase tracking-wider">{t('occupancy.accommodation')}</span>
                                        <p className="text-base font-bold text-brand-main mt-0.5">
                                            {unitMap.get(selectedOcc.unitId) || selectedOcc.unitName || selectedOcc.unitId}
                                        </p>
                                    </div>

                                    <div className="grid grid-cols-2 gap-4 pt-3 border-t border-brand-accent/20">
                                        <div>
                                            <span
                                                className="block text-[10px] font-bold text-brand-muted uppercase tracking-wider">{t('adminReservations.guest')}</span>
                                            <p className="text-sm font-bold text-brand-main mt-0.5">
                                                {formatGuestLabel(selectedOcc.guestName)}
                                            </p>
                                            {selectedOcc.reservationId && (
                                                <p className="text-xs text-brand-muted mt-0.5 break-all">
                                                    {selectedOcc.guestEmail || t('occupancy.unknownGuest', { defaultValue: 'nieznany gość' })}
                                                </p>
                                            )}
                                        </div>
                                        <div>
                                            <span
                                                className="block text-[10px] font-bold text-brand-muted uppercase tracking-wider">{t('adminReservations.stayDates')}</span>
                                            <p className="text-xs font-semibold text-brand-main mt-1 whitespace-nowrap">
                                                {selectedOcc.startDate} <span
                                                className="text-brand-primary font-bold">→</span> {selectedOcc.endDate}
                                            </p>
                                        </div>
                                    </div>
                                </div>
                            </div>

                            <div className="flex flex-row items-center gap-2 pt-2">
                                <button
                                    disabled={!isConfirmEnabled}
                                    onClick={() => handleStatusChange(selectedOcc.reservationId, 'CONFIRMED')}
                                    className="px-3 py-1.5 text-xs font-semibold rounded-lg border transition-all inline-flex items-center justify-center gap-1 shrink-0 text-gray-700 bg-white border-gray-300 hover:bg-gray-50 active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed disabled:bg-gray-100 disabled:text-gray-400 disabled:border-gray-200 cursor-pointer disabled:cursor-not-allowed"
                                >
                                    {t('common.confirm')}
                                </button>

                                <button
                                    disabled={!isCompleteEnabled}
                                    onClick={() => handleStatusChange(selectedOcc.reservationId, 'COMPLETED')}
                                    className="px-3 py-1.5 text-xs font-semibold rounded-lg border transition-all inline-flex items-center justify-center gap-1 shrink-0 text-gray-700 bg-white border-gray-300 hover:bg-gray-50 active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed disabled:bg-gray-100 disabled:text-gray-400 disabled:border-gray-200 cursor-pointer disabled:cursor-not-allowed"
                                >
                                    {t('common.complete')}
                                </button>

                                <button
                                    disabled={!isCancelEnabled}
                                    onClick={() => handleStatusChange(selectedOcc.reservationId, 'CANCELLED')}
                                    className="px-3 py-1.5 text-xs font-semibold rounded-lg border border-transparent transition-all inline-flex items-center justify-center gap-1 shrink-0 text-red-600 bg-red-50 hover:bg-red-100 active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed disabled:bg-gray-100 disabled:text-gray-400 disabled:border-gray-200 cursor-pointer disabled:cursor-not-allowed"
                                >
                                    {t('common.cancel')}
                                </button>
                            </div>

                            {message && (
                                <div
                                    className={`flex items-center justify-between gap-3 p-4 border-l-4 rounded-r-xl animate-fade-in shadow-sm mb-6 ${
                                        message.type === 'success' ? 'bg-emerald-50 border-emerald-500 text-emerald-700' : 'bg-red-50 border-red-500 text-red-700'
                                    }`}>
                                    <div className="flex items-center gap-3">
                                        <svg className="w-5 h-5 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
                                            <path fillRule="evenodd"
                                                  d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z"
                                                  clipRule="evenodd"/>
                                        </svg>
                                        <span className="text-sm font-semibold">{message.text}</span>
                                    </div>
                                    <button onClick={() => setMessage(null)}
                                            className="font-bold text-lg hover:opacity-75 transition-opacity px-2">&times;</button>
                                </div>
                            )}

                            {selectedOcc.totalPrice !== undefined && selectedOcc.totalPrice !== null && (
                                <div className="space-y-2">
                                    <span className="block text-xs font-bold text-brand-muted uppercase tracking-wider">{t('occupancy.financialSummary')}</span>
                                    <div className="bg-white border border-brand-accent rounded-xl p-5 shadow-sm">
                                        <span
                                            className="text-[10px] font-bold text-brand-muted uppercase tracking-wider block">{t('checkout.totalPrice')}</span>
                                        <p className="text-2xl text-brand-primary font-black mt-1 tracking-tight">
                                            {selectedOcc.totalPrice.toLocaleString(getLocaleCode(i18n.language), {
                                                style: 'currency',
                                                currency: 'PLN'
                                            })}
                                        </p>
                                    </div>
                                </div>
                            )}

                        </div>

                        <div className="p-6 border-t border-brand-accent bg-brand-bg/10 flex flex-col gap-3">
                            <Link
                                to={`/reservations/${selectedOcc.reservationId}`}
                                className="w-full py-3.5 px-4 bg-brand-primary text-white font-bold text-center hover:bg-brand-primary-hover text-sm rounded-lg transition-colors border border-brand-accent shadow-sm flex items-center justify-center gap-2 cursor-pointer"
                            >
                                {t('occupancy.viewFullDetails')}
                            </Link>
                            <button
                                onClick={() => setSelectedOcc(null)}
                                className="w-full py-3 px-4 border border-brand-accent bg-white text-brand-main font-bold hover:bg-gray-50 text-sm rounded-lg transition-colors cursor-pointer"
                            >
                                {t('occupancy.close')}
                            </button>
                        </div>

                    </div>
                </>
            )}

            {isManualReservationOpen && (
                <ManualReservationModal
                    isOpen={isManualReservationOpen}
                    onClose={() => {
                        setIsManualReservationOpen(false);
                        setSelectedQuickActionDate(null);
                    }}
                    onSuccess={fetchOccupancyData}
                    units={uniqueUnits}
                    initialStartDate={selectedQuickActionDate}
                    occupancies={occupancies}
                />
            )}

            {isBlockDatesOpen && (
                <BlockDatesModal
                    isOpen={isBlockDatesOpen}
                    onClose={() => {
                        setIsBlockDatesOpen(false);
                        setSelectedQuickActionDate(null);
                    }}
                    onSuccess={fetchOccupancyData}
                    units={uniqueUnits}
                    initialStartDate={selectedQuickActionDate}
                    occupancies={occupancies}
                />
            )}
        </div>
    );
}
