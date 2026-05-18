import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getOccupancy } from "../api/adminApi";
import { format, addDays, startOfToday } from "date-fns";

interface Occupancy {
    reservationId: string;
    unitId: string;
    startDate: string;
    endDate: string;
    status: string;
}

export default function OccupancyCalendarPage() {
    const [occupancies, setOccupancies] = useState<Occupancy[]>([]);
    const [startDate, setStartDate] = useState(startOfToday());
    const daysToShow = 14;

    const dates = Array.from({ length: daysToShow }).map((_, i) => addDays(startDate, i));

    useEffect(() => {
        const startStr = format(dates[0], 'yyyy-MM-dd');
        const endStr = format(dates[dates.length - 1], 'yyyy-MM-dd');

        getOccupancy(startStr, endStr)
            .then(setOccupancies)
            .catch(console.error);
    }, [startDate]);

    const unitIds = Array.from(new Set(occupancies.map(o => o.unitId)));

    const getOccupancyForCell = (unitId: string, date: Date) => {
        const dateStr = format(date, 'yyyy-MM-dd');
        return occupancies.find(o => {
            return o.unitId === unitId && o.startDate <= dateStr && o.endDate >= dateStr;
        });
    };

    return (
        <div className="p-6 max-w-7xl mx-auto overflow-x-auto">
            <div className="flex justify-between items-center mb-6">
                <h1 className="text-2xl font-bold">Occupancy Calendar</h1>
                <div className="flex gap-2">
                    <button onClick={() => setStartDate(addDays(startDate, -7))} className="px-4 py-2 bg-gray-200 rounded">- 7 Days</button>
                    <button onClick={() => setStartDate(startOfToday())} className="px-4 py-2 bg-blue-100 rounded">Today</button>
                    <button onClick={() => setStartDate(addDays(startDate, 7))} className="px-4 py-2 bg-gray-200 rounded">+ 7 Days</button>
                </div>
            </div>

            <table className="w-full border-collapse border border-gray-300 min-w-max text-sm">
                <thead>
                <tr>
                    <th className="border border-gray-300 p-2 bg-gray-100 sticky left-0 z-10 w-48">Unit ID</th>
                    {dates.map(d => (
                        <th key={d.toISOString()} className="border border-gray-300 p-2 bg-gray-50 min-w-[80px]">
                            {format(d, 'MMM dd')}
                        </th>
                    ))}
                </tr>
                </thead>
                <tbody>
                {unitIds.map(unitId => (
                    <tr key={unitId}>
                        <td className="border border-gray-300 p-2 bg-white sticky left-0 z-10 font-mono text-xs truncate max-w-[12rem]" title={unitId}>
                            {unitId.substring(0, 8)}...
                        </td>
                        {dates.map(d => {
                            const occ = getOccupancyForCell(unitId, d);
                            const isStart = occ && occ.startDate === format(d, 'yyyy-MM-dd');

                            let bgColor = "bg-white";
                            if (occ) {
                                bgColor = occ.status === 'CONFIRMED' ? "bg-blue-400 text-white" :
                                    occ.status === 'PENDING' ? "bg-orange-300 text-white" : "bg-gray-400 text-white";
                            }

                            return (
                                <td key={d.toISOString()} className={`border border-gray-300 p-1 text-center ${bgColor}`}>
                                    {occ && isStart ? (
                                        <Link to={`/reservations/${occ.reservationId}`} className="hover:underline font-bold text-xs block">
                                            {occ.status.substring(0, 4)}
                                        </Link>
                                    ) : (
                                        <span className="block h-4"></span>
                                    )}
                                </td>
                            );
                        })}
                    </tr>
                ))}
                {unitIds.length === 0 && (
                    <tr>
                        <td colSpan={daysToShow + 1} className="p-4 text-center text-gray-500 border border-gray-300">
                            No occupancies found in this range.
                        </td>
                    </tr>
                )}
                </tbody>
            </table>
        </div>
    );
}