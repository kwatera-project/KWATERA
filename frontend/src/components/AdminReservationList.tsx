import { useEffect, useState, useCallback } from "react";
import { Link } from "react-router-dom";
import { getAdminReservations, updateReservationStatus } from "../api/adminReservationApi";
import type { ReservationOverview } from "../types/reservation";

export default function AdminReservationList() {
    const [reservations, setReservations] = useState<ReservationOverview[]>([]);
    const [statusFilter, setStatusFilter] = useState("");

    const fetchReservations = useCallback(() => {
        getAdminReservations(statusFilter || undefined).then(setReservations);
    }, [statusFilter]);

    useEffect(() => {
        fetchReservations();
    }, [fetchReservations]);

    const handleStatusUpdate = async (id: string, newStatus: string) => {
        try {
            await updateReservationStatus(id, newStatus);
            fetchReservations();
        } catch {
            alert("Failed to update status");
        }
    };

    return (
        <div className="p-6 max-w-6xl mx-auto">
            <h1 className="text-2xl font-bold mb-4">Reservations Management</h1>

            <div className="mb-4">
                <select
                    value={statusFilter}
                    onChange={(e) => setStatusFilter(e.target.value)}
                    className="border p-2 rounded"
                >
                    <option value="">All Statuses</option>
                    <option value="PENDING">Pending</option>
                    <option value="CONFIRMED">Confirmed</option>
                    <option value="CANCELLED">Cancelled</option>
                </select>
            </div>

            <div className="overflow-x-auto shadow rounded-lg">
                <table className="w-full text-left bg-white">
                    <thead className="bg-gray-50 border-b">
                    <tr>
                        <th className="p-4">Guest</th>
                        <th className="p-4">Unit</th>
                        <th className="p-4">Stay Dates</th>
                        <th className="p-4">Status</th>
                        <th className="p-4">Actions</th>
                    </tr>
                    </thead>
                    <tbody>
                    {reservations.map((r) => (
                        <tr key={r.id} className="border-b hover:bg-gray-50">
                            <td className="p-4 font-medium">{r.guestName}</td>
                            <td className="p-4 text-sm text-gray-600">{r.unitName}</td>
                            <td className="p-4 text-sm">{r.startDate} - {r.endDate}</td>
                            <td className="p-4">
                                    <span className={`px-2 py-1 rounded text-xs font-bold ${
                                        r.status === 'CONFIRMED' ? 'bg-green-100 text-green-800' :
                                            r.status === 'PENDING' ? 'bg-yellow-100 text-yellow-800' : 'bg-red-100 text-red-800'
                                    }`}>
                                        {r.status}
                                    </span>
                            </td>
                            <td className="p-4 flex gap-2">
                                <Link
                                    to={`/reservations/${r.id}`}
                                    className="text-blue-600 hover:underline text-sm font-medium mr-2"
                                >
                                    View Details
                                </Link>
                                {r.status === 'PENDING' && (
                                    <button
                                        onClick={() => handleStatusUpdate(r.id, 'CONFIRMED')}
                                        className="bg-green-500 text-white px-2 py-1 rounded text-xs"
                                    >
                                        Confirm
                                    </button>
                                )}
                                {r.status !== 'CANCELLED' && (
                                    <button
                                        onClick={() => handleStatusUpdate(r.id, 'CANCELLED')}
                                        className="bg-red-500 text-white px-2 py-1 rounded text-xs"
                                    >
                                        Cancel
                                    </button>
                                )}
                            </td>
                        </tr>
                    ))}
                    </tbody>
                </table>
            </div>
        </div>
    );
}