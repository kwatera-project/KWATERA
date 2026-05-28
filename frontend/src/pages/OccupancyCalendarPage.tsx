import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getOccupancy } from "../api/adminApi";

import { format, addDays, startOfToday, differenceInCalendarDays, isBefore, isAfter } from "date-fns";
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
    const [startDate, setStartDate] = useState(startOfToday());
    const [endDate, setEndDate] = useState(() => addDays(startOfToday(), 6));

    const isValidRange = startDate && endDate && !isBefore(endDate, startDate);
    const N = isValidRange ? differenceInCalendarDays(endDate, startDate) + 1 : 0;

    const dates: Date[] = [];
    if (isValidRange) {
        let current = startDate;
        while (!isAfter(current, endDate)) {
            dates.push(current);
            current = addDays(current, 1);
        }
    }

    useEffect(() => {
        if (!isValidRange) {
            setOccupancies([]);
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
    const unitIds = Array.from(unitMap.keys());

    const renderRowCells = (unitId: string, chunkDates: Date[]) => {
        const cells = [];
        let i = 0;

        while (i < chunkDates.length) {
            const d = chunkDates[i];
            const dateStr = format(d, 'yyyy-MM-dd');
            const occ = occupancies.find(o => o.unitId === unitId && o.startDate <= dateStr && o.endDate >= dateStr);

            if (occ) {
                const visibleEndIndex = chunkDates.findIndex(date => format(date, 'yyyy-MM-dd') === occ.endDate);
                const endIdx = visibleEndIndex !== -1 ? visibleEndIndex : chunkDates.length - 1;
                const span = endIdx - i + 1;

                let bgColor = "bg-gray-300 text-title";
                if (occ.status === 'CONFIRMED') bgColor = "bg-button text-white";
                if (occ.status === 'PENDING') bgColor = "bg-main text-title";

                cells.push(
                    <td key={dateStr} colSpan={span} className="border border-gray-300 p-1 relative group">
                        <Link
                            to={`/reservations/${occ.reservationId}`}
                            className={`block w-full h-8 rounded ${bgColor} text-white text-xs font-bold flex items-center justify-center hover:opacity-90 transition-opacity px-2 shadow-sm truncate`}
                        >
                            {occ.status}
                        </Link>

                        <div className="absolute hidden group-hover:block z-50 bg-gray-900 text-white p-3 rounded shadow-xl text-xs w-56 left-1/2 transform -translate-x-1/2 bottom-full mb-2 pointer-events-none">
                            <p className="mb-1"><span className="text-gray-400 font-normal">ID:</span> {occ.reservationId.substring(0, 8)}...</p>
                            <p className="mb-1"><span className="text-gray-400 font-normal">From:</span> {occ.startDate}</p>
                            <p className="mb-1"><span className="text-gray-400 font-normal">To:</span> {occ.endDate}</p>
                            <p className="mb-1"><span className="text-gray-400 font-normal">Status:</span> {occ.status}</p>
                            {occ.guestName && <p className="mb-1"><span className="text-gray-400 font-normal">Guest:</span> {occ.guestName}</p>}
                            {occ.totalPrice && <p><span className="text-gray-400 font-normal">Price:</span> {occ.totalPrice} PLN</p>}
                        </div>
                    </td>
                );
                i += span;
            } else {
                cells.push(
                    <td key={dateStr} className="border border-gray-300 p-1 bg-white min-w-[80px]">
                        <span className="block h-8"></span>
                    </td>
                );
                i++;
            }
        }
        return cells;
    };

    return (
        <div className="p-8 max-w-7xl mx-auto">
            <div className="flex justify-between items-center mb-6">
                <h1 className="text-3xl font-bold text-title">Occupancy</h1>
                <div className="flex bg-main p-1 rounded-lg border border-pink-burgundy shadow-sm">
                    <Link
                        to="/admin/reservations"
                        className="px-4 py-2 text-sm font-medium rounded-md text-title hover:bg-white hover:shadow-sm transition-all"
                    >
                        List View
                    </Link>
                    <span className="px-4 py-2 text-sm font-medium rounded-md bg-button text-white shadow cursor-default">
                        Calendar View
                    </span>
                </div>
            </div>

            <div className="mb-6 flex gap-4 p-4 bg-card rounded-xl shadow items-center flex-wrap justify-between">
                <div className="flex gap-4 items-center flex-wrap">
                    <span className="text-sm font-semibold text-title">Date Range:</span>
                    <div className="flex gap-3 items-center flex-wrap">
                        <div className="flex items-center gap-2 bg-main/10 p-1.5 rounded-lg border border-pink-burgundy/40">
                            <div className="flex items-center gap-1.5 relative z-[60]">
                                <span className="text-xs font-medium text-details pl-1">Start:</span>
                                <DatePicker
                                    selected={startDate}
                                    onChange={(date: Date | null) => date && setStartDate(date)}
                                    selectsStart
                                    startDate={startDate}
                                    endDate={endDate}
                                    className="px-3 py-1.5 bg-card border border-gray-300 rounded text-sm w-28 text-center cursor-pointer font-medium text-title outline-none focus:ring-2 focus:ring-button"
                                    dateFormat="yyyy-MM-dd"
                                    previousMonthButtonLabel={
                                        <svg fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" d="M15 19l-7-7 7-7" /></svg>
                                    }
                                    nextMonthButtonLabel={
                                        <svg fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" d="M9 5l7 7-7 7" /></svg>
                                    }
                                />
                            </div>
                            
                            <span className="text-gray-400 font-light px-0.5">to</span>

                            <div className="flex items-center gap-1.5 relative z-[60]">
                                <span className="text-xs font-medium text-details">End:</span>
                                <DatePicker
                                    selected={endDate}
                                    onChange={(date: Date | null) => date && setEndDate(date)}
                                    selectsEnd
                                    startDate={startDate}
                                    endDate={endDate}
                                    minDate={startDate}
                                    className="px-3 py-1.5 bg-card border border-gray-300 rounded text-sm w-28 text-center cursor-pointer font-medium text-title outline-none focus:ring-2 focus:ring-button"
                                    dateFormat="yyyy-MM-dd"
                                    previousMonthButtonLabel={
                                        <svg fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" d="M15 19l-7-7 7-7" /></svg>
                                    }
                                    nextMonthButtonLabel={
                                        <svg fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" d="M9 5l7 7-7 7" /></svg>
                                    }
                                />
                            </div>
                        </div>

                        <button
                            onClick={() => {
                                const today = startOfToday();
                                if (isValidRange) {
                                    setStartDate(today);
                                    setEndDate(addDays(today, N - 1));
                                } else {
                                    setStartDate(today);
                                    setEndDate(addDays(today, 6));
                                }
                            }}
                            className="px-4 py-2 bg-button text-white hover:bg-button-hover text-sm font-medium rounded transition-colors shadow-sm"
                        >
                            Today
                        </button>
                    </div>
                </div>

                {isValidRange && (
                    <div className="text-xs font-semibold bg-button/10 text-button px-3 py-1.5 rounded-full border border-button/20 shadow-sm mt-2 sm:mt-0">
                        {N} {N === 1 ? "Day" : "Days"} selected
                    </div>
                )}
            </div>

            {!isValidRange && (
                <div className="mb-6 p-4 bg-red-50 border border-red-200 text-red-700 rounded-xl flex items-center gap-3 shadow-sm animate-pulse">
                    <svg className="w-5 h-5 flex-shrink-0 text-red-600" fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                    </svg>
                    <div className="flex flex-col">
                        <span className="font-semibold text-title">Invalid Date Range Selected</span>
                        <span className="text-sm text-red-600/90">The End Date must be on or after the Start Date. Please select a valid range to load occupancy data.</span>
                    </div>
                </div>
            )}

            {isValidRange && (
                <div className="bg-card rounded-xl shadow p-4 overflow-x-auto border border-gray-200">
                    <table className="w-full border-collapse min-w-max text-sm">
                        <thead>
                            <tr>
                                <th className="border-b border-r border-gray-200 p-3 bg-card text-left font-semibold text-title sticky left-0 z-20 w-56 shadow-[1px_0_0_0_#e5e7eb]">
                                    Unit / Property
                                </th>
                                {dates.map(d => (
                                    <th key={d.toISOString()} className="border-b border-r border-gray-200 p-2 bg-card text-center text-title font-medium min-w-[80px]">
                                        <div className="flex flex-col">
                                            <span>{format(d, 'MMM dd')}</span>
                                        </div>
                                    </th>
                                ))}
                            </tr>
                        </thead>
                        <tbody>
                            {unitIds.map(unitId => {
                                const name = unitMap.get(unitId) || unitId;
                                return (
                                    <tr key={unitId} className="hover:bg-gray-50/50">
                                        <td className="border-b border-r border-gray-200 p-3 bg-card sticky left-0 z-10 shadow-[1px_0_0_0_#e5e7eb]">
                                            <div className="flex flex-col truncate max-w-[14rem]">
                                                <span className="font-semibold text-title truncate" title={name}>
                                                    {name.length > 20 ? name.substring(0, 20) + '...' : name}
                                                </span>
                                            </div>
                                        </td>
                                        {renderRowCells(unitId, dates)}
                                    </tr>
                                );
                            })}
                            {unitIds.length === 0 && (
                                <tr>
                                    <td colSpan={dates.length + 1} className="p-8 text-center text-gray-500">
                                        No occupancies found in this range.
                                    </td>
                                </tr>
                            )}
                        </tbody>
                    </table>
                </div>
            )}
        </div>
    );
}