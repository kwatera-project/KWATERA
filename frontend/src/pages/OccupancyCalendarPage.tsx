import { useEffect, useState, useRef } from "react";
import { Link } from "react-router-dom";
import { getOccupancy } from "../api/adminApi";
import { format, addDays, startOfToday, isAfter, isBefore, startOfWeek, endOfWeek, startOfMonth, endOfMonth, addMonths } from "date-fns";
import SharedDatePicker from "../components/SharedDatePicker";

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
    const calendarRef = useRef<HTMLDivElement>(null);
    const todayStr = format(startOfToday(), 'yyyy-MM-dd');

    const anchorDate = startDate || startOfToday();
    const firstDayOfMonth = startOfMonth(anchorDate);
    const lastDayOfMonth = endOfMonth(anchorDate);
    
    // Strict 7-column calendar starting on Monday (Mon-Sun)
    const firstCalendarDay = startOfWeek(firstDayOfMonth, { weekStartsOn: 1 });
    const lastCalendarDay = endOfWeek(lastDayOfMonth, { weekStartsOn: 1 });

    const dates: Date[] = [];
    let current = firstCalendarDay;
    while (!isAfter(current, lastCalendarDay)) {
        dates.push(current);
        current = addDays(current, 1);
    }

    // Split dates array into weeks of 7 days
    const weeks: Date[][] = [];
    for (let i = 0; i < dates.length; i += 7) {
        weeks.push(dates.slice(i, i + 7));
    }

    useEffect(() => {
        const startStr = format(firstCalendarDay, 'yyyy-MM-dd');
        const endStr = format(lastCalendarDay, 'yyyy-MM-dd');

        getOccupancy(startStr, endStr)
            .then(setOccupancies)
            .catch(console.error);
    }, [startDate]); // re-fetch only when selected month starts changing

    const unitMap = new Map<string, string>();
    occupancies.forEach(o => {
        if (!unitMap.has(o.unitId)) {
            unitMap.set(o.unitId, o.unitName || o.unitId);
        }
    });

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
            occ.startDate <= chunkEndStr && occ.endDate >= chunkStartStr
        );

        overlapping.sort((a, b) => {
            if (a.startDate === b.startDate) {
                return a.unitId.localeCompare(b.unitId);
            }
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
            if (!placed) {
                lanes.push([occ]);
            }
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
                        style={{
                            gridColumn: `${i + 1} / span ${span}`
                        }}
                        className={`h-8 ${roundedClass} ${bgColor} ${textColor} text-[10px] sm:text-xs font-bold flex items-center shadow-sm transition-colors truncate cursor-pointer focus:outline-none focus:ring-2 focus:ring-offset-1 focus:ring-brand-primary`}
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

    return (
        <div className="p-8 max-w-7xl mx-auto min-h-screen text-brand-main space-y-6">
            {/* Page Header */}
            <div className="flex justify-between items-center relative z-[100]">
                <h1 className="text-3xl font-bold text-brand-main">Occupancy Dashboard</h1>
                <div className="flex bg-brand-bg p-1 rounded-lg border border-brand-accent shadow-sm">
                    <Link to="/admin/reservations" className="px-4 py-2 text-sm font-medium rounded-md text-brand-muted hover:bg-[#FFFFFF] hover:text-brand-main hover:shadow-sm transition-all">
                        List View
                    </Link>
                    <span className="px-4 py-2 text-sm font-bold rounded-md bg-[#FFFFFF] text-brand-main shadow border border-brand-accent cursor-default">
                        Calendar View
                    </span>
                </div>
            </div>

            {/* Filter Bar */}
            <div className="flex flex-col gap-4 relative z-[90] bg-[#FFFFFF] border border-brand-accent p-6 rounded-xl shadow-sm">
                <div className="flex gap-6 items-center flex-wrap justify-between">
                    <div className="flex gap-4 items-center flex-wrap">
                        <span className="text-sm font-bold text-brand-muted">Date Anchor</span>
                        <div className="flex gap-3 items-center flex-wrap">
                            <div className="flex items-center bg-brand-bg border border-brand-accent rounded-lg px-3 py-2 shadow-sm focus-within:ring-1 focus-within:ring-brand-primary z-[100] gap-2">
                                <svg className="w-4 h-4 text-brand-muted flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"></path>
                                </svg>
                                <SharedDatePicker
                                    selected={startDate}
                                    onChange={(date: Date | null) => { if (date) setDateRange([date, endOfMonth(date)]) }}
                                    placeholderText="Select Month"
                                />
                            </div>
                        </div>
                    </div>

                    <div className="flex items-center gap-6">
                        <span className="text-sm font-bold text-brand-muted">Legend:</span>
                        <div className="flex gap-4">
                            <div className="flex items-center gap-2"><span className="w-3.5 h-3.5 rounded-full bg-emerald-600 shadow-sm border border-black/10"></span><span className="text-xs font-bold text-brand-main">Confirmed</span></div>
                            <div className="flex items-center gap-2"><span className="w-3.5 h-3.5 rounded-full bg-amber-500 shadow-sm border border-black/10"></span><span className="text-xs font-bold text-brand-main">Pending</span></div>
                            <div className="flex items-center gap-2"><span className="w-3.5 h-3.5 rounded-full bg-brand-muted shadow-sm border border-black/10"></span><span className="text-xs font-bold text-brand-main">Other</span></div>
                        </div>
                    </div>
                </div>
            </div>

            {/* Monthly Navigator */}
            <div className="flex justify-between items-center bg-white border border-brand-accent p-4 rounded-xl shadow-sm mb-6">
                <button
                    onClick={handlePrevMonth}
                    className="px-4 py-2 text-sm font-bold text-brand-primary bg-brand-bg hover:bg-brand-accent/20 border border-brand-accent rounded-lg transition-colors cursor-pointer"
                >
                    &larr; Previous Month
                </button>
                <h2 className="text-xl font-black text-brand-main tracking-tight">
                    {format(anchorDate, 'MMMM yyyy')}
                </h2>
                <button
                    onClick={handleNextMonth}
                    className="px-4 py-2 text-sm font-bold text-brand-primary bg-brand-bg hover:bg-brand-accent/20 border border-brand-accent rounded-lg transition-colors cursor-pointer"
                >
                    Next Month &rarr;
                </button>
            </div>

            {/* Wall Calendar Grid */}
            <div className="w-full relative z-10 flex flex-col" ref={calendarRef}>
                {/* Weekdays Header */}
                <div className="grid grid-cols-7 border border-brand-accent rounded-t-xl bg-brand-bg text-center py-3 font-bold text-xs uppercase tracking-wider border-b-0">
                    <div className="text-brand-muted">Mon</div>
                    <div className="text-brand-muted">Tue</div>
                    <div className="text-brand-muted">Wed</div>
                    <div className="text-brand-muted">Thu</div>
                    <div className="text-brand-muted">Fri</div>
                    <div className="text-brand-muted bg-gray-50/60">Sat</div>
                    <div className="text-brand-muted bg-gray-50/60">Sun</div>
                </div>

                {/* Weeks Grid Rows */}
                <div className="w-full border border-brand-accent rounded-b-xl shadow-sm bg-white overflow-hidden divide-y divide-brand-accent/20">
                    {weeks.map((weekDates, weekIdx) => (
                        <div key={weekIdx} className="min-h-[110px] flex flex-col justify-between hover:bg-[#F7F7F7]/30 transition-colors">
                            {/* Day Numbers Row */}
                            <div className="grid grid-cols-7 border-b border-brand-accent/10 bg-brand-bg/10">
                                {weekDates.map((d, dayIdx) => {
                                    const dateStr = format(d, 'yyyy-MM-dd');
                                    const isCurrentMonth = format(d, 'MM') === format(anchorDate, 'MM');
                                    const isToday = dateStr === todayStr;
                                    const isPast = isBefore(d, startOfToday()) && !isToday;
                                    const isWeekend = dayIdx >= 5; // Sat=5, Sun=6 in Mon-start grid
                                    return (
                                        <div
                                            key={d.toISOString()}
                                            className={`p-1.5 pr-2.5 text-right font-semibold text-xs border-r border-brand-accent/10 last:border-r-0 ${isWeekend ? 'bg-gray-50/60' : ''} ${isToday ? 'ring-1 ring-inset ring-brand-primary bg-brand-accent/15' : ''}`}
                                        >
                                            <span className={`${isPast ? 'text-gray-300' : isCurrentMonth ? 'text-brand-main' : 'text-gray-300'} ${isToday ? 'font-black text-brand-primary' : ''}`}>
                                                {format(d, 'd')}
                                            </span>
                                        </div>
                                    );
                                })}
                            </div>

                            {/* Event Lanes + Click Grid */}
                            <div className="flex-1 relative min-h-[70px]">
                                {/* Invisible click targets for empty cells (Quick Action) */}
                                <div className="absolute inset-0 grid grid-cols-7">
                                    {weekDates.map((d, dayIdx) => {
                                        const dateStr = format(d, 'yyyy-MM-dd');
                                        const isPast = isBefore(d, startOfToday());
                                        const hasOcc = occupancies.some(o => o.startDate <= dateStr && o.endDate >= dateStr);
                                        const isWeekend = dayIdx >= 5;
                                        return (
                                            <div
                                                key={d.toISOString()}
                                                onClick={() => { if (!isPast && !hasOcc) setQuickAction({ date: d }); }}
                                                className={`h-full border-r border-brand-accent/5 last:border-r-0 ${isWeekend ? 'bg-gray-50/40' : ''} ${!isPast && !hasOcc ? 'cursor-pointer hover:bg-brand-accent/10 transition-colors' : ''}`}
                                            />
                                        );
                                    })}
                                </div>
                                {/* Reservation lanes overlay */}
                                <div className="relative z-[1] p-2 space-y-1">
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

            {/* Quick Action Modal */}
            {quickAction && (
                <>
                    <div className="fixed inset-0 bg-black/50 z-[9998] backdrop-blur-sm" onClick={() => setQuickAction(null)} />
                    <div className="fixed top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 z-[9999] bg-white rounded-2xl shadow-2xl border border-brand-accent p-8 w-full max-w-sm space-y-6">
                        <div className="text-center space-y-1">
                            <h3 className="text-lg font-bold text-brand-main">Quick Action</h3>
                            <p className="text-sm text-brand-muted">
                                Selected date: <span className="font-bold text-brand-main">{format(quickAction.date, 'EEEE, d MMMM yyyy')}</span>
                            </p>
                        </div>
                        <div className="bg-gray-50 border border-gray-100 rounded-lg p-3 flex justify-between items-center shadow-sm">
                            <span className="text-xs font-bold text-brand-muted uppercase tracking-wider">Guest</span>
                            <span className="text-sm font-semibold text-brand-main">Unassigned Guest</span>
                        </div>
                        <div className="space-y-3">
                            <button
                                onClick={() => setQuickAction(null)}
                                className="w-full py-3 px-4 rounded-lg text-sm font-bold text-white bg-brand-primary hover:opacity-90 transition-all shadow-sm cursor-pointer flex items-center justify-center gap-2"
                            >
                                <svg className="w-4 h-4" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" d="M12 4v16m8-8H4" /></svg>
                                New Booking
                            </button>
                            <button
                                onClick={() => setQuickAction(null)}
                                className="w-full py-3 px-4 rounded-lg text-sm font-bold text-brand-main bg-brand-bg border border-brand-accent hover:bg-gray-200 transition-all shadow-sm cursor-pointer flex items-center justify-center gap-2"
                            >
                                <svg className="w-4 h-4" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" d="M18.364 18.364A9 9 0 005.636 5.636m12.728 12.728A9 9 0 015.636 5.636m12.728 12.728L5.636 5.636" /></svg>
                                Block Dates
                            </button>
                        </div>
                        <button
                            onClick={() => setQuickAction(null)}
                            className="w-full text-center text-sm font-medium text-gray-500 hover:text-brand-main transition-colors cursor-pointer"
                        >
                            Cancel
                        </button>
                    </div>
                </>
            )}

            {/* Slide-out Drawer Panel */}
            {selectedOcc && (
                <>
                    <div className="fixed inset-0 bg-[#1A1A1A]/50 z-[9998] backdrop-blur-sm transition-opacity" onClick={() => setSelectedOcc(null)}></div>
                    <div className="fixed top-0 right-0 w-full sm:w-96 h-full bg-[#FFFFFF] opacity-100 shadow-2xl z-[9999] transform transition-transform duration-300 flex flex-col border-l border-brand-accent">

                        <div className="p-6 border-b border-brand-accent flex justify-between items-center bg-[#FFFFFF]">
                            <h2 className="text-xl font-bold text-brand-main">Reservation Details</h2>
                            <button onClick={() => setSelectedOcc(null)} className="text-brand-muted hover:text-brand-main hover:bg-brand-bg p-1.5 rounded-md transition-colors">
                                <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12"></path></svg>
                            </button>
                        </div>

                        <div className="p-6 flex-1 overflow-y-auto bg-[#FFFFFF]">
                            <div className="mb-6">
                                <span className="text-xs font-bold text-brand-muted block mb-2">Status</span>
                                <span className={`inline-flex items-center px-3 py-1.5 rounded-full text-xs font-bold border uppercase tracking-wider ${
                                    selectedOcc.status === 'CONFIRMED' ? 'bg-emerald-50 border-emerald-200 text-emerald-800' :
                                    selectedOcc.status === 'PENDING' ? 'bg-amber-50 border-amber-200 text-amber-800' :
                                    'bg-gray-50 border-gray-200 text-gray-800'
                                }`}>
                                    {selectedOcc.status}
                                </span>
                            </div>

                            <div className="grid grid-cols-1 gap-4">
                                <div className="bg-[#FFFFFF] border border-brand-accent rounded-xl p-4 shadow-sm">
                                    <span className="text-[10px] font-bold text-brand-muted block">Reservation ID</span>
                                    <p className="text-brand-main font-semibold mt-1 text-sm break-all">#RES-{selectedOcc.reservationId.slice(-8)}</p>
                                </div>
                                <div className="bg-[#FFFFFF] border border-brand-accent rounded-xl p-4 shadow-sm">
                                    <span className="text-[10px] font-bold text-brand-muted block">Unit / Property</span>
                                    <p className="text-brand-main font-semibold mt-1 text-base">{unitMap.get(selectedOcc.unitId) || selectedOcc.unitId}</p>
                                </div>
                                <div className="bg-[#FFFFFF] border border-brand-accent rounded-xl p-4 shadow-sm">
                                    <span className="text-[10px] font-bold text-brand-muted block">Guest Name</span>
                                    <p className="text-brand-main font-semibold mt-1 text-base">
                                        {formatGuestLabel(selectedOcc.guestName)}
                                    </p>
                                </div>

                                <div className="grid grid-cols-2 gap-4">
                                    <div className="bg-[#FFFFFF] p-4 rounded-xl border border-brand-accent shadow-sm">
                                        <span className="text-[10px] font-bold text-brand-muted block">Check-in</span>
                                        <p className="text-brand-main font-bold mt-1">{selectedOcc.startDate}</p>
                                    </div>
                                    <div className="bg-[#FFFFFF] p-4 rounded-xl border border-brand-accent shadow-sm">
                                        <span className="text-[10px] font-bold text-brand-muted block">Check-out</span>
                                        <p className="text-brand-main font-bold mt-1">{selectedOcc.endDate}</p>
                                    </div>
                                </div>

                                {selectedOcc.totalPrice && (
                                    <div className="bg-[#FFFFFF] border border-brand-accent rounded-xl p-4 mt-2 shadow-sm">
                                        <span className="text-[10px] font-bold text-brand-muted block">Total Price</span>
                                        <p className="text-2xl text-brand-main font-bold mt-1 tracking-tight">{selectedOcc.totalPrice} <span className="text-base font-semibold text-brand-muted">PLN</span></p>
                                    </div>
                                )}
                            </div>
                        </div>

                        <div className="p-6 border-t border-brand-accent bg-[#FFFFFF]">
                            <Link
                                to={`/reservations/${selectedOcc.reservationId}`}
                                className="w-full flex justify-center items-center py-3.5 px-4 rounded-lg shadow-sm text-sm font-bold text-white bg-brand-primary hover:bg-brand-primary-hover transition-colors cursor-pointer"
                            >
                                View Full Details
                            </Link>
                        </div>
                    </div>
                </>
            )}
        </div>
    );
}