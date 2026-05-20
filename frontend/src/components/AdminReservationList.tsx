import { useEffect, useState, useCallback } from 'react';
import { Link } from "react-router-dom";
import { GATEWAY_BASE_URL } from '../api/apiConfig';

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
                if (res.status === 401) {
                    console.error("Token is incorrect!");
                }
                if (!res.ok) throw new Error("Get data error");
                return res.json();
            })
            .then((data) => setReservations(data))
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
                    setMessage({ text: "Reservation status updated successfully!", type: 'success' });
                    setReservations(prev => prev.map(r => r.id === id ? { ...r, status: newStatus } : r));
                } else {
                    const errorData = await res.json().catch(() => ({ message: "An error occurred" }));
                    let errorMsg = errorData.message || "An error occurred";

                    if (res.status === 400) errorMsg = "This status transition is not allowed";
                    if (res.status === 401) errorMsg = "Session expired or invalid. Please log in again";
                    if (res.status === 403) errorMsg = "You are not allowed to update this reservation";
                    if (res.status === 404) errorMsg = "Reservation not found";

                    setMessage({ text: errorMsg, type: 'error' });
                }
            })
            .catch((err) => {
                console.error(err);
                setMessage({ text: "Network error occurred", type: 'error' });
            });
    };

    const filteredReservations = reservations;

    return (
        <div className="p-8 max-w-6xl mx-auto">
            <div className="flex justify-between items-center mb-6">
                <h1 className="text-3xl font-bold text-title">Reservation Overview</h1>
                <div className="flex bg-gray-100 p-1 rounded-lg border border-gray-200">
                    <span className="px-4 py-2 text-sm font-medium rounded-md bg-white shadow text-gray-800 cursor-default">
                        List View
                    </span>
                    <Link
                        to="/admin/occupancy"
                        className="px-4 py-2 text-sm font-medium rounded-md text-gray-600 hover:text-gray-800 transition-colors"
                    >
                        Calendar View
                    </Link>
                </div>
            </div>

            {message && (
                <div className={`mb-4 p-4 rounded-lg shadow-sm ${message.type === 'success' ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'}`}>
                    {message.text}
                    <button onClick={() => setMessage(null)} className="ml-4 font-bold">×</button>
                </div>
            )}

            <div className="mb-6 flex gap-4 p-4 bg-gray-100 rounded-lg shadow-sm">
                <div>
                    <label className="block text-sm font-medium mb-1">Status</label>
                    <select
                        className="p-2 border rounded"
                        value={statusFilter}
                        onChange={(e) => setStatusFilter(e.target.value)}
                    >
                        <option value="">All</option>
                        <option value="PENDING">Pending</option>
                        <option value="CONFIRMED">Confirmed</option>
                        <option value="CANCELLED">Cancelled</option>
                        <option value="COMPLETED">Completed</option>
                    </select>
                </div>
            </div>

            <div className="overflow-x-auto bg-white shadow-md rounded-lg">
                <table className="min-w-full table-auto">
                    <thead className="bg-gray-200">
                    <tr>
                        <th className="px-4 py-2 text-left">Guest</th>
                        <th className="px-4 py-2 text-left">Unit/Property</th>
                        <th className="px-4 py-2 text-left">Stay Dates</th>
                        <th className="px-4 py-2 text-left">Status</th>
                        <th className="px-4 py-2 text-center">Actions</th>
                    </tr>
                    </thead>
                    <tbody>
                    {filteredReservations.map((res) => (
                        <tr key={res.id} className="border-b hover:bg-gray-50">
                            <td className="px-4 py-3">{res.guestName}</td>
                            <td className="px-4 py-3">{res.unitName}</td>
                            <td className="px-4 py-3">{res.startDate} to {res.endDate}</td>
                            <td className="px-4 py-3">
                                    <span
                                        className={`px-2 py-1 rounded text-sm font-bold ${res.status === 'CONFIRMED' ? 'bg-green-100 text-green-800' :
                                            res.status === 'PENDING' ? 'bg-yellow-100 text-yellow-800' :
                                                res.status === 'COMPLETED' ? 'bg-blue-100 text-blue-800' :
                                                    'bg-gray-100 text-gray-800'
                                        }`}>
                                        {res.status}
                                    </span>
                            </td>
                            <td className="px-4 py-3 text-center">
                                <div className="flex justify-center items-center gap-4">
                                    <Link
                                        to={`/reservations/${res.id}`}
                                        className="text-blue-600 hover:underline text-sm font-medium"
                                    >
                                        View Details
                                    </Link>

                                    <Link
                                        to={`/admin/occupancy`}
                                        className="text-indigo-600 hover:underline text-sm font-medium"
                                    >
                                        View Calendar
                                    </Link>

                                    {res.status === 'PENDING' && (
                                        <>
                                            <button
                                                onClick={() => handleStatusChange(res.id, 'CONFIRMED')}
                                                className="text-green-600 hover:underline text-sm font-medium"
                                            >
                                                Confirm
                                            </button>
                                            <button
                                                onClick={() => handleStatusChange(res.id, 'CANCELLED')}
                                                className="text-red-600 hover:underline text-sm font-medium"
                                            >
                                                Cancel
                                            </button>
                                        </>
                                    )}

                                    {res.status === 'CONFIRMED' && (
                                        <>
                                            <button
                                                onClick={() => handleStatusChange(res.id, 'COMPLETED')}
                                                className="text-blue-600 hover:underline text-sm font-medium"
                                            >
                                                Complete
                                            </button>
                                            <button
                                                onClick={() => handleStatusChange(res.id, 'CANCELLED')}
                                                className="text-red-600 hover:underline text-sm font-medium"
                                            >
                                                Cancel
                                            </button>
                                        </>
                                    )}

                                    {(res.status === 'COMPLETED' || res.status === 'CANCELLED') && (
                                        <span className="text-gray-400 text-sm"></span>
                                    )}
                                </div>
                            </td>
                        </tr>
                    ))}
                    {filteredReservations.length === 0 && (
                        <tr>
                            <td colSpan={5} className="px-4 py-6 text-center text-gray-500">No reservations found.
                            </td>
                        </tr>
                    )}
                    </tbody>
                </table>
            </div>
        </div>
    );
}
