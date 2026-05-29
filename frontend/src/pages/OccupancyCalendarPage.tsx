import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getOccupancy } from "../api/adminApi";
import { format, addDays, startOfToday, differenceInCalendarDays, isBefore, isAfter, startOfWeek, endOfWeek, startOfMonth, endOfMonth } from "date-fns";
import DatePicker from "react-datepicker";
import "react-datepicker/dist/react-datepicker.css";

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
        startOfToday(),
        addDays(startOfToday(), 27)
    ]);
    const [startDate, endDate] = dateRange;
    const [selectedOcc, setSelectedOcc] = useState<Occupancy | null>(null);

    const isValidRange = !!(startDate && endDate && !isBefore(endDate, startDate));
    const N = isValidRange && startDate && endDate ? differenceInCalendarDays(endDate, startDate) + 1 : 0;
    const CHUNK_SIZE = 14;

    const dates: Date[] = [];
    if (isValidRange && startDate && endDate) {
        let current = startDate;
        while (!isAfter(current, endDate)) {
            dates.push(current);
            current = addDays(current, 1);
        }
    }

    const dateChunks: Date[][] = [];
    for (let i = 0; i < dates.length; i += CHUNK_SIZE) {
        dateChunks.push(dates.slice(i, i + CHUNK_SIZE));
    }

    useEffect(() => {
        if (!isValidRange || !startDate || !endDate) {
            return;
        }

        const startStr = format(startDate, 'yyyy-MM-dd');
        const endStr = format(endDate, 'yyyy-MM-dd');

        getOccupancy(startStr, endStr)
            .then(setOccupancies)
            .catch(console.error);
    }, [startDate, endDate, isValidRange]);

    const unitMap = new Map<string, string>();
    occupancies.forEach(o => {
        if (!unitMap.has(o.unitId)) {
            unitMap.set(o.unitId, o.unitName || o.unitId);
        }
    });

    const setPreset = (preset: 'week' | 'month') => {
        const today = startOfToday();
        if (preset === 'week') setDateRange([startOfWeek(today, { weekStartsOn: 1 }), endOfWeek(today, { weekStartsOn: 1 })]);
        if (preset === 'month') setDateRange([startOfMonth(today), endOfMonth(today)]);
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

    const renderLaneCells = (lane: Occupancy[], chunkDates: Date[]) => {
        const cells = [];
        let i = 0;

        while (i < chunkDates.length) {
            const d = chunkDates[i];
            const dateStr = format(d, 'yyyy-MM-dd');
            const occ = lane.find(o => o.startDate <= dateStr && o.endDate >= dateStr);

            if (occ) {
                const visibleEndIndex = chunkDates.findIndex(date => format(date, 'yyyy-MM-dd') === occ.endDate);
                const endIdx = visibleEndIndex !== -1 ? visibleEndIndex : chunkDates.length - 1;
                const span = endIdx - i + 1;

                const isStart = occ.startDate >= format(chunkDates[0], 'yyyy-MM-dd') && occ.startDate === dateStr;
                const isEnd = occ.endDate <= format(chunkDates[chunkDates.length - 1], 'yyyy-MM-dd') && occ.endDate === format(chunkDates[endIdx], 'yyyy-MM-dd');

                const roundedClass = `${isStart ? 'rounded-l-full pl-4' : 'rounded-l-sm pl-2 border-l border-white/40'} ${isEnd ? 'rounded-r-full pr-4' : 'rounded-r-sm pr-2 border-r border-white/40'}`;

                let bgColor = "bg-[#7A7A7A] hover:bg-[#5A5A5A]";
                let textColor = "text-white";
                if (occ.status === 'CONFIRMED') {
                    bgColor = "bg-emerald-600 hover:bg-emerald-700";
                } else if (occ.status === 'PENDING') {
                    bgColor = "bg-amber-500 hover:bg-amber-600";
                    textColor = "text-[#1A1A1A]";
                }

                const unitName = unitMap.get(occ.unitId) || occ.unitId;
                const displayText = `${unitName} - ${occ.guestName || occ.status}`;

                cells.push(
                    <td key={dateStr} colSpan={span} className="p-1.5 border-b border-r border-[#DACDCA] bg-[#FFFFFF]">
                        <button
                            onClick={() => setSelectedOcc(occ)}
                            className={`w-full h-10 ${roundedClass} ${bgColor} ${textColor} text-xs font-semibold flex items-center shadow-sm transition-colors truncate cursor-pointer focus:outline-none focus:ring-2 focus:ring-offset-1 focus:ring-[#42211D]`}
                        >
                            <span className="truncate">{displayText}</span>
                        </button>
                    </td>
                );
                i += span;
            } else {
                cells.push(
                    <td key={dateStr} className="p-1.5 border-b border-r border-[#DACDCA] bg-[#FFFFFF] min-w-[60px] w-[calc(100%/14)] group">
                        <div className="w-full h-10 rounded flex items-center justify-center bg-transparent group-hover:bg-[#F7F7F7] transition-colors"></div>
                    </td>
                );
                i++;
            }
        }
        return cells;
    };

    return (
        <div className="p-8 max-w-7xl mx-auto min-h-screen text-[#1A1A1A]">
            <div className="flex justify-between items-center mb-6 relative z-[100]">
                <h1 className="text-3xl font-bold text-[#1A1A1A]">Occupancy Dashboard</h1>
                <div className="flex bg-[#F7F7F7] p-1 rounded-lg border border-[#DACDCA] shadow-sm">
                    <Link to="/admin/reservations" className="px-4 py-2 text-sm font-medium rounded-md text-[#7A7A7A] hover:bg-[#FFFFFF] hover:text-[#1A1A1A] hover:shadow-sm transition-all">
                        List View
                    </Link>
                    <span className="px-4 py-2 text-sm font-bold rounded-md bg-[#FFFFFF] text-[#1A1A1A] shadow border border-[#DACDCA] cursor-default">
                        Calendar View
                    </span>
                </div>
            </div>

            <div className="mb-8 flex gap-4 p-6 bg-[#FFFFFF] rounded-xl shadow-sm flex-col border border-[#DACDCA] relative z-[90]">
                <div className="flex gap-6 items-center flex-wrap justify-between">
                    <div className="flex gap-4 items-center flex-wrap">
                        <span className="text-sm font-bold text-[#7A7A7A] uppercase tracking-wider">Date Range</span>
                        <div className="flex gap-3 items-center flex-wrap">
                            <div className="flex items-center bg-[#F7F7F7] border border-[#DACDCA] rounded-lg px-3 py-2 shadow-sm focus-within:ring-1 focus-within:ring-[#42211D] z-[100] gap-2">
                                <svg className="w-4 h-4 text-[#7A7A7A] flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"></path>
                                </svg>
                                <DatePicker
                                    selected={startDate ?? undefined}
                                    onChange={(date: Date | null) => { if (date) setDateRange([date, endDate]) }}
                                    selectsStart
                                    startDate={startDate ?? undefined}
                                    endDate={endDate ?? undefined}
                                    className="bg-transparent text-sm font-bold text-[#1A1A1A] outline-none cursor-pointer w-24 text-center"
                                    dateFormat="yyyy-MM-dd"
                                    placeholderText="Start"
                                    previousMonthButtonLabel={<span className="text-[#1A1A1A] text-xl font-bold leading-none px-2">&lt;</span>}
                                    nextMonthButtonLabel={<span className="text-[#1A1A1A] text-xl font-bold leading-none px-2">&gt;</span>}
                                />
                                <span className="text-[#7A7A7A] font-bold">-</span>
                                <DatePicker
                                    selected={endDate ?? undefined}
                                    onChange={(date: Date | null) => { if (date) setDateRange([startDate, date]) }}
                                    selectsEnd
                                    startDate={startDate ?? undefined}
                                    endDate={endDate ?? undefined}
                                    minDate={startDate ?? undefined}
                                    className="bg-transparent text-sm font-bold text-[#1A1A1A] outline-none cursor-pointer w-24 text-center"
                                    dateFormat="yyyy-MM-dd"
                                    placeholderText="End"
                                    previousMonthButtonLabel={<span className="text-[#1A1A1A] text-xl font-bold leading-none px-2">&lt;</span>}
                                    nextMonthButtonLabel={<span className="text-[#1A1A1A] text-xl font-bold leading-none px-2">&gt;</span>}
                                />
                            </div>
                            <div className="flex gap-2">
                                <button onClick={() => setPreset('week')} className="px-4 py-2 bg-[#F7F7F7] text-[#1A1A1A] font-bold hover:bg-[#e8e8e8] text-sm rounded-lg transition-colors border border-[#DACDCA] shadow-sm">This Week</button>
                                <button onClick={() => setPreset('month')} className="px-4 py-2 bg-[#F7F7F7] text-[#1A1A1A] font-bold hover:bg-[#e8e8e8] text-sm rounded-lg transition-colors border border-[#DACDCA] shadow-sm">This Month</button>
                            </div>
                        </div>
                    </div>
                    {isValidRange && N > 0 && (
                        <div className="text-xs font-bold bg-[#F7F7F7] text-[#42211D] px-4 py-2 rounded-full border border-[#DACDCA] shadow-sm mt-2 sm:mt-0 uppercase tracking-wider">
                            {N} {N === 1 ? 'Day' : 'Days'} selected
                        </div>
                    )}
                </div>

                <div className="flex items-center gap-6 mt-2 pt-4 border-t border-[#DACDCA]">
                    <span className="text-xs font-bold text-[#7A7A7A] uppercase tracking-wider">Legend:</span>
                    <div className="flex gap-5">
                        <div className="flex items-center gap-2"><span className="w-3.5 h-3.5 rounded-full bg-emerald-600 shadow-sm border border-black/10"></span><span className="text-sm font-semibold text-[#1A1A1A]">Confirmed</span></div>
                        <div className="flex items-center gap-2"><span className="w-3.5 h-3.5 rounded-full bg-amber-500 shadow-sm border border-black/10"></span><span className="text-sm font-semibold text-[#1A1A1A]">Pending</span></div>
                        <div className="flex items-center gap-2"><span className="w-3.5 h-3.5 rounded-full bg-[#7A7A7A] shadow-sm border border-black/10"></span><span className="text-sm font-semibold text-[#1A1A1A]">Other</span></div>
                    </div>
                </div>
            </div>

            {isValidRange && (
                <div className="w-full flex flex-col gap-8 relative z-10">
                    {dateChunks.map((chunkDates, chunkIdx) => {
                        const lanes = getLanesForChunk(chunkDates);

                        return (
                            <div key={chunkIdx} className="w-full border border-[#DACDCA] rounded-xl shadow-sm bg-[#FFFFFF] overflow-hidden">
                                <div className="w-full overflow-x-auto">
                                    <table className="w-full border-collapse text-sm table-fixed min-w-max">
                                        <thead>
                                        <tr>
                                            {chunkDates.map(d => (
                                                <th key={d.toISOString()} className="border-b border-r border-[#DACDCA] bg-[#F7F7F7] p-3 text-center text-[#1A1A1A] font-medium w-[calc(100%/14)] last:border-r-0">
                                                    <div className="flex flex-col items-center">
                                                        <span className="text-[10px] text-[#7A7A7A] font-bold uppercase tracking-wider">{format(d, 'eee')}</span>
                                                        <span className="font-bold text-base my-0.5">{format(d, 'dd')}</span>
                                                        <span className="text-[10px] text-[#7A7A7A] font-bold">{format(d, 'MMM')}</span>
                                                    </div>
                                                </th>
                                            ))}
                                            {chunkDates.length < CHUNK_SIZE && Array.from({ length: CHUNK_SIZE - chunkDates.length }).map((_, idx) => (
                                                <th key={`empty-th-${idx}`} className="border-b border-r border-[#DACDCA] bg-[#F7F7F7] w-[calc(100%/14)] last:border-r-0"></th>
                                            ))}
                                        </tr>
                                        </thead>
                                        <tbody>
                                        {lanes.length > 0 ? (
                                            lanes.map((lane, laneIdx) => (
                                                <tr key={`lane-${laneIdx}`} className="hover:bg-[#F7F7F7] transition-colors">
                                                    {renderLaneCells(lane, chunkDates)}
                                                    {chunkDates.length < CHUNK_SIZE && Array.from({ length: CHUNK_SIZE - chunkDates.length }).map((_, idx) => (
                                                        <td key={`empty-td-${idx}`} className="p-1.5 border-b border-r border-[#DACDCA] bg-[#F7F7F7] opacity-50 last:border-r-0"></td>
                                                    ))}
                                                </tr>
                                            ))
                                        ) : (
                                            <tr>
                                                {chunkDates.map(d => (
                                                    <td key={`empty-row-${d.toISOString()}`} className="p-1.5 border-b border-r border-[#DACDCA] bg-[#FFFFFF] h-14 w-[calc(100%/14)] last:border-r-0"></td>
                                                ))}
                                                {chunkDates.length < CHUNK_SIZE && Array.from({ length: CHUNK_SIZE - chunkDates.length }).map((_, idx) => (
                                                    <td key={`empty-row-fill-${idx}`} className="p-1.5 border-b border-r border-[#DACDCA] bg-[#FFFFFF] h-14 w-[calc(100%/14)] last:border-r-0"></td>
                                                ))}
                                            </tr>
                                        )}
                                        </tbody>
                                    </table>
                                </div>
                            </div>
                        );
                    })}
                </div>
            )}

            {selectedOcc && (
                <>
                    <div className="fixed inset-0 bg-[#1A1A1A]/50 z-[9998] backdrop-blur-sm transition-opacity" onClick={() => setSelectedOcc(null)}></div>
                    <div className="fixed top-0 right-0 w-full sm:w-96 h-full bg-[#FFFFFF] opacity-100 shadow-2xl z-[9999] transform transition-transform duration-300 flex flex-col border-l border-[#DACDCA]">

                        <div className="p-6 border-b border-[#DACDCA] flex justify-between items-center bg-[#FFFFFF]">
                            <h2 className="text-xl font-bold text-[#1A1A1A]">Reservation Details</h2>
                            <button onClick={() => setSelectedOcc(null)} className="text-[#7A7A7A] hover:text-[#1A1A1A] hover:bg-[#F7F7F7] p-1.5 rounded-md transition-colors">
                                <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12"></path></svg>
                            </button>
                        </div>

                        <div className="p-6 flex-1 overflow-y-auto bg-[#FFFFFF]">
                            <div className="mb-6">
                                <span className="text-xs font-bold text-[#7A7A7A] uppercase tracking-wider block mb-2">Status</span>
                                <span className={`inline-flex items-center px-3 py-1.5 rounded-full text-xs font-bold border ${selectedOcc.status === 'CONFIRMED' ? 'bg-emerald-50 border-emerald-200 text-emerald-800' :
                                    selectedOcc.status === 'PENDING' ? 'bg-amber-50 border-amber-200 text-amber-800' :
                                        'bg-gray-50 border-gray-200 text-gray-800'
                                }`}>
                                    {selectedOcc.status}
                                </span>
                            </div>

                            <div className="grid grid-cols-1 gap-4">
                                <div className="bg-[#FFFFFF] border border-[#DACDCA] rounded-xl p-4 shadow-sm">
                                    <span className="text-[10px] font-bold text-[#7A7A7A] uppercase tracking-wider block">Reservation ID</span>
                                    <p className="text-[#1A1A1A] font-semibold mt-1 text-sm break-all">{selectedOcc.reservationId}</p>
                                </div>
                                <div className="bg-[#FFFFFF] border border-[#DACDCA] rounded-xl p-4 shadow-sm">
                                    <span className="text-[10px] font-bold text-[#7A7A7A] uppercase tracking-wider block">Unit / Property</span>
                                    <p className="text-[#1A1A1A] font-semibold mt-1 text-base">{unitMap.get(selectedOcc.unitId) || selectedOcc.unitId}</p>
                                </div>
                                <div className="bg-[#FFFFFF] border border-[#DACDCA] rounded-xl p-4 shadow-sm">
                                    <span className="text-[10px] font-bold text-[#7A7A7A] uppercase tracking-wider block">Guest Name</span>
                                    <p className="text-[#1A1A1A] font-semibold mt-1 text-base">{selectedOcc.guestName || "Not provided"}</p>
                                </div>

                                <div className="grid grid-cols-2 gap-4">
                                    <div className="bg-[#FFFFFF] p-4 rounded-xl border border-[#DACDCA] shadow-sm">
                                        <span className="text-[10px] font-bold text-[#7A7A7A] uppercase tracking-wider block">Check-in</span>
                                        <p className="text-[#1A1A1A] font-bold mt-1">{selectedOcc.startDate}</p>
                                    </div>
                                    <div className="bg-[#FFFFFF] p-4 rounded-xl border border-[#DACDCA] shadow-sm">
                                        <span className="text-[10px] font-bold text-[#7A7A7A] uppercase tracking-wider block">Check-out</span>
                                        <p className="text-[#1A1A1A] font-bold mt-1">{selectedOcc.endDate}</p>
                                    </div>
                                </div>

                                {selectedOcc.totalPrice && (
                                    <div className="bg-[#FFFFFF] border border-[#DACDCA] rounded-xl p-4 mt-2 shadow-sm">
                                        <span className="text-[10px] font-bold text-[#7A7A7A] uppercase tracking-wider block">Total Price</span>
                                        <p className="text-2xl text-[#1A1A1A] font-bold mt-1 tracking-tight">{selectedOcc.totalPrice} <span className="text-base font-semibold text-[#7A7A7A]">PLN</span></p>
                                    </div>
                                )}
                            </div>
                        </div>

                        <div className="p-6 border-t border-[#DACDCA] bg-[#FFFFFF]">
                            <Link
                                to={`/reservations/${selectedOcc.reservationId}`}
                                className="w-full flex justify-center items-center py-3.5 px-4 rounded-lg shadow-sm text-sm font-bold text-[#FFFFFF] bg-[#42211D] hover:bg-[#2a1412] transition-colors"
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