import { useEffect, useState } from 'react';

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

    useEffect(() => {
        const url = statusFilter
            ? `/api/v1/admin/reservations?status=${statusFilter}`
            : "/api/v1/admin/reservations";

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

    }, [statusFilter]);

    const filteredReservations = statusFilter
        ? reservations.filter(r => r.status === statusFilter)
        : reservations;

    return (
        <div className="p-8 max-w-6xl mx-auto">
            <h1 className="text-3xl font-bold mb-6 text-title">Reservation Overview</h1>

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
                  <span className={`px-2 py-1 rounded text-sm font-bold ${
                      res.status === 'CONFIRMED' ? 'bg-green-100 text-green-800' :
                          res.status === 'PENDING' ? 'bg-yellow-100 text-yellow-800' :
                              'bg-gray-100 text-gray-800'
                  }`}>
                    {res.status}
                  </span>
                            </td>
                            <td className="px-4 py-3 text-center">
                                <button className="text-blue-600 hover:underline text-sm mr-2">Edit Status</button>
                            </td>
                        </tr>
                    ))}
                    {filteredReservations.length === 0 && (
                        <tr>
                            <td colSpan={5} className="px-4 py-6 text-center text-gray-500">No reservations found.</td>
                        </tr>
                    )}
                    </tbody>
                </table>
            </div>
        </div>
    );
}