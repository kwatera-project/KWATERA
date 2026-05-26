import { useEffect, useState } from "react";
import { useParams, Link } from "react-router-dom";
import { getReservationDetails } from "../api/reservationApi";
import type { ReservationDetails } from "../types/reservation";

export default function ReservationDetailsPage() {
    const { id } = useParams();
    const [reservation, setReservation] = useState<ReservationDetails | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        if (!id) return;

        getReservationDetails(id)
            .then(setReservation)
            .catch((err) => setError(err.message))
            .finally(() => setLoading(false));
    }, [id]);

    if (loading) return <div className="p-6">Loading reservation details...</div>;
    if (error) return <div className="p-6 text-red-500">{error}</div>;
    if (!reservation) return <div className="p-6">Reservation not found.</div>;

    const displayCurrency = reservation.currencyInfo?.displayCurrency || 'PLN';

    return (
        <div className="max-w-3xl mx-auto p-6">
            <div className="bg-card rounded-xl p-6 shadow border">
                <div className="flex justify-between items-center mb-6 border-b pb-2">
                    <h1 className="text-2xl font-bold">Reservation Details</h1>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    <div>
                        <p className="text-gray-500 text-sm">Reservation ID</p>
                        <p className="font-mono text-sm break-all">{reservation.id}</p>
                    </div>
                    <div>
                        <p className="text-gray-500 text-sm">Status</p>
                        <p className="font-bold text-blue-600">{reservation.status}</p>
                    </div>
                    <div>
                        <p className="text-gray-500 text-sm">Check-in Date</p>
                        <p className="font-medium">{reservation.startDate}</p>
                    </div>
                    <div>
                        <p className="text-gray-500 text-sm">Check-out Date</p>
                        <p className="font-medium">{reservation.endDate}</p>
                    </div>
                    <div>
                        <p className="text-gray-500 text-sm">Unit ID</p>
                        <p className="font-mono text-sm break-all">{reservation.unitId}</p>
                    </div>
                    <div>
                        <p className="text-gray-500 text-sm">Booked On</p>
                        <p className="font-medium">{new Date(reservation.createdAt).toLocaleString()}</p>
                    </div>
                    {reservation.totalPrice != null && (
                        <div>
                            <p className="text-gray-500 text-sm">Total Price</p>
                            <p className="font-medium text-lg">
                                {reservation.convertedTotalPrice} {displayCurrency}
                            </p>
                        </div>
                    )}
                </div>

                <div className="mt-8 pt-4 border-t">
                    <Link to="/admin/reservations" className="text-blue-500 hover:text-blue-700 hover:underline">
                        &larr; Return to reservations
                    </Link>
                </div>
            </div>
        </div>
    );
}