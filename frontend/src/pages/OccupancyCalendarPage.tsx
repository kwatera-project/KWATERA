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

interface Occupancy {
    reservationId: string;
    unitId: string;
    unitName?: string;
    startDate: string;
    endDate: string;
    status: string;
    guestName?: string;
    totalPrice?: number;
}

export default function OccupancyCalendarPage() {
    const [occupancies, setOccupancies] = useState<Occupancy[]>([]);
    const [dateRange, setDateRange] = useState<[Date | null, Date | null]>([
        startOfMonth(startOfToday()),
        endOfMonth(startOfToday())
    ]);
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
                const displayText = `${unitName} - ${occ.guestName || occ.status}`;

                elements.push(
                    <button
                        key={occ.reservationId || occ.unitId + dateStr}
                        onClick={() => setSelectedOcc(occ)}
                        style={{gridColumn: `${i + 1} / span ${span}`}}
                        className={`h-8 ${roundedClass} ${bgColor} ${textColor} text-[10px] sm:text-xs font-bold flex items-center shadow-sm transition-colors truncate cursor-pointer focus:outline-none focus:ring-2 focus:ring-offset-1 focus:ring-brand-primary pointer-events-auto`}
                    >
                        <span className="truncate">{displayText}</span>
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
        if (!name || name === 'N/A') return 'Unassigned Guest';
        if (name.startsWith('Guest ') && name.length > 15) return `#GST-${name.slice(-8)}`;
        return name;
    };

    const handleStatusChange = (id: string, newStatus: string) => {
        updateAdminReservationStatus(id, newStatus)
            .then(() => {
                setMessage({text: "Reservation status updated successfully!", type: 'success'});
                setSelectedOcc(prev => prev && prev.reservationId === id ? {...prev, status: newStatus} : prev);
                fetchOccupancyData();
            })
            .catch((err) => {
                console.error(err);
                setMessage({text: err instanceof Error ? err.message : "Network error occurred", type: 'error'});
            });
    };

    return (
        <div className="p-8 max-w-7xl mx-auto min-h-screen text-brand-main space-y-6">
            <div
                className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 relative z-[100]">
                <div>
                    <h1 className="text-3xl font-bold text-brand-main">Occupancy Dashboard</h1>
                    <p className="text-sm text-brand-muted mt-0.5">Manage occupancies, manual reservations, and
                        maintenance blocks.</p>
                </div>
                <div className="flex bg-brand-bg p-1 rounded-lg border border-brand-accent shadow-sm">
                    <Link to="/admin/reservations"
                          className="px-4 py-2 text-sm font-medium rounded-md text-brand-muted hover:bg-[#FFFFFF] hover:text-brand-main hover:shadow-sm transition-all">
                        List View
                    </Link>
                    <span
                        className="px-4 py-2 text-sm font-bold rounded-md bg-[#FFFFFF] text-brand-main shadow border border-brand-accent cursor-default">
                        Calendar View
                    </span>
                </div>
            </div>

            <div
                className="flex flex-col gap-4 relative z-[90] bg-[#FFFFFF] border border-brand-accent p-6 rounded-xl shadow-sm">
                <div className="flex gap-6 items-center flex-wrap justify-between">
                    <div className="flex gap-4 items-center flex-wrap">
                        <span className="text-sm font-bold text-brand-muted">Date Anchor</span>
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
                                placeholderText="Select Month"
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
                            Add Reservation
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
                            Block Dates
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
                    {format(anchorDate, 'MMMM yyyy')}
                </h2>
                <button onClick={handleNextMonth}
                        className="p-2 border border-brand-accent rounded-lg hover:bg-gray-50 transition">
                    <svg className="w-6 h-6 text-brand-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 5l7 7-7 7"/>
                    </svg>
                </button>
            </div>

            <div className="w-full relative z-10 flex flex-col" ref={calendarRef}>
                <div
                    className="grid grid-cols-7 border border-brand-accent rounded-t-xl bg-brand-bg text-center py-3 font-bold text-xs uppercase tracking-wider border-b-0">
                    <div className="text-brand-muted">Mon</div>
                    <div className="text-brand-muted">Tue</div>
                    <div className="text-brand-muted">Wed</div>
                    <div className="text-brand-muted">Thu</div>
                    <div className="text-brand-muted">Fri</div>
                    <div className="text-brand-muted bg-gray-50/60">Sat</div>
                    <div className="text-brand-muted bg-gray-50/60">Sun</div>
                </div>

                <div
                    className="w-full border border-brand-accent rounded-b-xl shadow-sm bg-white overflow-hidden divide-y divide-brand-accent/20">
                    {weeks.map((weekDates, weekIdx) => (
                        <div key={weekIdx}
                             className="min-h-[110px] flex flex-col justify-between hover:bg-[#F7F7F7]/30 transition-colors">
                            <div className="grid grid-cols-7 border-b border-brand-accent/10 bg-brand-bg/10">
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
                                            className={`p-1.5 pr-2.5 text-right font-semibold text-xs border-r border-brand-accent/10 last:border-r-0 ${isWeekend ? 'bg-gray-50/60' : ''} ${
                                                isHighlighted ? 'bg-brand-accent/20 border-t-4 border-t-brand-primary border-x border-brand-accent/30 shadow-sm' : ''
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
                            <div className="flex-1 relative min-h-[70px]">
                                <div className="absolute inset-0 grid grid-cols-7">
                                    {weekDates.map((d, dayIdx) => {
                                        const dateStr = format(d, 'yyyy-MM-dd');
                                        const isToday = dateStr === todayStr;
                                        const isSelected = selectedQuickActionDate && dateStr === format(selectedQuickActionDate, 'yyyy-MM-dd');
                                        const isHighlighted = isToday || isSelected;
                                        const isWeekend = dayIdx >= 5;
                                        return (
                                            <div
                                                key={d.toISOString()}
                                                onClick={() => {
                                                    setQuickAction({date: d});
                                                    setSelectedQuickActionDate(d);
                                                }}
                                                className={`h-full border-r border-brand-accent/5 last:border-r-0 ${
                                                    isWeekend ? "bg-gray-50/40" : ""
                                                } ${
                                                    isHighlighted ? "bg-brand-accent/10 border-x border-b border-brand-accent/30 shadow-inner" : ""
                                                } cursor-pointer hover:bg-brand-accent/20 transition-colors`}
                                            />
                                        );
                                    })}
                                </div>
                                <div className="relative z-[1] p-2 space-y-1 pointer-events-none">
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

            {quickAction && (
                <>
                    <div className="fixed inset-0 bg-black/50 z-[9998] backdrop-blur-sm" onClick={() => {
                        setQuickAction(null);
                        setSelectedQuickActionDate(null);
                    }}/>
                    <div
                        className="fixed top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 z-[9999] bg-[#FFFFFF] rounded-2xl shadow-2xl border border-brand-accent p-8 w-full max-w-sm space-y-6">
                        <div className="text-center space-y-1">
                            <h3 className="text-lg font-bold text-brand-main">Quick Action</h3>
                            <p className="text-sm text-brand-muted">Date: <span
                                className="font-bold text-brand-main">{format(quickAction.date, 'EEEE, d MMMM yyyy')}</span>
                            </p>
                        </div>
                        <button onClick={() => {
                            setIsManualReservationOpen(true);
                            setQuickAction(null);
                        }}
                                className="w-full py-3 bg-brand-primary text-white font-bold rounded-lg hover:opacity-90 transition cursor-pointer">New
                            Booking
                        </button>
                        <button onClick={() => {
                            setIsBlockDatesOpen(true);
                            setQuickAction(null);
                        }}
                                className="w-full py-3 bg-[#FFFFFF] border border-brand-accent text-brand-main font-bold rounded-lg hover:bg-gray-50 transition cursor-pointer">Block
                            Dates
                        </button>
                        <button onClick={() => {
                            setQuickAction(null);
                            setSelectedQuickActionDate(null);
                        }}
                                className="w-full py-3 border border-brand-accent rounded-lg text-brand-main font-bold hover:bg-gray-50 transition cursor-pointer">Cancel
                        </button>
                    </div>
                </>
            )}

            {selectedOcc && (
                <>
                    <div className="fixed inset-0 bg-black/50 z-[9998] backdrop-blur-sm animate-in fade-in duration-200"
                         onClick={() => setSelectedOcc(null)}></div>
                    <div
                        className="fixed top-0 right-0 w-full sm:w-[420px] h-full bg-[#FFFFFF] shadow-2xl z-[9999] border-l border-brand-accent flex flex-col justify-between animate-in slide-in-from-right duration-300">

                        {/* Header */}
                        <div className="p-6 border-b border-brand-accent flex items-center justify-between">
                            <div>
                                <span
                                    className="text-[10px] font-bold text-brand-muted uppercase tracking-widest block mb-1">Reservation Info</span>
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

                        {/* Content */}
                        <div className="flex-1 overflow-y-auto p-6 space-y-6">

                            {/* Status Badge */}
                            <div>
                                <span className="block text-xs font-bold text-brand-muted uppercase tracking-wider mb-2">
                                    Reservation Status
                                </span>

                                {selectedOcc.status === 'CONFIRMED' ? (
                                    <span
                                        className="inline-flex items-center px-3 py-1 rounded-full text-xs font-bold border uppercase tracking-wider bg-emerald-50 border-emerald-200 text-emerald-800 animate-fade-in">
                                        Confirmed
                                    </span>
                                ) : selectedOcc.status === 'PENDING' ? (
                                    <span
                                        className="inline-flex items-center px-3 py-1 rounded-full text-xs font-bold border uppercase tracking-wider bg-amber-50 border-amber-200 text-amber-800 animate-fade-in">
                                        Pending
                                    </span>
                                ) : selectedOcc.status === 'COMPLETED' ? (
                                    <span
                                        className="inline-flex items-center px-3 py-1 rounded-full text-xs font-bold border uppercase tracking-wider bg-blue-50 border-blue-200 text-blue-800 animate-fade-in">
                                        Completed
                                    </span>
                                ) : selectedOcc.status === 'CANCELLED' ? (
                                    <span
                                        className="inline-flex items-center px-3 py-1 rounded-full text-xs font-bold border uppercase tracking-wider bg-red-50 border-red-200 text-red-800 animate-fade-in">
                                        Cancelled
                                    </span>
                                ) : (
                                    <span
                                        className="inline-flex items-center px-3 py-1 rounded-full text-xs font-bold border uppercase tracking-wider bg-gray-50 border-gray-200 text-gray-800 animate-fade-in">
                                        {selectedOcc.status}
                                    </span>
                                )}
                            </div>

                            {/* Accommodation Details */}
                            <div className="space-y-4">
                                <span className="block text-xs font-bold text-brand-muted uppercase tracking-wider">Stay & Accommodation</span>

                                <div className="bg-white border border-brand-accent rounded-xl p-5 shadow-sm space-y-4">
                                    <div>
                                        <span
                                            className="block text-[10px] font-bold text-brand-muted uppercase tracking-wider">Accommodation</span>
                                        <p className="text-base font-bold text-brand-main mt-0.5">
                                            {unitMap.get(selectedOcc.unitId) || selectedOcc.unitName || selectedOcc.unitId}
                                        </p>
                                    </div>

                                    <div className="grid grid-cols-2 gap-4 pt-3 border-t border-brand-accent/20">
                                        <div>
                                            <span
                                                className="block text-[10px] font-bold text-brand-muted uppercase tracking-wider">Guest</span>
                                            <p className="text-sm font-bold text-brand-main mt-0.5">
                                                {formatGuestLabel(selectedOcc.guestName)}
                                            </p>
                                        </div>
                                        <div>
                                            <span
                                                className="block text-[10px] font-bold text-brand-muted uppercase tracking-wider">Dates</span>
                                            <p className="text-xs font-semibold text-brand-main mt-1 whitespace-nowrap">
                                                {selectedOcc.startDate} <span
                                                className="text-brand-primary font-bold">→</span> {selectedOcc.endDate}
                                            </p>
                                        </div>
                                    </div>
                                </div>
                            </div>

                            {/* Status Change Buttons Grouped in Row */}
                            <div className="flex flex-row items-center gap-2 pt-2">
                                <button
                                    disabled={!isConfirmEnabled}
                                    onClick={() => handleStatusChange(selectedOcc.reservationId, 'CONFIRMED')}
                                    className="px-3 py-1.5 text-xs font-semibold rounded-lg border transition-all inline-flex items-center justify-center gap-1 shrink-0 text-gray-700 bg-white border-gray-300 hover:bg-gray-50 active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed disabled:bg-gray-100 disabled:text-gray-400 disabled:border-gray-200 cursor-pointer disabled:cursor-not-allowed"
                                >
                                    Confirm
                                </button>

                                <button
                                    disabled={!isCompleteEnabled}
                                    onClick={() => handleStatusChange(selectedOcc.reservationId, 'COMPLETED')}
                                    className="px-3 py-1.5 text-xs font-semibold rounded-lg border transition-all inline-flex items-center justify-center gap-1 shrink-0 text-gray-700 bg-white border-gray-300 hover:bg-gray-50 active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed disabled:bg-gray-100 disabled:text-gray-400 disabled:border-gray-200 cursor-pointer disabled:cursor-not-allowed"
                                >
                                    Complete
                                </button>

                                <button
                                    disabled={!isCancelEnabled}
                                    onClick={() => handleStatusChange(selectedOcc.reservationId, 'CANCELLED')}
                                    className="px-3 py-1.5 text-xs font-semibold rounded-lg border border-transparent transition-all inline-flex items-center justify-center gap-1 shrink-0 text-red-600 bg-red-50 hover:bg-red-100 active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed disabled:bg-gray-100 disabled:text-gray-400 disabled:border-gray-200 cursor-pointer disabled:cursor-not-allowed"
                                >
                                    Cancel
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

                            {/* Total Price */}
                            {selectedOcc.totalPrice !== undefined && selectedOcc.totalPrice !== null && (
                                <div className="space-y-2">
                                    <span className="block text-xs font-bold text-brand-muted uppercase tracking-wider">Financial Summary</span>
                                    <div className="bg-white border border-brand-accent rounded-xl p-5 shadow-sm">
                                        <span
                                            className="text-[10px] font-bold text-brand-muted uppercase tracking-wider block">Total Price</span>
                                        <p className="text-2xl text-brand-primary font-black mt-1 tracking-tight">
                                            {selectedOcc.totalPrice.toLocaleString('pl-PL', {
                                                style: 'currency',
                                                currency: 'PLN'
                                            })}
                                        </p>
                                    </div>
                                </div>
                            )}

                        </div>

                        {/* Footer Action Buttons */}
                        <div className="p-6 border-t border-brand-accent bg-brand-bg/10 flex flex-col gap-3">
                            <Link
                                to={`/reservations/${selectedOcc.reservationId}`}
                                className="w-full py-3.5 px-4 bg-brand-primary text-white font-bold text-center hover:bg-brand-primary-hover text-sm rounded-lg transition-colors border border-brand-accent shadow-sm flex items-center justify-center gap-2 cursor-pointer"
                            >
                                View Full Details
                            </Link>
                            <button
                                onClick={() => setSelectedOcc(null)}
                                className="w-full py-3 px-4 border border-brand-accent bg-white text-brand-main font-bold hover:bg-gray-50 text-sm rounded-lg transition-colors cursor-pointer"
                            >
                                Close
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