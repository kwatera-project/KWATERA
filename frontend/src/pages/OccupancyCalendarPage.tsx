import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getOccupancy } from "../api/adminApi";

import { format, addDays, startOfToday } from "date-fns";
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
    const [daysToShow, setDaysToShow] = useState(7);

    const dates = Array.from({ length: daysToShow }).map((_, i) => addDays(startDate, i));

    useEffect(() => {
        const startStr = format(dates[0], 'yyyy-MM-dd');
        const endStr = format(dates[dates.length - 1], 'yyyy-MM-dd');

        getOccupancy(startStr, endStr)
            .then(setOccupancies)
            .catch(console.error);
    }, [startDate]);

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

    const chunks: Date[][] = [];
    for (let i = 0; i < dates.length; i += 7) {
        chunks.push(dates.slice(i, i + 7));
    }

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

            <div className="mb-6 flex gap-4 p-4 bg-card rounded-xl shadow items-center flex-wrap">
                <label className="block text-sm font-medium text-title">Date Range:</label>
                <div className="flex gap-3 items-center flex-wrap">
                    <button
                        onClick={() => setStartDate(addDays(startDate, -daysToShow))}
                        className="px-4 py-2 bg-main hover:opacity-90 text-title text-sm font-medium rounded transition-colors"
                    >
                        - {daysToShow} Days
                    </button>
                    <div className="relative z-[60]">
                        <DatePicker
                            selected={startDate}
                            onChange={(date: Date | null) => date && setStartDate(date)}
                            className="px-4 py-2 bg-card border border-gray-300 rounded text-sm w-32 text-center cursor-pointer font-medium text-title outline-none focus:ring-2 focus:ring-button"
                            dateFormat="yyyy-MM-dd"
                            previousMonthButtonLabel={
                                <svg fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" d="M15 19l-7-7 7-7" /></svg>
                            }
                            nextMonthButtonLabel={
                                <svg fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" d="M9 5l7 7-7 7" /></svg>
                            }
                        />
                    </div>
                    <button
                        onClick={() => setStartDate(startOfToday())}
                        className="px-4 py-2 bg-button text-white hover:bg-button-hover text-sm font-medium rounded transition-colors"
                    >
                        Today
                    </button>
                    <button
                        onClick={() => setStartDate(addDays(startDate, daysToShow))}
                        className="px-4 py-2 bg-main hover:opacity-90 text-title text-sm font-medium rounded transition-colors"
                    >
                        + {daysToShow} Days
                    </button>
                </div>
                
                <div className="h-6 border-l border-gray-300 mx-2 hidden md:block"></div>

                <div className="flex gap-2 items-center">
                    <label className="block text-sm font-medium text-title">View:</label>
                    <select
                        value={daysToShow}
                        onChange={(e) => setDaysToShow(Number(e.target.value))}
                        className="px-4 py-2 bg-card border border-gray-300 rounded text-sm font-medium text-title outline-none focus:ring-2 focus:ring-button cursor-pointer"
                    >
                        <option value={7}>7 Days</option>
                        <option value={14}>14 Days</option>
                        <option value={28}>28 Days</option>
                    </select>
                </div>
            </div>

            {chunks.map((chunkDates, chunkIdx) => (
                <div key={chunkIdx} className="bg-card rounded-xl shadow p-4 overflow-visible mb-6">
                    <table className="w-full border-collapse min-w-max text-sm">
                        <thead>
                        <tr>
                            <th className="border-b border-r border-gray-200 p-3 bg-card text-left font-semibold text-title sticky left-0 z-20 w-56 shadow-[1px_0_0_0_#e5e7eb]">
                                Unit / Property
                            </th>
                            {chunkDates.map(d => (
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
                                    {renderRowCells(unitId, chunkDates)}
                                </tr>
                            );
                        })}
                        {unitIds.length === 0 && (
                            <tr>
                                <td colSpan={chunkDates.length + 1} className="p-8 text-center text-gray-500">
                                    No occupancies found in this range.
                                </td>
                            </tr>
                        )}
                        </tbody>
                    </table>
                </div>
            ))}
        </div>
    );
}